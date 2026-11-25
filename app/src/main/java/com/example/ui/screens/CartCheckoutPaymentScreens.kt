package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CartItem
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PaymentRecord
import com.example.model.PaymentStatus
import com.example.model.PickupSlot
import com.example.model.Voucher
import com.example.ui.components.LaparEmptyState
import com.example.ui.components.LaparStatusBadge
import com.example.ui.components.LaparTopBar
import com.example.ui.components.MidtransIntegrationStatusCard
import com.example.ui.components.QrCodeCard
import com.example.ui.theme.LaparMint
import com.example.util.formatRupiah
import com.example.util.formatTimestamp
import com.example.viewmodel.MidtransUiState

@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onBack: () -> Unit,
    onUpdateQty: (String, String, Int) -> Unit,
    onGoToCheckout: () -> Unit
) {
    val groupedByStall = remember(cartItems) { cartItems.groupBy { it.stall.id } }
    val grandTotal = remember(cartItems) { cartItems.sumOf { it.lineTotal } }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Keranjang Belanja", onBack = onBack)

        if (cartItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Keranjang Masih Kosong",
                    subtitle = "Tambahkan makanan dari berbagai stan kantin kampus. Ongkos kirim selalu Rp 0!"
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (groupedByStall.size > 1) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Keranjang berisi ${groupedByStall.size} stan berbeda. Sistem otomatis memisahkan pesanan per stan agar setiap penjual hanya menerima pesanan miliknya.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            groupedByStall.forEach { (_, items) ->
                val stall = items.first().stall
                val stallSubtotal = items.sumOf { it.lineTotal }
                item(key = "cart_stall_${stall.id}") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stall.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = formatRupiah(stallSubtotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider()
                            items.forEach { ci ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ci.product.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = formatRupiah(ci.product.effectivePrice),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (ci.note.isNotBlank()) {
                                            Text(
                                                text = "Catatan: ${ci.note}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onUpdateQty(ci.product.id, ci.note, -1) }) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Kurangi")
                                        }
                                        Text(ci.quantity.toString(), style = MaterialTheme.typography.titleSmall)
                                        IconButton(onClick = { onUpdateQty(ci.product.id, ci.note, 1) }) {
                                            Icon(Icons.Filled.Add, contentDescription = "Tambah")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ongkos Kirim (Ambil Sendiri)", style = MaterialTheme.typography.bodyMedium)
                    Text("Rp 0", style = MaterialTheme.typography.titleSmall, color = LaparMint)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Pembayaran", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatRupiah(grandTotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onGoToCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("proceed_checkout_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Lanjut Checkout (${groupedByStall.size} Stan)")
                }
            }
        }
    }
}

@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    allSlots: List<PickupSlot>,
    selectedSlotsByStall: Map<String, PickupSlot>,
    myVouchers: List<Voucher>,
    selectedVoucher: Voucher?,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSelectSlot: (String, PickupSlot) -> Unit,
    onSelectVoucher: (Voucher?) -> Unit,
    onConfirmCheckout: () -> Unit
) {
    val grouped = remember(cartItems) { cartItems.groupBy { it.stall.id } }
    val subtotal = remember(cartItems) { cartItems.sumOf { it.lineTotal } }
    val eligibleVouchers = remember(myVouchers, subtotal) {
        myVouchers.filter { !it.redeemed && it.active && subtotal >= it.minOrderAmount }
    }
    val discount = selectedVoucher?.discountAmount?.coerceAtMost(subtotal) ?: 0L
    val finalTotal = (subtotal - discount).coerceAtLeast(0L)

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Checkout Pre-Order", onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            grouped.forEach { (stallId, items) ->
                val stall = items.first().stall
                val stallSlots = allSlots.filter { it.stallId == stallId && it.active }
                val selectedSlot = selectedSlotsByStall[stallId]

                item(key = "chk_stall_$stallId") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stall.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${items.sumOf { it.quantity }} item • ${formatRupiah(items.sumOf { it.lineTotal })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Smart Pickup Scheduler", style = MaterialTheme.typography.titleSmall)
                            }
                            if (stallSlots.isEmpty()) {
                                Text(
                                    text = "Belum ada slot waktu aktif dari penjual ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(stallSlots, key = { it.id }) { slot ->
                                        val full = slot.remainingCapacity <= 0L
                                        FilterChip(
                                            selected = selectedSlot?.id == slot.id,
                                            enabled = !full,
                                            onClick = { onSelectSlot(stallId, slot) },
                                            label = {
                                                Text(
                                                    if (full) "${slot.label} (Penuh)"
                                                    else "${slot.date.takeLast(5)} ${slot.label} (${slot.remainingCapacity} sisa)"
                                                )
                                            },
                                            modifier = Modifier.testTag("slot_chip_${slot.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ConfirmationNumber, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Voucher Lapar Points", style = MaterialTheme.typography.titleSmall)
                        }
                        if (eligibleVouchers.isEmpty()) {
                            Text(
                                text = "Belum ada voucher yang memenuhi syarat pesanan ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(eligibleVouchers, key = { it.id }) { v ->
                                    FilterChip(
                                        selected = selectedVoucher?.id == v.id,
                                        onClick = { onSelectVoucher(if (selectedVoucher?.id == v.id) null else v) },
                                        label = { Text("${v.code} (-${formatRupiah(v.discountAmount)})") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Ringkasan Pembayaran Transparan", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal Makanan")
                            Text(formatRupiah(subtotal))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ongkos Kirim")
                            Text("Rp 0", color = LaparMint)
                        }
                        if (discount > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Potongan Voucher")
                                Text("-${formatRupiah(discount)}", color = LaparMint)
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Tagihan", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = formatRupiah(finalTotal),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
            Button(
                onClick = onConfirmCheckout,
                enabled = !isSubmitting && grouped.keys.all { selectedSlotsByStall[it] != null },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp)
                    .testTag("confirm_checkout_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Payment, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buat Pesanan & Bayar Midtrans")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PaymentScreen(
    payment: PaymentRecord?,
    midtransUiState: MidtransUiState,
    onBack: () -> Unit,
    onRetryMidtrans: (String) -> Unit,
    onOpenOrders: () -> Unit
) {
    var openWebView by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Pembayaran Midtrans", onBack = onBack)

        if (payment == null) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(
                    icon = Icons.Filled.Payment,
                    title = "Transaksi Tidak Ditemukan",
                    subtitle = "Buat pesanan dari keranjang atau buka dari riwayat pesanan."
                )
            }
            return
        }

        val status = PaymentStatus.fromRaw(payment.status)
        val snapRedirect = (midtransUiState as? MidtransUiState.Ready)?.redirectUrl ?: payment.redirectUrl

        if (openWebView && !snapRedirect.isNullOrBlank()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Midtrans Snap Sandbox", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(onClick = { openWebView = false }) { Text("Tutup Snap") }
                }
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl(snapRedirect)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ID Transaksi: ${payment.paymentGroupId}", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = formatRupiah(payment.grossAmount),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Status Pembayaran: ${status.label}", style = MaterialTheme.typography.titleSmall)
                    if (!payment.paymentType.isNullOrBlank()) {
                        Text("Metode: ${payment.paymentType}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "Pesanan akan diteruskan ke penjual setelah webhook Midtrans memverifikasi pembayaran.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (midtransUiState) {
                is MidtransUiState.Loading -> {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Menghubungi backend untuk Snap Token Midtrans Sandbox...")
                        }
                    }
                }
                is MidtransUiState.Ready -> {
                    MidtransIntegrationStatusCard(
                        isConfigured = true,
                        statusMessage = "Snap Token siap (${midtransUiState.snapToken.take(12)}...). Pilih metode QRIS, GoPay, ShopeePay, atau Virtual Account pada halaman Snap."
                    )
                    Button(
                        onClick = { openWebView = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("open_midtrans_snap_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Buka Midtrans Snap Sandbox")
                    }
                }
                is MidtransUiState.NotConfigured -> {
                    MidtransIntegrationStatusCard(
                        isConfigured = false,
                        statusMessage = midtransUiState.reason
                    )
                }
                is MidtransUiState.Error -> {
                    MidtransIntegrationStatusCard(
                        isConfigured = false,
                        statusMessage = midtransUiState.message
                    )
                }
                is MidtransUiState.Idle -> {
                    if (!snapRedirect.isNullOrBlank() && status == PaymentStatus.PENDING) {
                        Button(
                            onClick = { openWebView = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Lanjutkan Pembayaran Midtrans Snap")
                        }
                    }
                }
            }

            if (status == PaymentStatus.PENDING) {
                OutlinedButton(
                    onClick = { onRetryMidtrans(payment.paymentGroupId) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("retry_snap_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Minta / Perbarui Snap Token Midtrans")
                }
            }

            Button(
                onClick = onOpenOrders,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Lihat Riwayat & Status Pesanan")
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(
    orders: List<Order>,
    onBack: (() -> Unit)? = null,
    onOpenOrderDetail: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Riwayat Pesanan", onBack = onBack)

        if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "Belum Ada Pesanan",
                    subtitle = "Pesanan pre-order yang Anda buat akan muncul di sini dengan pelacakan realtime."
                )
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                val st = OrderStatus.fromRaw(order.status)
                Card(
                    onClick = { onOpenOrderDetail(order.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("order_history_card_${order.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.orderCode, style = MaterialTheme.typography.titleMedium)
                            LaparStatusBadge(status = st)
                        }
                        Text(order.stallName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Ambil: ${order.pickupDate} • ${order.pickupSlotLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${order.items.sumOf { it.quantity }} porsi", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = formatRupiah(order.finalAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDetailScreen(
    order: Order?,
    onBack: () -> Unit,
    onOpenPayment: (String) -> Unit,
    onCancelPendingOrder: (Order) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = order?.orderCode ?: "Detail Pesanan", onBack = onBack)

        if (order == null) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(title = "Pesanan tidak ditemukan", subtitle = "Periksa kembali riwayat pesanan Anda.")
            }
            return
        }

        val currentStatus = OrderStatus.fromRaw(order.status)
        val steps = listOf(
            OrderStatus.PAID to "Diterima Penjual",
            OrderStatus.PREPARING to "Sedang Disiapkan",
            OrderStatus.READY_FOR_PICKUP to "Siap Diambil",
            OrderStatus.COMPLETED to "Selesai Diambil"
        )
        val activeIndex = when (currentStatus) {
            OrderStatus.PAID -> 0
            OrderStatus.PREPARING -> 1
            OrderStatus.READY_FOR_PICKUP -> 2
            OrderStatus.COMPLETED -> 3
            else -> -1
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(order.stallName, style = MaterialTheme.typography.titleMedium)
                            Text(formatTimestamp(order.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LaparStatusBadge(status = currentStatus)
                    }
                    Text("Jadwal Pengambilan: ${order.pickupDate} (${order.pickupSlotLabel})", style = MaterialTheme.typography.bodyMedium)
                    if (!order.groupOrderCode.isNullOrBlank()) {
                        Text("Group Order: ${order.groupOrderCode}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Realtime Tracking Stepper
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pelacakan Status Realtime", style = MaterialTheme.typography.titleMedium)
                    steps.forEachIndexed { idx, (stepStatus, label) ->
                        val reached = activeIndex >= idx
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (reached) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (reached) LaparMint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (stepStatus == currentStatus) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // QR Code appears once payment verified
            if (currentStatus in listOf(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.COMPLETED)) {
                QrCodeCard(qrToken = order.qrToken, orderCode = order.orderCode)
            } else if (currentStatus == OrderStatus.PENDING_PAYMENT) {
                Button(
                    onClick = { onOpenPayment(order.paymentGroupId) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Lanjutkan Pembayaran Midtrans")
                }
                OutlinedButton(
                    onClick = { onCancelPendingOrder(order) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batalkan Pesanan")
                }
            }

            // Item breakdown
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rincian Menu", style = MaterialTheme.typography.titleMedium)
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodyMedium)
                                if (item.note.isNotBlank()) {
                                    Text("Catatan: ${item.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(formatRupiah(item.price * item.quantity), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ongkos Kirim")
                        Text("Rp 0", color = LaparMint)
                    }
                    if (order.discountAmount > 0L) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diskon Voucher")
                            Text("-${formatRupiah(order.discountAmount)}", color = LaparMint)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Dibayar", style = MaterialTheme.typography.titleMedium)
                        Text(formatRupiah(order.finalAmount), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
