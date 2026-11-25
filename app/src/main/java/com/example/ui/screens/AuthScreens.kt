package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.attemptAutoSignIn
import com.example.data.onGoogleSignInClicked
import com.example.model.UserRole
import com.example.ui.theme.LaparOrange

@Composable
fun LandingScreen(
    onGoToLogin: () -> Unit,
    onAutoSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAutoSignInSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LaparOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fastfood,
                        contentDescription = "Logo Lapar.id",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Lapar.id",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pre-Order Kantin Kampus",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_campus),
                    contentDescription = "Hidangan kantin kampus Lapar.id",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Surface(
                        color = LaparOrange,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Rp 0 Ongkos Kirim",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Pesan Sekarang,\nAmbil Tepat Waktu.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "Kenapa mahasiswa & UMKM kampus memilih Lapar.id?",
                style = MaterialTheme.typography.titleMedium
            )

            LandingBenefitRow(
                icon = Icons.Filled.Schedule,
                title = "Smart Pickup Scheduler",
                subtitle = "Pilih jam ambil di antara kelas tanpa perlu antre di kantin."
            )
            LandingBenefitRow(
                icon = Icons.Filled.QrCode2,
                title = "Verifikasi QR Instan",
                subtitle = "Tunjukkan QR Code unik saat mengambil pesanan yang sudah siap."
            )
            LandingBenefitRow(
                icon = Icons.Filled.Groups,
                title = "Group Order & Flash Deals",
                subtitle = "Pesan bareng teman sekelas dan nikmati promo menu harian."
            )
            LandingBenefitRow(
                icon = Icons.Filled.Storefront,
                title = "Dashboard Khusus UMKM",
                subtitle = "Kelola kuota harian, slot pengambilan, dan grafik penjualan realtime."
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onGoToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("landing_start_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mulai Sekarang (Masuk / Daftar)")
            }

            OutlinedButton(
                onClick = onGoToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("landing_seller_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Masuk sebagai UMKM / Admin")
            }
        }
    }
}

@Composable
private fun LandingBenefitRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(LaparOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Fastfood,
                    contentDescription = "Lapar.id",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = "Masuk ke Lapar.id",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Gunakan akun Google kampus atau pribadi Anda untuk akses Mahasiswa, Penjual UMKM, atau Admin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            errorMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                            errorMessage = "Masuk dibatalkan. Silakan pilih akun Google untuk melanjutkan."
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LaparOrange)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Masuk dengan Google")
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("login_back_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Kembali ke Halaman Utama")
            }
        }
    }
}

@Composable
fun RegisterScreen(
    initialName: String,
    email: String,
    isBootstrappedAdmin: Boolean,
    isSubmitting: Boolean,
    onSubmit: (
        displayName: String,
        role: UserRole,
        nimOrUnit: String,
        phone: String,
        businessName: String,
        campusArea: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember { mutableStateOf(initialName.ifBlank { email.substringBefore("@") }) }
    var selectedRole by remember {
        mutableStateOf(if (isBootstrappedAdmin) UserRole.ADMIN else UserRole.CUSTOMER)
    }
    var nimOrUnit by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var campusArea by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Lengkapi Pendaftaran Akun", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Terhubung sebagai $email. Pilih peran Anda di ekosistem Lapar.id.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Pilih Peran Akun", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedRole == UserRole.CUSTOMER,
                onClick = { selectedRole = UserRole.CUSTOMER },
                label = { Text("Mahasiswa") },
                modifier = Modifier.testTag("role_chip_customer")
            )
            FilterChip(
                selected = selectedRole == UserRole.SELLER,
                onClick = { selectedRole = UserRole.SELLER },
                label = { Text("UMKM Kantin") },
                modifier = Modifier.testTag("role_chip_seller")
            )
            if (isBootstrappedAdmin) {
                FilterChip(
                    selected = selectedRole == UserRole.ADMIN,
                    onClick = { selectedRole = UserRole.ADMIN },
                    label = { Text("Admin") },
                    modifier = Modifier.testTag("role_chip_admin")
                )
            }
        }

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Nama Lengkap") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_name_input")
        )

        OutlinedTextField(
            value = nimOrUnit,
            onValueChange = { nimOrUnit = it },
            label = { Text(if (selectedRole == UserRole.SELLER) "Fakultas / Lokasi Kantin" else "NIM / Program Studi") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_nim_input")
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Nomor WhatsApp Aktif") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_phone_input")
        )

        if (selectedRole == UserRole.SELLER) {
            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Nama Stan / Usaha Kantin") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_business_input")
            )
            OutlinedTextField(
                value = campusArea,
                onValueChange = { campusArea = it },
                label = { Text("Gedung / Area Kantin Kampus") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_area_input")
            )
        }

        Button(
            onClick = {
                onSubmit(displayName, selectedRole, nimOrUnit, phone, businessName, campusArea)
            },
            enabled = !isSubmitting && displayName.trim().length >= 2,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("register_submit_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Simpan & Masuk Dashboard")
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Ganti Akun Google")
        }
    }
}
