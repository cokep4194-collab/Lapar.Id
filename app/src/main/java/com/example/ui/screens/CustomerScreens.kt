package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FoodCategory
import com.example.model.PickupSlot
import com.example.model.Product
import com.example.model.Stall
import com.example.ui.components.FoodArtwork
import com.example.ui.components.LaparEmptyState
import com.example.ui.components.LaparProductCard
import com.example.ui.components.LaparStallCard
import com.example.ui.components.LaparTopBar
import com.example.ui.theme.LaparAmber
import com.example.ui.theme.LaparMint
import com.example.ui.theme.LaparOrange
import com.example.util.formatRupiah

@Composable
fun CustomerHomeScreen(
    userName: String,
    loyaltyPoints: Long,
    unreadNotifications: Int,
    cartCount: Int,
    categories: List<FoodCategory>,
    stalls: List<Stall>,
    products: List<Product>,
    searchQuery: String,
    selectedCategoryId: String?,
    maxPriceFilter: Long?,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectMaxPrice: (Long?) -> Unit,
    onOpenStallList: () -> Unit,
    onOpenStallDetail: (String) -> Unit,
    onOpenProductDetail: (String) -> Unit,
    onOpenCart: () -> Unit,
    onOpenGroupOrder: () -> Unit,
    onOpenLaparPoints: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val stallMap = remember(stalls) { stalls.associateBy { it.id } }
    val flashDeals = remember(products) { products.filter { it.flashDealActive && it.remainingStock > 0 } }
    val filteredProducts = remember(products, searchQuery, selectedCategoryId, maxPriceFilter, stallMap) {
        products.filter { p ->
            val stallName = stallMap[p.stallId]?.name.orEmpty()
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.categoryName.contains(searchQuery, ignoreCase = true) ||
                stallName.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategoryId == null ||
                p.categoryId == selectedCategoryId ||
                p.categoryName.equals(selectedCategoryId, ignoreCase = true)
            val matchesPrice = maxPriceFilter == null || p.effectivePrice <= maxPriceFilter
            matchesQuery && matchesCat && matchesPrice
        }
    }

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

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(
            title = "Halo, ${userName.substringBefore(" ")}!",
            subtitle = "Pesan Sekarang, Ambil Tepat Waktu",
            unreadNotifications = unreadNotifications,
            onOpenNotifications = onOpenNotifications
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero & Quick Feature Bar (Points, Cart, Group Order)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_campus),
                            contentDescription = "Promo Kantin Kampus",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.18f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Pre-Order Kantin Tanpa Antre",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Rp 0 ongkir • Ambil sesuai jadwal kuliahmu",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = LaparOrange,
                                    shape = RoundedCornerShape(50),
                                    onClick = onOpenGroupOrder,
                                    modifier = Modifier.testTag("home_group_order_chip")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Groups, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Group Order", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.94f),
                                    shape = RoundedCornerShape(50),
                                    onClick = onOpenLaparPoints,
                                    modifier = Modifier.testTag("home_points_chip")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Stars, null, tint = LaparAmber, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("$loyaltyPoints Poin", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1E1B18))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search & Price Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Cari nasi, mi, minuman, atau nama stan...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari") },
                        trailingIcon = {
                            if (cartCount > 0) {
                                IconButton(
                                    onClick = onOpenCart,
                                    modifier = Modifier.testTag("home_cart_shortcut")
                                ) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Keranjang")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("home_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { onSelectCategory(null) },
                                label = { Text("Semua") }
                            )
                        }
                        items(effectiveCategories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    onSelectCategory(if (selectedCategoryId == cat.id) null else cat.id)
                                },
                                label = { Text(cat.name) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = maxPriceFilter == 15000L,
                                onClick = { onSelectMaxPrice(if (maxPriceFilter == 15000L) null else 15000L) },
                                leadingIcon = { Icon(Icons.Filled.FilterList, null, modifier = Modifier.size(16.dp)) },
                                label = { Text("<= Rp 15rb") }
                            )
                        }
                    }
                }
            }

            // Flash Food Deals
            if (flashDeals.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = LaparOrange)
                            Spacer(Modifier.width(4.dp))
                            Text("Flash Food Deals", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Hemat & anti mubazir",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(flashDeals, key = { "flash_${it.id}" }) { product ->
                            LaparProductCard(
                                product = product,
                                stallName = stallMap[product.stallId]?.name ?: "Stan Kantin",
                                onClick = { onOpenProductDetail(product.id) },
                                modifier = Modifier.width(196.dp)
                            )
                        }
                    }
                }
            }

            // Popular Stalls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stan Kantin Populer", style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = onOpenStallList,
                        modifier = Modifier.testTag("see_all_stalls_button")
                    ) {
                        Text("Lihat Semua")
                    }
                }
                if (stalls.isEmpty()) {
                    LaparEmptyState(
                        title = "Belum Ada Stan Terdaftar",
                        subtitle = "Penjual UMKM kantin dapat mendaftar untuk menampilkan menu pre-order di sini."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stalls.take(3).forEach { stall ->
                            LaparStallCard(
                                stall = stall,
                                onClick = { onOpenStallDetail(stall.id) }
                            )
                        }
                    }
                }
            }

            // Food Recommendations / Search Results
            item {
                Text(
                    text = if (searchQuery.isBlank() && selectedCategoryId == null && maxPriceFilter == null)
                        "Rekomendasi Makanan" else "Hasil Pencarian (${filteredProducts.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (filteredProducts.isEmpty()) {
                item {
                    LaparEmptyState(
                        title = "Menu Tidak Ditemukan",
                        subtitle = "Coba ubah kata kunci pencarian atau pilih kategori lain."
                    )
                }
            } else {
                val rows = filteredProducts.chunked(2)
                items(rows) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { product ->
                            LaparProductCard(
                                product = product,
                                stallName = stallMap[product.stallId]?.name ?: "Stan Kantin",
                                onClick = { onOpenProductDetail(product.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StallListScreen(
    stalls: List<Stall>,
    onBack: () -> Unit,
    onOpenStallDetail: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(stalls, query) {
        stalls.filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.campusLocation.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = "Daftar Stan Kantin", onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari nama stan atau lokasi gedung...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    LaparEmptyState(
                        icon = Icons.Filled.Storefront,
                        title = "Belum Ada Stan",
                        subtitle = "Stan kantin yang aktif akan tampil di halaman ini."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { stall ->
                    LaparStallCard(stall = stall, onClick = { onOpenStallDetail(stall.id) })
                }
            }
        }
    }
}

@Composable
fun StallDetailScreen(
    stall: Stall?,
    products: List<Product>,
    slots: List<PickupSlot>,
    onBack: () -> Unit,
    onOpenProductDetail: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = stall?.name ?: "Detail Stan", onBack = onBack)
        if (stall == null) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(title = "Stan tidak ditemukan", subtitle = "Stan mungkin sudah tidak aktif.")
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        FoodArtwork(
                            imageUrl = stall.imageUrl,
                            imageKey = stall.bannerKey,
                            contentDescription = stall.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stall.name, style = MaterialTheme.typography.headlineSmall)
                                if (stall.verified) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Filled.Verified, "Terverifikasi", tint = LaparMint)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${stall.campusLocation} • Jam Buka ${stall.openHours}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(stall.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Text("Slot Pengambilan Tersedia", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (slots.isEmpty()) {
                    Text(
                        text = "Penjual belum menambahkan slot jadwal pengambilan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(slots, key = { it.id }) { slot ->
                            Surface(
                                color = if (slot.remainingCapacity > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text("${slot.date} • ${slot.label}", style = MaterialTheme.typography.labelMedium)
                                    Text("Sisa kuota: ${slot.remainingCapacity}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Daftar Menu (${products.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (products.isEmpty()) {
                item {
                    LaparEmptyState(title = "Menu Belum Tersedia", subtitle = "Stan ini belum menambahkan menu aktif.")
                }
            } else {
                val rows = products.chunked(2)
                items(rows) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { p ->
                            LaparProductCard(
                                product = p,
                                stallName = p.categoryName,
                                onClick = { onOpenProductDetail(p.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(
    product: Product?,
    stall: Stall?,
    onBack: () -> Unit,
    onAddToCart: (Product, Stall, Int, String) -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    var note by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LaparTopBar(title = product?.name ?: "Detail Menu", onBack = onBack)
        if (product == null || stall == null) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                LaparEmptyState(title = "Menu tidak ditemukan", subtitle = "Produk mungkin telah dinonaktifkan penjual.")
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
            FoodArtwork(
                imageUrl = product.imageUrl,
                imageKey = product.imageKey,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${stall.name} • ${product.categoryName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                val stockMsg = when {
                    product.remainingStock <= 0L -> "Kuota Habis"
                    product.remainingStock <= 5L -> "Hampir Habis! Sisa ${product.remainingStock}/${product.dailyQuota}"
                    else -> "Tersedia ${product.remainingStock}/${product.dailyQuota} porsi"
                }
                Text(
                    text = stockMsg,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (product.remainingStock <= 5L) LaparOrange else LaparMint
                )
            }

            Text(product.name, style = MaterialTheme.typography.headlineMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = formatRupiah(product.effectivePrice),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (product.flashDealActive && product.discountPrice != null && product.discountPrice < product.price) {
                    Text(
                        text = formatRupiah(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }

            Text(
                text = product.description.ifBlank { "Menu segar disiapkan khusus sesuai jadwal pre-order Anda." },
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan khusus (opsional, mis. tidak pedas / sambal pisah)") },
                modifier = Modifier.fillMaxWidth().testTag("product_note_input")
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jumlah Pesanan", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.testTag("qty_minus_button")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Kurangi")
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(
                            onClick = { if (quantity < product.remainingStock) quantity++ },
                            modifier = Modifier.testTag("qty_plus_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Tambah")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onAddToCart(product, stall, quantity, note)
                    onBack()
                },
                enabled = product.remainingStock > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("add_to_cart_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (product.remainingStock > 0L)
                        "Tambah ke Keranjang • ${formatRupiah(product.effectivePrice * quantity)}"
                    else "Kuota Hari Ini Habis"
                )
            }
        }
    }
}
