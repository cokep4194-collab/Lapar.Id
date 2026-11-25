const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "customer_123";
const OTHER_CUSTOMER_UID = "customer_999";
const SELLER_UID = "seller_456";
const ADMIN_EMAIL = "raflisyawal2005@gmail.com";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("stalls").where("active", "==", true).get());
  await assertFails(unauth.collection("products").where("active", "==", true).get());
  await assertFails(unauth.collection("orders").where("customerId", "==", CUSTOMER_UID).get());
  await assertFails(unauth.collection("users").doc(CUSTOMER_UID).get());
});

test("User registration prevents self-assigned admin role and isolates PII", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID, {
    email: "budi@kampus.ac.id",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  // Self-assigning admin must fail
  await assertFails(
    customerDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Budi",
      email: "budi@kampus.ac.id",
      role: "admin",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid customer registration succeeds (no @DocumentId id field)
  await assertSucceeds(
    customerDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Budi Mahasiswa",
      email: "budi@kampus.ac.id",
      role: "customer",
      nimOrUnit: "21060123",
      phone: "08123456789",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user PII read and user list scraping must fail
  const otherDb = testEnv.authenticatedContext(OTHER_CUSTOMER_UID).firestore();
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(otherDb.collection("users").get());

  // Role escalation via update must fail
  await assertFails(
    customerDb.collection("users").doc(CUSTOMER_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Bootstrapped Admin requires verified email (spoof check) and manages categories", async () => {
  const unverifiedSpoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const now = new Date(Date.now() - 1000);
  await assertFails(
    unverifiedSpoofDb.collection("categories").doc("cat_nasi").set({
      name: "Nasi",
      slug: "nasi",
      iconName: "rice_bowl",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("categories").doc("cat_nasi").set({
      name: "Nasi",
      slug: "nasi",
      iconName: "rice_bowl",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertSucceeds(
    customerDb.collection("categories").where("active", "==", true).get()
  );
});

test("Seller stall, product, pickup slot, and Customer order + slot increment parity", async () => {
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_CUSTOMER_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Seller creates stall (verified must be false)
  await assertSucceeds(
    sellerDb.collection("stalls").doc("stall_1").set({
      sellerId: SELLER_UID,
      name: "Warung Geprek Teknik",
      description: "Ayam geprek segar setiap hari",
      campusLocation: "Kantin FT Lantai 1",
      openHours: "08:00 - 16:00",
      bannerKey: "img_food_nasi",
      rating: 4.8,
      totalOrders: 0,
      verified: false,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seller creates product
  await assertSucceeds(
    sellerDb.collection("products").doc("prod_1").set({
      stallId: "stall_1",
      sellerId: SELLER_UID,
      categoryId: "cat_nasi",
      categoryName: "Nasi",
      name: "Nasi Ayam Geprek",
      description: "Crispy dengan sambal matah",
      price: 18000,
      discountPrice: 15000,
      flashDealActive: true,
      flashDealLabel: "Hemat 17%",
      dailyQuota: 40,
      remainingStock: 40,
      active: true,
      imageKey: "img_food_nasi",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seller creates pickup slot
  await assertSucceeds(
    sellerDb.collection("pickup_slots").doc("slot_1").set({
      stallId: "stall_1",
      sellerId: SELLER_UID,
      date: "2026-11-25",
      startTime: "12:00",
      endTime: "12:30",
      label: "12:00 - 12:30",
      capacity: 15,
      bookedCount: 0,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer filtered queries succeed; unfiltered queries fail
  await assertSucceeds(customerDb.collection("stalls").where("active", "==", true).get());
  await assertSucceeds(customerDb.collection("products").where("active", "==", true).get());
  await assertSucceeds(
    customerDb.collection("pickup_slots").where("stallId", "==", "stall_1").where("active", "==", true).get()
  );
  await assertFails(customerDb.collection("stalls").get());

  // Customer books slot (+1 only) and creates PENDING_PAYMENT order
  await assertSucceeds(
    customerDb.collection("pickup_slots").doc("slot_1").update({
      bookedCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Customer cannot forge PAID status on order creation
  await assertFails(
    customerDb.collection("orders").doc("order_bad").set({
      orderCode: "LPR-0001",
      customerId: CUSTOMER_UID,
      customerName: "Budi",
      sellerId: SELLER_UID,
      stallId: "stall_1",
      stallName: "Warung Geprek Teknik",
      paymentGroupId: "pay_1",
      items: [{ productId: "prod_1", productName: "Nasi Ayam Geprek", price: 15000, quantity: 1 }],
      totalAmount: 15000,
      discountAmount: 0,
      finalAmount: 15000,
      pickupDate: "2026-11-25",
      pickupSlotId: "slot_1",
      pickupSlotLabel: "12:00 - 12:30",
      status: "PAID",
      paymentStatus: "SETTLEMENT",
      qrToken: "QR-123456",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid PENDING_PAYMENT order succeeds
  await assertSucceeds(
    customerDb.collection("orders").doc("order_1").set({
      orderCode: "LPR-0001",
      customerId: CUSTOMER_UID,
      customerName: "Budi",
      sellerId: SELLER_UID,
      stallId: "stall_1",
      stallName: "Warung Geprek Teknik",
      paymentGroupId: "pay_1",
      items: [{ productId: "prod_1", productName: "Nasi Ayam Geprek", price: 15000, quantity: 1 }],
      totalAmount: 15000,
      discountAmount: 0,
      finalAmount: 15000,
      pickupDate: "2026-11-25",
      pickupSlotId: "slot_1",
      pickupSlotLabel: "12:00 - 12:30",
      status: "PENDING_PAYMENT",
      paymentStatus: "PENDING",
      qrToken: "QR-123456",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unrelated customer cannot view order
  await assertFails(otherDb.collection("orders").doc("order_1").get());

  // Seller cannot mark PENDING_PAYMENT order as PREPARING before payment verified
  await assertFails(
    sellerDb.collection("orders").doc("order_1").update({
      status: "PREPARING",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Loyalty points cannot be self-inflated by customer", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Initializing with > 0 points fails
  await assertFails(
    customerDb.collection("loyalty_points").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      points: 1000,
      totalEarned: 1000,
      totalRedeemed: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Initializing with 0 points succeeds
  await assertSucceeds(
    customerDb.collection("loyalty_points").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      points: 0,
      totalEarned: 0,
      totalRedeemed: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});
