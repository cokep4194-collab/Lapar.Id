package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.FoodCategory
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.SellerProfile
import com.example.model.Stall
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.LaparBarChart
import com.example.ui.components.LaparEmptyState
import com.example.ui.components.LaparMetricCard
import com.example.ui.components.LaparStatusBadge
import com.example.ui.components.LaparTopBar
import com.example.ui.components.MidtransIntegrationStatusCard
import com.example.ui.theme.LaparMint
import com.example.ui.theme.LaparOrange
import com.example.util.formatRupiah

@Composable
fun AdminDashboardScreen(
    users: List<UserProfile>,
    sellers: List<SellerProfile>,
    stalls: List<Stall>,
    categories: List<FoodCategory>,
    orders: List<Order>,
    onUpdateUser: (String, String, Boolean) -> Unit,
    onVerifySeller: (String, String?, Boolean, Boolean) -> Unit,
    onSaveCategory: (String?, String, String, Long, Boolean) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSeedDefaults: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val customerCount = remember(users) { users.count { it.role == UserRole.CUSTOMER.value } }
    val sellerCount = remember(users, sellers) { maxOf(sellers.size, users.count { it.role == UserRole.SELLER.value }) }
    val paidOrders = remember(orders) {
        orders.filter {
            it.status in listOf(
                OrderStatus.PAID.value,
                OrderStatus.PREPARING.value,
                OrderStatus.READY_FOR_PICKUP.value,
                OrderStatus.COMPLETED.value
            )
        }
    }
    val totalTransactionValue = remember(paidOrders) { paidOrders.sumOf { it.finalAmount } }
    val activityChart = remember(orders) {
        OrderStatus.entries.map { st ->
            st.label.take(8) to orders.count { it.status == st.value }.toFloat()
        }.filter { it.second > 0f }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Dashboard Administrator", subtitle = "Kontrol Ekosistem Lapar.id")

        ScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 12.dp) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Ringkasan") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Pengguna (${users.size})") })
            Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("UMKM (${sellers.size})") })
            Tab(selected = tabIndex == 3, onClick = { tabIndex = 3 }, text = { Text("Kategori (${categories.size})") })
            Tab(selected = tabIndex == 4, onClick = { tabIndex = 4 }, text = { Text("Transaksi (${orders.size})") })
        }

        when (tabIndex) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            LaparMetricCard(
                                title = "Customer",
                                value = customerCount.toString(),
                                icon = Icons.Filled.People,
                                modifier = Modifier.weight(1f)
                            )
                            LaparMetricCard(
                                title = "UMKM",
                                value = sellerCount.toString(),
                                icon = Icons.Filled.Storefront,
                                accentColor = LaparOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            LaparMetricCard(
                                title = "Total Transaksi",
                                value = orders.size.toString(),
                                icon = Icons.Filled.ReceiptLong,
                                modifier = Modifier.weight(1f)
                            )
                            LaparMetricCard(
                                title = "Nilai Transaksi",
                                value = formatRupiah(totalTransactionValue),
                                icon = Icons.Filled.MonetizationOn,
                                accentColor = LaparMint,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        LaparBarChart(
                            title = "Grafik Aktivitas Aplikasi (Status Pesanan)",
                            entries = activityChart,
                            barColor = LaparOrange
                        )
                    }
                    item {
                        Button(
                            onClick = onSeedDefaults,
                            modifier = Modifier.fillMaxWidth().testTag("admin_seed_defaults_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Inisialisasi Kategori Default & Katalog Voucher")
                        }
                    }
                }
            }
            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(users, key = { it.userId }) { u ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(u.displayName, style = MaterialTheme.typography.titleSmall)
                                        Text("${u.email} • ${u.role.uppercase()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(
                                        checked = u.active,
                                        onCheckedChange = { onUpdateUser(u.userId, u.role, it) }
                                    )
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(UserRole.entries) { role ->
                                        FilterChip(
                                            selected = u.role == role.value,
                                            onClick = { onUpdateUser(u.userId, role.value, u.active) },
                                            label = { Text(role.value) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (sellers.isEmpty()) {
                        item {
                            LaparEmptyState(title = "Belum Ada Pendaftar UMKM", subtitle = "Akun UMKM yang mendaftar akan muncul di sini untuk diverifikasi.")
                        }
                    } else {
                        items(sellers, key = { it.sellerId }) { s ->
                            val stall = stalls.firstOrNull { it.sellerId == s.sellerId }
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.businessName, style = MaterialTheme.typography.titleSmall)
                                        Text("${s.ownerName} • ${s.campusArea}", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = if (s.verified) "Terverifikasi" else "Menunggu Verifikasi",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (s.verified) LaparMint else LaparOrange
                                        )
                                    }
                                    Button(onClick = { onVerifySeller(s.sellerId, stall?.id, !s.verified, s.active) }) {
                                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (s.verified) "Cabut" else "Verifikasi")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showCategoryDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Tambah Kategori Makanan")
                        }
                    }
                    items(categories, key = { it.id }) { cat ->
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Category, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                        Text("Urutan ${cat.sortOrder}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus Kategori")
                                }
                            }
                        }
                    }
                }
            }
            4 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders, key = { it.id }) { o ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${o.orderCode} • ${o.stallName}", style = MaterialTheme.typography.titleSmall)
                                    Text("${o.customerName} • ${formatRupiah(o.finalAmount)}", style = MaterialTheme.typography.bodySmall)
                                }
                                LaparStatusBadge(status = OrderStatus.fromRaw(o.status))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        var name by remember { mutableStateOf("") }
        var order by remember { mutableStateOf("6") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Tambah Kategori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Kategori") }, singleLine = true)
                    OutlinedTextField(value = order, onValueChange = { order = it.filter(Char::isDigit) }, label = { Text("Urutan Tampil") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = name.trim().length >= 2,
                    onClick = {
                        onSaveCategory(null, name, "restaurant", order.toLongOrNull() ?: 6L, true)
                        showCategoryDialog = false
                    }
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    isBootstrappedAdmin: Boolean,
    activeRoleOverride: UserRole?,
    isBackendUrlConfigured: Boolean,
    isSubmitting: Boolean,
    onSwitchRolePreview: (UserRole?) -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLaparPoints: () -> Unit,
    onOpenGroupOrder: () -> Unit,
    onCheckMidtransHealth: ((String) -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var nimOrUnit by remember(userProfile) { mutableStateOf(userProfile?.nimOrUnit ?: "") }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var healthStatus by remember {
        mutableStateOf(
            if (isBackendUrlConfigured) "MIDTRANS_BACKEND_URL dikonfigurasi. Tekan Uji Koneksi untuk verifikasi server."
            else "MIDTRANS_BACKEND_URL belum diisi pada Secrets panel AI Studio."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LaparTopBar(title = "Pengaturan Profil")

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(userProfile?.email ?: "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Peran Terdaftar: ${(userProfile?.role ?: "customer").uppercase()}", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nimOrUnit,
                        onValueChange = { nimOrUnit = it },
                        label = { Text("NIM / Unit Kampus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor WhatsApp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = !isSubmitting && displayName.trim().length >= 2,
                        onClick = { onSaveProfile(displayName, nimOrUnit, phone) },
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Simpan Perubahan Profil")
                    }
                }
            }

            if (isBootstrappedAdmin || userProfile?.role == UserRole.ADMIN.value) {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mode Tampilan Administrator", style = MaterialTheme.typography.titleSmall)
                        Text("Beralih tampilan untuk menguji alur Mahasiswa, UMKM, atau Admin:", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserRole.entries.forEach { role ->
                                val current = activeRoleOverride ?: UserRole.fromRaw(userProfile?.role)
                                FilterChip(
                                    selected = current == role,
                                    onClick = { onSwitchRolePreview(role) },
                                    label = { Text(role.value.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ProfileActionRow(Icons.Filled.Stars, "Lapar Points & Voucher", onOpenLaparPoints)
                    HorizontalDivider()
                    ProfileActionRow(Icons.Filled.Groups, "Group Order Kampus", onOpenGroupOrder)
                    HorizontalDivider()
                    ProfileActionRow(Icons.Filled.Notifications, "Kotak Notifikasi", onOpenNotifications)
                }
            }

            MidtransIntegrationStatusCard(
                isConfigured = isBackendUrlConfigured,
                statusMessage = healthStatus
            )
            OutlinedButton(
                onClick = { onCheckMidtransHealth { healthStatus = it } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Uji Koneksi Server Midtrans Sandbox")
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text("Keluar Akun")
            }
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
