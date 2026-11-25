package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppNotification
import com.example.model.GroupOrder
import com.example.model.LoyaltyPoints
import com.example.model.PickupSlot
import com.example.model.Product
import com.example.model.Stall
import com.example.model.Voucher
import com.example.ui.components.LaparEmptyState
import com.example.ui.components.LaparTopBar
import com.example.ui.theme.LaparAmber
import com.example.ui.theme.LaparMint
import com.example.util.formatRupiah
import com.example.util.formatTimestamp

@Composable
fun GroupOrderScreen(
    currentUserId: String,
    stalls: List<Stall>,
    products: List<Product>,
    slots: List<PickupSlot>,
    openGroups: List<GroupOrder>,
    myGroups: List<GroupOrder>,
    onBack: () -> Unit,
    onCreateGroup: (Stall, String, PickupSlot, Int) -> Unit,
    onAddItemToGroup: (GroupOrder, Product, Int, String) -> Unit,
    onCloseGroup: (String) -> Unit,
    onCheckoutGroup: (GroupOrder) -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var activeGroupForMenu by remember { mutableStateOf<GroupOrder?>(null) }

    val combinedGroups = remember(openGroups, myGroups, joinCodeInput) {
        (openGroups + myGroups)
            .distinctBy { it.id }
            .filter {
                joinCodeInput.isBlank() ||
                    it.code.contains(joinCodeInput.trim(), ignoreCase = true) ||
                    it.title.contains(joinCodeInput.trim(), ignoreCase = true)
            }
    }

    Scaffold(
        topBar = { LaparTopBar(title = "Group Order Kampus", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("create_group_order_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Buat Group Order")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = joinCodeInput,
                    onValueChange = { joinCodeInput = it },
                    label = { Text("Cari / Gabung dengan Kode Grup (mis. GRP-A1B2C)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_code_search_input")
                )
            }

            if (combinedGroups.isEmpty()) {
                item {
                    LaparEmptyState(
                        icon = Icons.Filled.Groups,
                        title = "Belum Ada Group Order Aktif",
                        subtitle = "Buat pesanan kelompok baru dan bagikan kodenya ke teman kelas agar semua bisa pilih menu masing-masing."
                    )
                }
            } else {
                items(combinedGroups, key = { it.id }) { group ->
                    val isHost = group.hostUserId == currentUserId
                    val total = group.items.sumOf { it.price * it.quantity }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${group.stallName} • Kode: ${group.code}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val shareText = "Yuk ikut Group Order Lapar.id '${group.title}' di ${group.stallName}! Kode Grup: ${group.code} (Ambil ${group.pickupSlotLabel})"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Bagikan Group Order"))
                                    }
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Bagikan Tautan")
                                }
                            }
                            Text(
                                text = "Host: ${group.hostName} • Ambil: ${group.pickupDate} (${group.pickupSlotLabel}) • Status: ${group.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (group.items.isNotEmpty()) {
                                HorizontalDivider()
                                group.items.forEach { gi ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${gi.userName}: ${gi.quantity}x ${gi.productName}", style = MaterialTheme.typography.bodySmall)
                                        Text(formatRupiah(gi.price * gi.quantity), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Grup", style = MaterialTheme.typography.titleSmall)
                                    Text(formatRupiah(total), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (group.status == "OPEN") {
                                    Button(
                                        onClick = { activeGroupForMenu = group },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Pilih Menu")
                                    }
                                }
                                if (isHost && group.status == "OPEN") {
                                    OutlinedButton(onClick = { onCloseGroup(group.id) }) {
                                        Text("Tutup Grup")
                                    }
                                }
                                if (isHost && group.status != "COMPLETED" && group.items.isNotEmpty()) {
                                    Button(onClick = { onCheckoutGroup(group) }) {
                                        Text("Checkout")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("Makan Siang Kelas") }
        var selectedStall by remember { mutableStateOf(stalls.firstOrNull()) }
        val stallSlots = remember(selectedStall, slots) {
            slots.filter { it.stallId == selectedStall?.id && it.remainingCapacity > 0L }
        }
        var selectedSlot by remember(selectedStall) { mutableStateOf(stallSlots.firstOrNull()) }
        var closesInMin by remember { mutableIntStateOf(30) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Group Order") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Kelompok") },
                        singleLine = true
                    )
                    Text("Pilih Stan", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stalls) { s ->
                            FilterChip(
                                selected = selectedStall?.id == s.id,
                                onClick = {
                                    selectedStall = s
                                    selectedSlot = slots.firstOrNull { it.stallId == s.id && it.remainingCapacity > 0L }
                                },
                                label = { Text(s.name) }
                            )
                        }
                    }
                    Text("Slot Ambil", style = MaterialTheme.typography.labelMedium)
                    if (stallSlots.isEmpty()) {
                        Text("Stan belum punya slot aktif.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(stallSlots) { sl ->
                                FilterChip(
                                    selected = selectedSlot?.id == sl.id,
                                    onClick = { selectedSlot = sl },
                                    label = { Text("${sl.date.takeLast(5)} ${sl.label}") }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 30, 60).forEach { m ->
                            FilterChip(
                                selected = closesInMin == m,
                                onClick = { closesInMin = m },
                                label = { Text("Tutup ${m}m") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedStall != null && selectedSlot != null && title.isNotBlank(),
                    onClick = {
                        onCreateGroup(selectedStall!!, title, selectedSlot!!, closesInMin)
                        showCreateDialog = false
                    }
                ) { Text("Buat Grup") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Batal") }
            }
        )
    }

    activeGroupForMenu?.let { group ->
        val stallProducts = remember(group, products) {
            products.filter { it.stallId == group.stallId && it.remainingStock > 0L }
        }
        var chosenProduct by remember { mutableStateOf(stallProducts.firstOrNull()) }
        var qty by remember { mutableIntStateOf(1) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { activeGroupForMenu = null },
            title = { Text("Tambah ke ${group.code}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (stallProducts.isEmpty()) {
                        Text("Belum ada menu tersedia di stan ini.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(stallProducts) { p ->
                                FilterChip(
                                    selected = chosenProduct?.id == p.id,
                                    onClick = { chosenProduct = p },
                                    label = { Text("${p.name} (${formatRupiah(p.effectivePrice)})") }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Catatan menu") },
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Jumlah:")
                            OutlinedButton(onClick = { if (qty > 1) qty-- }) { Text("-") }
                            Text(qty.toString(), style = MaterialTheme.typography.titleSmall)
                            OutlinedButton(onClick = { qty++ }) { Text("+") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = chosenProduct != null,
                    onClick = {
                        onAddItemToGroup(group, chosenProduct!!, qty, note)
                        activeGroupForMenu = null
                    }
                ) { Text("Tambahkan") }
            },
            dismissButton = {
                TextButton(onClick = { activeGroupForMenu = null }) { Text("Tutup") }
            }
        )
    }
}

@Composable
fun LaparPointsScreen(
    loyaltyPoints: LoyaltyPoints?,
    catalogVouchers: List<Voucher>,
    myVouchers: List<Voucher>,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onRedeemVoucher: (Voucher) -> Unit
) {
    val pts = loyaltyPoints?.points ?: 0L
    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Lapar Points & Voucher", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Stars, null, tint = LaparAmber, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Saldo Lapar Points", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "$pts Poin",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1 poin per Rp 1.000 transaksi Midtrans terverifikasi.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item { Text("Tukar Poin Jadi Voucher", style = MaterialTheme.typography.titleMedium) }

            if (catalogVouchers.isEmpty()) {
                item {
                    LaparEmptyState(
                        icon = Icons.Filled.ConfirmationNumber,
                        title = "Katalog Voucher Kosong",
                        subtitle = "Admin dapat menambahkan promo voucher untuk ditukarkan mahasiswa."
                    )
                }
            } else {
                items(catalogVouchers, key = { it.id }) { tpl ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tpl.title, style = MaterialTheme.typography.titleSmall)
                                Text(tpl.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Butuh ${tpl.pointsCost} poin", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = !isSubmitting && pts >= tpl.pointsCost,
                                onClick = { onRedeemVoucher(tpl) }
                            ) {
                                Text("Tukar")
                            }
                        }
                    }
                }
            }

            item { Text("Voucher Saya (${myVouchers.size})", style = MaterialTheme.typography.titleMedium) }
            items(myVouchers, key = { it.id }) { mv ->
                Surface(
                    color = if (mv.redeemed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${mv.code} • ${mv.title}", style = MaterialTheme.typography.titleSmall)
                            Text("Potongan ${formatRupiah(mv.discountAmount)} (min. ${formatRupiah(mv.minOrderAmount)})", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = if (mv.redeemed) "Terpakai" else "Siap Pakai",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (mv.redeemed) MaterialTheme.colorScheme.onSurfaceVariant else LaparMint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkReadAndOpenOrder: (AppNotification) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Notifikasi", onBack = onBack)
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(
                    icon = Icons.Filled.Notifications,
                    title = "Belum Ada Notifikasi",
                    subtitle = "Pembaruan pesanan diterima, diproses, dan siap diambil akan tampil di sini secara realtime."
                )
            }
            return
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications, key = { it.id }) { notif ->
                Card(
                    onClick = { onMarkReadAndOpenOrder(notif) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(notif.title, style = MaterialTheme.typography.titleSmall)
                        Text(notif.message, style = MaterialTheme.typography.bodySmall)
                        Text(formatTimestamp(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
