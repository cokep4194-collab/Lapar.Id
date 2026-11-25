require("dotenv").config();
const express = require("express");
const cors = require("cors");
const crypto = require("crypto");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const PORT = process.env.PORT || 3001;
const MIDTRANS_SERVER_KEY = process.env.MIDTRANS_SERVER_KEY || "";
const MIDTRANS_IS_PRODUCTION = process.env.MIDTRANS_IS_PRODUCTION === "true";
const FIRESTORE_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-laparid-cdab0cdc-0bc7-49d0-a654-c2ca8fd70cbf";

const MIDTRANS_SNAP_URL = MIDTRANS_IS_PRODUCTION
  ? "https://app.midtrans.com/snap/v1/transactions"
  : "https://app.sandbox.midtrans.com/snap/v1/transactions";

const isMidtransConfigured =
  MIDTRANS_SERVER_KEY.length > 10 &&
  !MIDTRANS_SERVER_KEY.includes("REPLACE_WITH_YOUR");

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = getFirestore(admin.app(), FIRESTORE_DATABASE_ID);

const app = express();
app.use(cors());
app.use(express.json());

async function requireFirebaseAuth(req, res, next) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ error: "UNAUTHORIZED", message: "Token Firebase dibutuhkan." });
  }
  const idToken = authHeader.slice(7);
  try {
    req.user = await admin.auth().verifyIdToken(idToken);
    next();
  } catch (err) {
    return res.status(401).json({ error: "INVALID_TOKEN", message: "Token Firebase tidak valid." });
  }
}

app.get("/api/health", (_req, res) => {
  res.json({
    ok: true,
    service: "laparid-midtrans-backend",
    mode: MIDTRANS_IS_PRODUCTION ? "production" : "sandbox",
    midtransConfigured: isMidtransConfigured,
    databaseId: FIRESTORE_DATABASE_ID,
  });
});

app.post("/api/payments/create-snap-token", requireFirebaseAuth, async (req, res) => {
  if (!isMidtransConfigured) {
    return res.status(503).json({
      error: "MIDTRANS_NOT_CONFIGURED",
      message:
        "Kredensial Midtrans Sandbox (MIDTRANS_SERVER_KEY) belum diatur di backend.",
    });
  }

  const { paymentGroupId } = req.body || {};
  if (!paymentGroupId || typeof paymentGroupId !== "string") {
    return res.status(400).json({ error: "INVALID_REQUEST", message: "paymentGroupId wajib diisi." });
  }

  try {
    const paymentRef = db.collection("payments").doc(paymentGroupId);
    const paymentSnap = await paymentRef.get();
    if (!paymentSnap.exists) {
      return res.status(404).json({ error: "PAYMENT_NOT_FOUND", message: "Data pembayaran tidak ditemukan." });
    }
    const payment = paymentSnap.data();
    if (payment.customerId !== req.user.uid) {
      return res.status(403).json({ error: "FORBIDDEN", message: "Akses pembayaran ditolak." });
    }

    if (payment.status === "SETTLEMENT") {
      return res.status(409).json({
        error: "ALREADY_PAID",
        message: "Pembayaran untuk transaksi ini sudah lunas.",
      });
    }

    if (payment.snapToken && payment.redirectUrl && payment.status === "PENDING") {
      return res.json({
        snapToken: payment.snapToken,
        redirectUrl: payment.redirectUrl,
      });
    }

    const basicAuth = Buffer.from(`${MIDTRANS_SERVER_KEY}:`).toString("base64");
    const snapPayload = {
      transaction_details: {
        order_id: paymentGroupId,
        gross_amount: Number(payment.grossAmount),
      },
      customer_details: {
        first_name: req.user.name || "Mahasiswa Lapar.id",
        email: req.user.email || "mahasiswa@kampus.ac.id",
      },
      enabled_payments: [
        "gopay",
        "shopeepay",
        "other_qris",
        "bca_va",
        "bni_va",
        "bri_va",
        "permata_va",
      ],
    };

    const midtransRes = await fetch(MIDTRANS_SNAP_URL, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        Authorization: `Basic ${basicAuth}`,
      },
      body: JSON.stringify(snapPayload),
    });

    const data = await midtransRes.json();
    if (!midtransRes.ok || !data.token) {
      return res.status(502).json({
        error: "MIDTRANS_ERROR",
        message: (data.error_messages || []).join(", ") || "Gagal membuat Snap Token Midtrans.",
      });
    }

    await paymentRef.update({
      snapToken: data.token,
      redirectUrl: data.redirect_url,
      updatedAt: FieldValue.serverTimestamp(),
    });

    return res.json({
      snapToken: data.token,
      redirectUrl: data.redirect_url,
    });
  } catch (err) {
    console.error("create-snap-token error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: "Terjadi kesalahan server." });
  }
});

app.post("/api/payments/midtrans-webhook", async (req, res) => {
  if (!isMidtransConfigured) {
    return res.status(503).json({ error: "MIDTRANS_NOT_CONFIGURED" });
  }

  const {
    order_id: paymentGroupId,
    status_code: statusCode,
    gross_amount: grossAmount,
    signature_key: signatureKey,
    transaction_status: transactionStatus,
    fraud_status: fraudStatus,
    payment_type: paymentType,
    transaction_id: transactionId,
  } = req.body || {};

  if (!paymentGroupId || !statusCode || !grossAmount || !signatureKey) {
    return res.status(400).json({ error: "INVALID_WEBHOOK_PAYLOAD" });
  }

  const expectedSignature = crypto
    .createHash("sha512")
    .update(`${paymentGroupId}${statusCode}${grossAmount}${MIDTRANS_SERVER_KEY}`)
    .digest("hex");

  const sigBuf = Buffer.from(String(signatureKey), "utf8");
  const expBuf = Buffer.from(expectedSignature, "utf8");
  if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
    return res.status(403).json({ error: "INVALID_SIGNATURE" });
  }

  let mappedPaymentStatus = "PENDING";
  let mappedOrderStatus = "PENDING_PAYMENT";
  if (
    transactionStatus === "settlement" ||
    (transactionStatus === "capture" && fraudStatus === "accept")
  ) {
    mappedPaymentStatus = "SETTLEMENT";
    mappedOrderStatus = "PAID";
  } else if (transactionStatus === "expire") {
    mappedPaymentStatus = "EXPIRE";
    mappedOrderStatus = "EXPIRED";
  } else if (transactionStatus === "cancel" || transactionStatus === "deny") {
    mappedPaymentStatus = "CANCEL";
    mappedOrderStatus = "CANCELLED";
  }

  try {
    await db.runTransaction(async (tx) => {
      const paymentRef = db.collection("payments").doc(paymentGroupId);
      const paymentSnap = await tx.get(paymentRef);
      if (!paymentSnap.exists) {
        throw new Error("Payment not found");
      }
      const payment = paymentSnap.data();

      // Idempotency guard: prevent double stock decrement or duplicate points
      if (payment.status === "SETTLEMENT") {
        return;
      }

      const orderIds = Array.isArray(payment.orderIds) ? payment.orderIds : [];
      const orderSnaps = [];
      for (const oid of orderIds) {
        const oSnap = await tx.get(db.collection("orders").doc(oid));
        if (oSnap.exists) orderSnaps.push(oSnap);
      }

      if (mappedPaymentStatus === "SETTLEMENT") {
        // Pre-read all products to decrement stock safely inside transaction
        const productDeductions = new Map();
        for (const oSnap of orderSnaps) {
          const order = oSnap.data();
          for (const item of order.items || []) {
            const pid = item.productId;
            const qty = Number(item.quantity || 0);
            productDeductions.set(pid, (productDeductions.get(pid) || 0) + qty);
          }
        }

        const productUpdates = [];
        for (const [pid, totalQty] of productDeductions.entries()) {
          const pRef = db.collection("products").doc(pid);
          const pSnap = await tx.get(pRef);
          if (pSnap.exists) {
            const currentStock = Number(pSnap.data().remainingStock || 0);
            if (currentStock < totalQty) {
              throw new Error(`Stok produk ${pid} tidak mencukupi`);
            }
            productUpdates.push({ ref: pRef, nextStock: currentStock - totalQty });
          }
        }

        for (const upd of productUpdates) {
          tx.update(upd.ref, {
            remainingStock: upd.nextStock,
            updatedAt: FieldValue.serverTimestamp(),
          });
        }

        for (const oSnap of orderSnaps) {
          const order = oSnap.data();
          tx.update(oSnap.ref, {
            status: "PAID",
            paymentStatus: "SETTLEMENT",
            updatedAt: FieldValue.serverTimestamp(),
          });
          // Notify seller of new verified order
          const sellerNotifRef = db.collection("notifications").doc();
          tx.set(sellerNotifRef, {
            userId: order.sellerId,
            title: "Pesanan Baru Masuk!",
            message: `Pesanan ${order.orderCode} (${order.pickupSlotLabel}) telah dibayar dan siap diproses.`,
            type: "NEW_ORDER",
            relatedOrderId: oSnap.id,
            read: false,
            createdAt: FieldValue.serverTimestamp(),
            updatedAt: FieldValue.serverTimestamp(),
          });
        }

        // Notify customer
        const custNotifRef = db.collection("notifications").doc();
        tx.set(custNotifRef, {
          userId: payment.customerId,
          title: "Pembayaran Terverifikasi!",
          message: `Pembayaran senilai Rp ${Number(payment.grossAmount).toLocaleString("id-ID")} berhasil. Pesanan diteruskan ke penjual.`,
          type: "PAYMENT_VERIFIED",
          relatedOrderId: orderIds[0] || "",
          read: false,
          createdAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        });

        // Award Lapar Points (1 point per Rp 1.000)
        const earnedPoints = Math.floor(Number(payment.grossAmount) / 1000);
        if (earnedPoints > 0) {
          const lpRef = db.collection("loyalty_points").doc(payment.customerId);
          const lpSnap = await tx.get(lpRef);
          if (lpSnap.exists) {
            const lp = lpSnap.data();
            tx.update(lpRef, {
              points: Number(lp.points || 0) + earnedPoints,
              totalEarned: Number(lp.totalEarned || 0) + earnedPoints,
              updatedAt: FieldValue.serverTimestamp(),
            });
          } else {
            tx.set(lpRef, {
              userId: payment.customerId,
              points: earnedPoints,
              totalEarned: earnedPoints,
              totalRedeemed: 0,
              createdAt: FieldValue.serverTimestamp(),
              updatedAt: FieldValue.serverTimestamp(),
            });
          }
        }
      } else if (mappedPaymentStatus !== "PENDING") {
        for (const oSnap of orderSnaps) {
          tx.update(oSnap.ref, {
            status: mappedOrderStatus,
            paymentStatus: mappedPaymentStatus,
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      }

      tx.update(paymentRef, {
        status: mappedPaymentStatus,
        paymentType: paymentType || "",
        midtransTransactionId: transactionId || "",
        updatedAt: FieldValue.serverTimestamp(),
      });
    });

    return res.json({ status: "ok", paymentStatus: mappedPaymentStatus });
  } catch (err) {
    console.error("midtrans-webhook error:", err);
    return res.status(500).json({ error: "WEBHOOK_PROCESSING_FAILED", message: err.message });
  }
});

app.get("/api/payments/status/:paymentGroupId", requireFirebaseAuth, async (req, res) => {
  const { paymentGroupId } = req.params;
  try {
    const snap = await db.collection("payments").doc(paymentGroupId).get();
    if (!snap.exists) {
      return res.status(404).json({ error: "NOT_FOUND" });
    }
    const data = snap.data();
    if (data.customerId !== req.user.uid) {
      return res.status(403).json({ error: "FORBIDDEN" });
    }
    return res.json({
      paymentGroupId,
      status: data.status,
      paymentType: data.paymentType || null,
      snapToken: data.snapToken || null,
      redirectUrl: data.redirectUrl || null,
    });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR" });
  }
});

app.listen(PORT, () => {
  console.log(`Lapar.id Midtrans Sandbox backend running on port ${PORT}`);
});
