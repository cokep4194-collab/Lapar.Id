package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.FoodCategory
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PickupSlot
import com.example.model.Product
import com.example.model.Stall
import com.example.ui.components.FoodArtwork
import com.example.ui.components.LaparBarChart
import com.example.ui.components.LaparEmptyState
import com.example.ui.components.LaparMetricCard
import com.example.ui.components.LaparStatusBadge
import com.example.ui.components.LaparTopBar
import com.example.ui.theme.LaparMint
import com.example.ui.theme.LaparOrange
import com.example.util.formatRupiah
import com.example.util.todayDateIso

@Composable
fun SellerDashboardScreen(
    stall: Stall?,
    products: List<Product>,
    orders: List<Order>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenReports: () -> Unit,
    onSaveStall: (String?, String, String, String, String, String, String?, Boolean) -> Unit
) {
    var showStallDialog by remember { mutableStateOf(false) }

    val paidOrActiveOrders = remember(orders) {
        orders.filter {
            it.status in listOf(
                OrderStatus.PAID.value,
                OrderStatus.PREPARING.value,
                OrderStatus.READY_FOR_PICKUP.value
            )
        }
    }
    val completedOrders = remember(orders) { orders.filter { it.status == OrderStatus.COMPLETED.value } }
    val totalRevenue = remember(orders) {
        orders.filter {
            it.status in listOf(
                OrderStatus.PAID.value,
                OrderStatus.PREPARING.value,
                OrderStatus.READY_FOR_PICKUP.value,
                OrderStatus.COMPLETED.value
            )
        }.sumOf { it.finalAmount }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(
            title = stall?.name ?: "Dashboard Penjual",
            subtitle = if (stall?.verified == true) "UMKM Terverifikasi • ${stall.campusLocation}" else "Menunggu Verifikasi Admin",
            unreadNotifications = unreadNotifications,
            onOpenNotifications = onOpenNotifications
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (stall == null) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Belum Ada Stan Kantin", style = MaterialTheme.typography.titleMedium)
                            Text("Buat profil stan UMKM Anda untuk mulai menerima pesanan pre-order mahasiswa.")
                            Button(onClick = { showStallDialog = true }) { Text("Buat Stan Sekarang") }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stall.name, style = MaterialTheme.typography.titleMedium)
                                Text("${stall.campusLocation} • ${stall.openHours}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { showStallDialog = true }) {
                                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Atur Stan")
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    LaparMetricCard(
                        title = "Pendapatan",
                        value = formatRupiah(totalRevenue),
                        icon = Icons.Filled.MonetizationOn,
                        accentColor = LaparMint,
                        modifier = Modifier.weight(1f)
                    )
                    LaparMetricCard(
                        title = "Antrean Aktif",
                        value = paidOrActiveOrders.size.toString(),
                        icon = Icons.Filled.ReceiptLong,
                        accentColor = LaparOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    LaparMetricCard(
                        title = "Menu Aktif",
                        value = products.count { it.active }.toString(),
                        icon = Icons.Filled.Fastfood,
                        modifier = Modifier.weight(1f)
                    )
                    LaparMetricCard(
                        title = "Pesanan Selesai",
                        value = completedOrders.size.toString(),
                        icon = Icons.Filled.Storefront,
                        accentColor = LaparMint,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text("Aksi Cepat UMKM", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onOpenProducts,
                        modifier = Modifier.weight(1f).testTag("seller_quick_products"),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Menu & Slot") }
                    Button(
                        onClick = onOpenOrders,
                        modifier = Modifier.weight(1f).testTag("seller_quick_orders"),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Pesanan & QR") }
                    OutlinedButton(
                        onClick = onOpenReports,
                        modifier = Modifier.weight(1f).testTag("seller_quick_reports"),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Laporan") }
                }
            }

            item {
                Text("Pesanan Masuk Terbaru (Urut Jam Ambil)", style = MaterialTheme.typography.titleMedium)
            }

            if (paidOrActiveOrders.isEmpty()) {
                item {
                    LaparEmptyState(
                        title = "Belum Ada Pesanan Aktif",
                        subtitle = "Pesanan akan masuk ke sini setelah pembayaran Midtrans pelanggan terverifikasi."
                    )
                }
            } else {
                items(paidOrActiveOrders.take(5), key = { it.id }) { order ->
                    Card(
                        onClick = onOpenOrders,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${order.orderCode} • ${order.customerName}", style = MaterialTheme.typography.titleSmall)
                                Text("Ambil ${order.pickupSlotLabel} • ${formatRupiah(order.finalAmount)}", style = MaterialTheme.typography.bodySmall)
                            }
                            LaparStatusBadge(status = OrderStatus.fromRaw(order.status))
                        }
                    }
                }
            }
        }
    }

    if (showStallDialog) {
        var name by remember { mutableStateOf(stall?.name ?: "") }
        var desc by remember { mutableStateOf(stall?.description ?: "") }
        var location by remember { mutableStateOf(stall?.campusLocation ?: "") }
        var hours by remember { mutableStateOf(stall?.openHours ?: "08:00 - 16:00") }
        var active by remember { mutableStateOf(stall?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showStallDialog = false },
            title = { Text(if (stall == null) "Buat Stan Kantin" else "Pengaturan Stan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Stan") }, singleLine = true)
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Lokasi Gedung Kampus") }, singleLine = true)
                    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Jam Operasional") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi Stan") })
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Stan Buka (Aktif)")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank() && location.isNotBlank(),
                    onClick = {
                        onSaveStall(stall?.id, name, desc, location, hours, stall?.bannerKey ?: "img_hero_campus", stall?.imageUrl, active)
                        showStallDialog = false
                    }
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showStallDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun SellerProductsScreen(
    stall: Stall?,
    categories: List<FoodCategory>,
    products: List<Product>,
    slots: List<PickupSlot>,
    onSaveProduct: (
        productId: String?,
        stallId: String,
        categoryId: String,
        categoryName: String,
        name: String,
        description: String,
        price: Long,
        discountPrice: Long?,
        flashDealActive: Boolean,
        flashDealLabel: String?,
        dailyQuota: Long,
        remainingStock: Long,
        active: Boolean,
        imageUrl: String?,
        imageKey: String
    ) -> Unit,
    onToggleProductActive: (String, Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveSlot: (String?, String, String, String, String, Long, Boolean) -> Unit,
    onDeleteSlot: (String) -> Unit
) {
    var tabIdx by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductForm by remember { mutableStateOf(false) }
    var showSlotForm by remember { mutableStateOf(false) }

    val effectiveCategories = remember(categories) {
        if (categories.isNotEmpty()) categories
        else listOf(
            FoodCategory(id = "cat_nasi", name = "Nasi"),
            FoodCategory(id = "cat_mi", name = "Mi"),
            FoodCategory(id = "cat_minuman", name = "Minuman"),
            FoodCategory(id = "cat_camilan", name = "Camilan"),
            FoodCategory(id = "cat_lainnya", name = "Makanan Lainnya")
        )
    }

    Scaffold(
        topBar = { LaparTopBar(title = "Manajemen Produk & Slot") },
        floatingActionButton = {
            if (stall != null) {
                FloatingActionButton(
                    onClick = {
                        if (tabIdx == 0) {
                            editingProduct = null
                            showProductForm = true
                        } else {
                            showSlotForm = true
                        }
                    },
                    modifier = Modifier.testTag("seller_add_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIdx) {
                Tab(selected = tabIdx == 0, onClick = { tabIdx = 0 }, text = { Text("Produk (${products.size})") })
                Tab(selected = tabIdx == 1, onClick = { tabIdx = 1 }, text = { Text("Slot Ambil (${slots.size})") })
            }

            if (stall == null) {
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    LaparEmptyState(title = "Buat Stan Terlebih Dahulu", subtitle = "Buka tab Dashboard untuk membuat stan Anda.")
                }
            } else if (tabIdx == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (products.isEmpty()) {
                        item {
                            LaparEmptyState(
                                icon = Icons.Filled.Inventory,
                                title = "Belum Ada Menu",
                                subtitle = "Tekan tombol + untuk menambah produk, mengatur kuota harian, dan Flash Food Deals."
                            )
                        }
                    } else {
                        items(products, key = { it.id }) { p ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FoodArtwork(
                                        imageUrl = p.imageUrl,
                                        imageKey = p.imageKey,
                                        contentDescription = p.name,
                                        modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${formatRupiah(p.effectivePrice)} • Stok ${p.remainingStock}/${p.dailyQuota}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (p.flashDealActive) {
                                            Text("Flash Deal Aktif", style = MaterialTheme.typography.labelSmall, color = LaparOrange)
                                        }
                                    }
                                    Switch(
                                        checked = p.active,
                                        onCheckedChange = { onToggleProductActive(p.id, it) }
                                    )
                                    IconButton(onClick = {
                                        editingProduct = p
                                        showProductForm = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onDeleteProduct(p.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (slots.isEmpty()) {
                        item {
                            LaparEmptyState(
                                icon = Icons.Filled.Schedule,
                                title = "Belum Ada Slot Pengambilan",
                                subtitle = "Tambahkan jadwal ambil (mis. 11:30 - 12:00) dan batas kapasitas porsi per jam."
                            )
                        }
                    } else {
                        items(slots, key = { it.id }) { sl ->
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${sl.date} • ${sl.label}", style = MaterialTheme.typography.titleSmall)
                                        Text("Terisi ${sl.bookedCount}/${sl.capacity} slot", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { onDeleteSlot(sl.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Hapus Slot")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProductForm && stall != null) {
        val initial = editingProduct
        var name by remember { mutableStateOf(initial?.name ?: "") }
        var desc by remember { mutableStateOf(initial?.description ?: "") }
        var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "15000") }
        var quotaText by remember { mutableStateOf(initial?.dailyQuota?.toString() ?: "30") }
        var stockText by remember { mutableStateOf(initial?.remainingStock?.toString() ?: "30") }
        var flashActive by remember { mutableStateOf(initial?.flashDealActive ?: false) }
        var discountText by remember { mutableStateOf(initial?.discountPrice?.toString() ?: "12000") }
        var flashLabel by remember { mutableStateOf(initial?.flashDealLabel ?: "Promo Jam Makan") }
        var selectedCat by remember {
            mutableStateOf(effectiveCategories.firstOrNull { it.id == initial?.categoryId } ?: effectiveCategories.first())
        }
        var imageKey by remember { mutableStateOf(initial?.imageKey ?: "img_food_nasi") }
        var pickedUri by remember { mutableStateOf(initial?.imageUrl) }

        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri -> if (uri != null) pickedUri = uri.toString() }
        )

        AlertDialog(
            onDismissRequest = { showProductForm = false },
            title = { Text(if (initial == null) "Tambah Menu" else "Edit Menu") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Makanan/Minuman") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter(Char::isDigit) },
                            label = { Text("Harga (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = quotaText,
                            onValueChange = { quotaText = it.filter(Char::isDigit) },
                            label = { Text("Kuota/Hari") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter(Char::isDigit) },
                        label = { Text("Sisa Stok Saat Ini") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text("Kategori", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(effectiveCategories) { cat ->
                            FilterChip(
                                selected = selectedCat.id == cat.id,
                                onClick = { selectedCat = cat },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                    Text("Foto Makanan", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (pickedUri != null) "Foto Dipilih" else "Pilih dari Galeri")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("img_food_nasi" to "Nasi", "img_food_mie" to "Mi", "img_food_minuman" to "Minuman").forEach { (k, lbl) ->
                            item {
                                FilterChip(
                                    selected = imageKey == k,
                                    onClick = { imageKey = k },
                                    label = { Text(lbl) }
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, null, tint = LaparOrange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Flash Food Deal")
                        }
                        Switch(checked = flashActive, onCheckedChange = { flashActive = it })
                    }
                    if (flashActive) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it.filter(Char::isDigit) },
                            label = { Text("Harga Diskon Flash Deal (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = flashLabel,
                            onValueChange = { flashLabel = it },
                            label = { Text("Label Promo") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank() && (priceText.toLongOrNull() ?: 0L) > 0L,
                    onClick = {
                        val p = priceText.toLongOrNull() ?: 10000L
                        val q = quotaText.toLongOrNull() ?: 20L
                        val s = (stockText.toLongOrNull() ?: q).coerceAtMost(q)
                        val d = if (flashActive) discountText.toLongOrNull() else null
                        onSaveProduct(
                            initial?.id, stall.id, selectedCat.id, selectedCat.name,
                            name, desc, p, d, flashActive, flashLabel,
                            q, s, initial?.active ?: true, pickedUri, imageKey
                        )
                        showProductForm = false
                    }
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showProductForm = false }) { Text("Batal") } }
        )
    }

    if (showSlotForm && stall != null) {
        var date by remember { mutableStateOf(todayDateIso()) }
        var start by remember { mutableStateOf("12:00") }
        var end by remember { mutableStateOf("12:30") }
        var capacity by remember { mutableStateOf("20") }

        AlertDialog(
            onDismissRequest = { showSlotForm = false },
            title = { Text("Tambah Slot Pengambilan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Tanggal (YYYY-MM-DD)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Mulai") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("Selesai") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it.filter(Char::isDigit) },
                        label = { Text("Kapasitas Maksimal Pesanan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = date.length == 10 && start.isNotBlank() && end.isNotBlank(),
                    onClick = {
                        onSaveSlot(null, stall.id, date, start, end, capacity.toLongOrNull() ?: 15L, true)
                        showSlotForm = false
                    }
                ) { Text("Simpan Slot") }
            },
            dismissButton = { TextButton(onClick = { showSlotForm = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun SellerOrdersScreen(
    orders: List<Order>,
    onAdvanceOrder: (Order, OrderStatus) -> Unit,
    onVerifyQrCode: (String, (Order) -> Unit) -> Unit
) {
    var showQrModal by remember { mutableStateOf(false) }
    var qrInput by remember { mutableStateOf("") }
    var lastVerifiedOrder by remember { mutableStateOf<Order?>(null) }

    val activeOrders = remember(orders) {
        orders.filter { it.status != OrderStatus.PENDING_PAYMENT.value }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Manajemen Pesanan & QR")

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Button(
                onClick = { showQrModal = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("seller_verify_qr_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Verifikasi QR Pengambilan Customer")
            }
        }

        if (activeOrders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "Belum Ada Pesanan Terbayar",
                    subtitle = "Pesanan akan muncul di sini setelah pembayaran Midtrans pelanggan terverifikasi."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeOrders, key = { it.id }) { order ->
                    val status = OrderStatus.fromRaw(order.status)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${order.orderCode} • ${order.customerName}", style = MaterialTheme.typography.titleSmall)
                                    Text("Ambil: ${order.pickupDate} ${order.pickupSlotLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                LaparStatusBadge(status = status)
                            }
                            HorizontalDivider()
                            order.items.forEach { item ->
                                Text("${item.quantity}x ${item.productName} ${if (item.note.isNotBlank()) "(${item.note})" else ""}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Total: ${formatRupiah(order.finalAmount)}", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (status) {
                                    OrderStatus.PAID -> {
                                        Button(onClick = { onAdvanceOrder(order, OrderStatus.PREPARING) }) {
                                            Text("Proses Pesanan")
                                        }
                                    }
                                    OrderStatus.PREPARING -> {
                                        Button(onClick = { onAdvanceOrder(order, OrderStatus.READY_FOR_PICKUP) }) {
                                            Text("Siap Diambil")
                                        }
                                    }
                                    OrderStatus.READY_FOR_PICKUP -> {
                                        Button(onClick = {
                                            qrInput = order.qrToken
                                            showQrModal = true
                                        }) {
                                            Text("Verifikasi QR")
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrModal) {
        AlertDialog(
            onDismissRequest = { showQrModal = false },
            title = { Text("Verifikasi QR Pengambilan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Masukkan kode token QR dari HP customer (mis. LAPAR-XXXXXX) atau kode pesanan (LPR-XXXXXX):")
                    OutlinedTextField(
                        value = qrInput,
                        onValueChange = { qrInput = it },
                        label = { Text("Kode QR / Pesanan") },
                        singleLine = true,
                        modifier = Modifier.testTag("qr_verify_input")
                    )
                    lastVerifiedOrder?.let { ok ->
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(10.dp)) {
                            Text("Terverifikasi: ${ok.orderCode} (${ok.customerName})", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = qrInput.isNotBlank(),
                    onClick = {
                        onVerifyQrCode(qrInput) { verified ->
                            lastVerifiedOrder = verified
                            qrInput = ""
                        }
                    }
                ) { Text("Verifikasi & Selesaikan") }
            },
            dismissButton = { TextButton(onClick = { showQrModal = false }) { Text("Tutup") } }
        )
    }
}

@Composable
fun SellerReportsScreen(
    orders: List<Order>,
    onBack: (() -> Unit)? = null
) {
    val validOrders = remember(orders) {
        orders.filter {
            it.status in listOf(
                OrderStatus.PAID.value,
                OrderStatus.PREPARING.value,
                OrderStatus.READY_FOR_PICKUP.value,
                OrderStatus.COMPLETED.value
            )
        }
    }
    val totalRevenue = remember(validOrders) { validOrders.sumOf { it.finalAmount } }
    val topMenuChart = remember(validOrders) {
        validOrders.flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, list) -> name.take(10) to list.sumOf { it.quantity }.toFloat() }
            .sortedByDescending { it.second }
            .take(5)
    }
    val peakHoursChart = remember(validOrders) {
        validOrders.groupBy { it.pickupSlotLabel.substringBefore(" -").ifBlank { "12:00" } }
            .map { (slot, list) -> slot to list.size.toFloat() }
            .sortedBy { it.first }
            .take(6)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Laporan Penjualan UMKM", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    LaparMetricCard(
                        title = "Total Pendapatan",
                        value = formatRupiah(totalRevenue),
                        icon = Icons.Filled.MonetizationOn,
                        accentColor = LaparMint,
                        modifier = Modifier.weight(1f)
                    )
                    LaparMetricCard(
                        title = "Total Transaksi",
                        value = validOrders.size.toString(),
                        icon = Icons.Filled.Assessment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                LaparBarChart(
                    title = "Grafik Menu Terlaris (Porsi Terjual)",
                    entries = topMenuChart,
                    barColor = LaparOrange
                )
            }
            item {
                LaparBarChart(
                    title = "Grafik Waktu Penjualan Paling Ramai",
                    entries = peakHoursChart,
                    barColor = LaparMint
                )
            }
            item {
                Text("Riwayat Transaksi Terbayar", style = MaterialTheme.typography.titleMedium)
            }
            items(validOrders, key = { it.id }) { o ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${o.orderCode} • ${o.customerName}", style = MaterialTheme.typography.titleSmall)
                            Text("${o.pickupDate} ${o.pickupSlotLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatRupiah(o.finalAmount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
