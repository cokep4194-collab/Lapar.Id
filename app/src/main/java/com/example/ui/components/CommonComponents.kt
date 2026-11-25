package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Stall
import com.example.ui.theme.LaparAmber
import com.example.ui.theme.LaparCrimson
import com.example.ui.theme.LaparMint
import com.example.ui.theme.LaparOrange
import com.example.util.QrCodeGenerator
import com.example.util.drawableForKey
import com.example.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaparTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    unreadNotifications: Int = 0,
    onOpenNotifications: (() -> Unit)? = null
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            if (onOpenNotifications != null) {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("notifications_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge { Text(unreadNotifications.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifikasi"
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun FoodArtwork(
    imageUrl: String?,
    imageKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = drawableForKey(imageKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun LaparProductCard(
    product: Product,
    stallName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowStock = product.remainingStock in 1..5
    val isSoldOut = product.remainingStock <= 0L

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                FoodArtwork(
                    imageUrl = product.imageUrl,
                    imageKey = product.imageKey,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f))
                            )
                        )
                )
                if (product.flashDealActive && product.discountPrice != null) {
                    Surface(
                        color = LaparOrange,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = product.flashDealLabel?.ifBlank { "Flash Deal" } ?: "Flash Deal",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Surface(
                    color = when {
                        isSoldOut -> LaparCrimson
                        isLowStock -> LaparAmber
                        else -> Color.Black.copy(alpha = 0.65f)
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = when {
                            isSoldOut -> "Habis"
                            isLowStock -> "Sisa ${product.remainingStock}!"
                            else -> "Stok ${product.remainingStock}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stallName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatRupiah(product.effectivePrice),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (product.flashDealActive && product.discountPrice != null && product.discountPrice < product.price) {
                        Text(
                            text = formatRupiah(product.price),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LaparStallCard(
    stall: Stall,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("stall_card_${stall.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodArtwork(
                imageUrl = stall.imageUrl,
                imageKey = stall.bannerKey,
                contentDescription = stall.name,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stall.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (stall.verified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "UMKM Terverifikasi",
                            tint = LaparMint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stall.campusLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = LaparAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", stall.rating),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = stall.openHours,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LaparStatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING_PAYMENT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.PAID -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.PREPARING -> Color(0xFFDBEAFE) to Color(0xFF1E3A8A)
        OrderStatus.READY_FOR_PICKUP -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.EXPIRED, OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LaparMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LaparBarChart(
    title: String,
    entries: List<Pair<String, Float>>,
    barColor: Color = LaparOrange,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text(
                    text = "Belum ada data transaksi untuk grafik ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = (entries.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val count = entries.size
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.54f).coerceAtMost(44.dp.toPx())
                    entries.forEachIndexed { idx, (_, v) ->
                        val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        val barH = (v / maxVal) * size.height
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, size.height - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    entries.forEach { (label, valFloat) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = valFloat.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LaparEmptyState(
    icon: ImageVector = Icons.Filled.Storefront,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QrCodeCard(
    qrToken: String,
    orderCode: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(qrToken) { QrCodeGenerator.generateQrBitmap(qrToken, 512) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text("QR Verifikasi Pengambilan", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code pesanan $orderCode",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = qrToken,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tunjukkan QR ini ke penjual saat mengambil makanan di stan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MidtransIntegrationStatusCard(
    isConfigured: Boolean,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfigured) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isConfigured) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isConfigured) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isConfigured) "Midtrans Sandbox Aktif" else "Konfigurasi Midtrans Sandbox Diperlukan",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall
            )
            if (!isConfigured) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Langkah aktivasi:\n1. Jalankan server di folder backend/ dengan MIDTRANS_SERVER_KEY Sandbox.\n2. Atur MIDTRANS_BACKEND_URL di Secrets panel AI Studio.\n3. Daftarkan webhook /api/payments/midtrans-webhook di Midtrans Dashboard Sandbox.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
