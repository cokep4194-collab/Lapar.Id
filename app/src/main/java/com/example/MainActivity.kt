package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.LaparRepository
import com.example.data.MidtransService
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.GroupOrderScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.LaparPointsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrderHistoryScreen
import com.example.ui.screens.PaymentScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.screens.SellerOrdersScreen
import com.example.ui.screens.SellerProductsScreen
import com.example.ui.screens.SellerReportsScreen
import com.example.ui.screens.StallDetailScreen
import com.example.ui.screens.StallListScreen
import com.example.ui.theme.LaparIdTheme
import com.example.viewmodel.LaparViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
    const val CUSTOMER_HOME = "customer_home"
    const val STALL_LIST = "stall_list"
    const val STALL_DETAIL = "stall_detail/{stallId}"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val PAYMENT = "payment/{paymentGroupId}"
    const val ORDER_HISTORY = "order_history"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val GROUP_ORDER = "group_order"
    const val LAPAR_POINTS = "lapar_points"
    const val NOTIFICATIONS = "notifications"
    const val SELLER_DASHBOARD = "seller_dashboard"
    const val SELLER_PRODUCTS = "seller_products"
    const val SELLER_ORDERS = "seller_orders"
    const val SELLER_REPORTS = "seller_reports"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val PROFILE = "profile"

    fun stallDetail(id: String) = "stall_detail/$id"
    fun productDetail(id: String) = "product_detail/$id"
    fun payment(id: String) = "payment/$id"
    fun orderDetail(id: String) = "order_detail/$id"
}

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaparIdTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        var showLogin by remember { mutableStateOf(false) }
        if (showLogin) {
            BackHandler { showLogin = false }
            LoginScreen(
                onBack = { showLogin = false },
                onAuthSuccess = { showLogin = false }
            )
        } else {
            LandingScreen(
                onGoToLogin = { showLogin = true },
                onAutoSignInSuccess = { showLogin = false }
            )
        }
    } else {
        val uid = user.uid
        val email = user.email ?: ""
        val displayName = user.displayName ?: "Pengguna"

        key(uid) {
            val viewModel: LaparViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        LaparViewModel(
                            repository = LaparRepository(db),
                            midtransService = MidtransService(),
                            currentUserId = uid,
                            currentUserEmail = email,
                            currentUserDisplayName = displayName
                        )
                    }
                }
            )
            AuthenticatedAppGate(viewModel = viewModel)
        }
    }
}

@Composable
private fun AuthenticatedAppGate(viewModel: LaparViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val performSignOut = {
        signOut(credentialManager, scope)
    }

    when (val state = profileState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat akun Lapar.id...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gagal Memuat Profil", style = MaterialTheme.typography.titleLarge)
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = performSignOut) { Text("Keluar & Masuk Ulang") }
                }
            }
        }
        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        RegisterScreen(
                            initialName = viewModel.currentUserDisplayName,
                            email = viewModel.currentUserEmail,
                            isBootstrappedAdmin = viewModel.isBootstrappedAdmin,
                            isSubmitting = isSubmitting,
                            onSubmit = { name, role, nim, phone, biz, area ->
                                viewModel.registerProfile(name, role, nim, phone, biz, area)
                            },
                            onSignOut = performSignOut
                        )
                    }
                }
            } else {
                MainRoleShell(
                    viewModel = viewModel,
                    userProfile = profile,
                    snackbarHostState = snackbarHostState,
                    onSignOut = performSignOut
                )
            }
        }
    }
}

@Composable
private fun MainRoleShell(
    viewModel: LaparViewModel,
    userProfile: UserProfile,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val roleOverride by viewModel.activeRoleOverride.collectAsStateWithLifecycle()
    val effectiveRole = if (viewModel.isBootstrappedAdmin || userProfile.role == UserRole.ADMIN.value) {
        roleOverride ?: UserRole.fromRaw(userProfile.role)
    } else {
        UserRole.fromRaw(userProfile.role)
    }

    val stalls by viewModel.activeStalls.collectAsStateWithLifecycle()
    val categories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val slots by viewModel.activePickupSlots.collectAsStateWithLifecycle()
    val customerOrders by viewModel.customerOrders.collectAsStateWithLifecycle()
    val customerPayments by viewModel.customerPayments.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val loyaltyPoints by viewModel.loyaltyPoints.collectAsStateWithLifecycle()
    val catalogVouchers by viewModel.catalogVouchers.collectAsStateWithLifecycle()
    val myVouchers by viewModel.myVouchers.collectAsStateWithLifecycle()
    val openGroups by viewModel.openGroupOrders.collectAsStateWithLifecycle()
    val myGroups by viewModel.myGroupOrders.collectAsStateWithLifecycle()

    val sellerStalls by viewModel.sellerStalls.collectAsStateWithLifecycle()
    val sellerProducts by viewModel.sellerProducts.collectAsStateWithLifecycle()
    val sellerSlots by viewModel.sellerPickupSlots.collectAsStateWithLifecycle()
    val sellerOrders by viewModel.sellerOrders.collectAsStateWithLifecycle()

    val allUsersAdmin by viewModel.allUsersAdmin.collectAsStateWithLifecycle()
    val allSellersAdmin by viewModel.allSellersAdmin.collectAsStateWithLifecycle()
    val allStallsAdmin by viewModel.allStallsAdmin.collectAsStateWithLifecycle()
    val allCategoriesAdmin by viewModel.allCategoriesAdmin.collectAsStateWithLifecycle()
    val allOrdersAdmin by viewModel.allOrdersAdmin.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val maxPriceFilter by viewModel.maxPriceFilter.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedSlotsByStall by viewModel.selectedSlotsByStall.collectAsStateWithLifecycle()
    val selectedVoucher by viewModel.selectedVoucher.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val midtransUiState by viewModel.midtransUiState.collectAsStateWithLifecycle()

    val unreadNotifications = remember(notifications) { notifications.count { !it.read } }
    val cartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }

    val startRoute = when (effectiveRole) {
        UserRole.CUSTOMER -> Routes.CUSTOMER_HOME
        UserRole.SELLER -> Routes.SELLER_DASHBOARD
        UserRole.ADMIN -> Routes.ADMIN_DASHBOARD
    }

    LaunchedEffect(effectiveRole) {
        navController.navigate(startRoute) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    val navTabs = when (effectiveRole) {
        UserRole.CUSTOMER -> listOf(
            NavTab(Routes.CUSTOMER_HOME, "Beranda", Icons.Filled.Home),
            NavTab(Routes.STALL_LIST, "Stan", Icons.Filled.Storefront),
            NavTab(Routes.CART, "Keranjang", Icons.Filled.ShoppingCart, cartCount),
            NavTab(Routes.ORDER_HISTORY, "Pesanan", Icons.Filled.ReceiptLong),
            NavTab(Routes.PROFILE, "Profil", Icons.Filled.Person)
        )
        UserRole.SELLER -> listOf(
            NavTab(Routes.SELLER_DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
            NavTab(Routes.SELLER_PRODUCTS, "Produk", Icons.Filled.Inventory),
            NavTab(Routes.SELLER_ORDERS, "Pesanan", Icons.Filled.ReceiptLong),
            NavTab(Routes.SELLER_REPORTS, "Laporan", Icons.Filled.Assessment),
            NavTab(Routes.PROFILE, "Profil", Icons.Filled.Person)
        )
        UserRole.ADMIN -> listOf(
            NavTab(Routes.ADMIN_DASHBOARD, "Admin", Icons.Filled.AdminPanelSettings),
            NavTab(Routes.STALL_LIST, "Stan", Icons.Filled.Storefront),
            NavTab(Routes.ORDER_HISTORY, "Pesanan", Icons.Filled.ReceiptLong),
            NavTab(Routes.PROFILE, "Profil", Icons.Filled.Person)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopTab = navTabs.any { tab -> currentDestination?.hierarchy?.any { it.route == tab.route } == true }

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (!useRail && isTopTab) {
                    NavigationBar {
                        navTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.badgeCount > 0) {
                                                Badge { Text(tab.badgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && isTopTab) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.badgeCount > 0) {
                                                Badge { Text(tab.badgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startRoute,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    composable(Routes.CUSTOMER_HOME) {
                        CustomerHomeScreen(
                            userName = userProfile.displayName,
                            loyaltyPoints = loyaltyPoints?.points ?: 0L,
                            unreadNotifications = unreadNotifications,
                            cartCount = cartCount,
                            categories = categories,
                            stalls = stalls,
                            products = products,
                            searchQuery = searchQuery,
                            selectedCategoryId = selectedCategoryId,
                            maxPriceFilter = maxPriceFilter,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategoryFilter,
                            onSelectMaxPrice = viewModel::updateMaxPriceFilter,
                            onOpenStallList = { navigateTab(Routes.STALL_LIST) },
                            onOpenStallDetail = { navController.navigate(Routes.stallDetail(it)) },
                            onOpenProductDetail = { navController.navigate(Routes.productDetail(it)) },
                            onOpenCart = { navigateTab(Routes.CART) },
                            onOpenGroupOrder = { navController.navigate(Routes.GROUP_ORDER) },
                            onOpenLaparPoints = { navController.navigate(Routes.LAPAR_POINTS) },
                            onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) }
                        )
                    }

                    composable(Routes.STALL_LIST) {
                        StallListScreen(
                            stalls = stalls,
                            onBack = { navController.popBackStack() },
                            onOpenStallDetail = { navController.navigate(Routes.stallDetail(it)) }
                        )
                    }

                    composable(
                        route = Routes.STALL_DETAIL,
                        arguments = listOf(navArgument("stallId") { type = NavType.StringType })
                    ) { backStack ->
                        val stallId = backStack.arguments?.getString("stallId") ?: ""
                        val stall = stalls.firstOrNull { it.id == stallId } ?: allStallsAdmin.firstOrNull { it.id == stallId }
                        StallDetailScreen(
                            stall = stall,
                            products = products.filter { it.stallId == stallId },
                            slots = slots.filter { it.stallId == stallId },
                            onBack = { navController.popBackStack() },
                            onOpenProductDetail = { navController.navigate(Routes.productDetail(it)) }
                        )
                    }

                    composable(
                        route = Routes.PRODUCT_DETAIL,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { backStack ->
                        val pid = backStack.arguments?.getString("productId") ?: ""
                        val product = products.firstOrNull { it.id == pid }
                        val stall = product?.let { p -> stalls.firstOrNull { it.id == p.stallId } }
                        ProductDetailScreen(
                            product = product,
                            stall = stall,
                            onBack = { navController.popBackStack() },
                            onAddToCart = viewModel::addToCart
                        )
                    }

                    composable(Routes.CART) {
                        CartScreen(
                            cartItems = cartItems,
                            onBack = { navController.popBackStack() },
                            onUpdateQty = viewModel::updateCartQuantity,
                            onGoToCheckout = { navController.navigate(Routes.CHECKOUT) }
                        )
                    }

                    composable(Routes.CHECKOUT) {
                        CheckoutScreen(
                            cartItems = cartItems,
                            allSlots = slots,
                            selectedSlotsByStall = selectedSlotsByStall,
                            myVouchers = myVouchers,
                            selectedVoucher = selectedVoucher,
                            isSubmitting = isSubmitting,
                            onBack = { navController.popBackStack() },
                            onSelectSlot = viewModel::selectPickupSlotForStall,
                            onSelectVoucher = viewModel::selectVoucher,
                            onConfirmCheckout = {
                                viewModel.checkoutAndCreatePayment { paymentGroupId ->
                                    navController.navigate(Routes.payment(paymentGroupId)) {
                                        popUpTo(Routes.CART)
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = Routes.PAYMENT,
                        arguments = listOf(navArgument("paymentGroupId") { type = NavType.StringType })
                    ) { backStack ->
                        val payId = backStack.arguments?.getString("paymentGroupId") ?: ""
                        val payment = customerPayments.firstOrNull { it.paymentGroupId == payId || it.id == payId }
                        PaymentScreen(
                            payment = payment,
                            midtransUiState = midtransUiState,
                            onBack = { navController.popBackStack() },
                            onRetryMidtrans = viewModel::initiateMidtransSnap,
                            onOpenOrders = {
                                navController.navigate(Routes.ORDER_HISTORY) {
                                    popUpTo(Routes.CUSTOMER_HOME)
                                }
                            }
                        )
                    }

                    composable(Routes.ORDER_HISTORY) {
                        OrderHistoryScreen(
                            orders = customerOrders,
                            onBack = null,
                            onOpenOrderDetail = { navController.navigate(Routes.orderDetail(it)) }
                        )
                    }

                    composable(
                        route = Routes.ORDER_DETAIL,
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { backStack ->
                        val oid = backStack.arguments?.getString("orderId") ?: ""
                        val order = customerOrders.firstOrNull { it.id == oid }
                            ?: allOrdersAdmin.firstOrNull { it.id == oid }
                        OrderDetailScreen(
                            order = order,
                            onBack = { navController.popBackStack() },
                            onOpenPayment = { payId ->
                                viewModel.initiateMidtransSnap(payId)
                                navController.navigate(Routes.payment(payId))
                            },
                            onCancelPendingOrder = viewModel::cancelOrder
                        )
                    }

                    composable(Routes.GROUP_ORDER) {
                        GroupOrderScreen(
                            currentUserId = viewModel.currentUserId,
                            stalls = stalls,
                            products = products,
                            slots = slots,
                            openGroups = openGroups,
                            myGroups = myGroups,
                            onBack = { navController.popBackStack() },
                            onCreateGroup = { stall, title, slot, mins ->
                                viewModel.createGroupOrder(stall, title, slot, mins) {}
                            },
                            onAddItemToGroup = viewModel::joinGroupAddItem,
                            onCloseGroup = viewModel::closeGroupOrder,
                            onCheckoutGroup = { grp ->
                                viewModel.checkoutGroupOrder(grp) { payId ->
                                    navController.navigate(Routes.payment(payId))
                                }
                            }
                        )
                    }

                    composable(Routes.LAPAR_POINTS) {
                        LaparPointsScreen(
                            loyaltyPoints = loyaltyPoints,
                            catalogVouchers = catalogVouchers,
                            myVouchers = myVouchers,
                            isSubmitting = isSubmitting,
                            onBack = { navController.popBackStack() },
                            onRedeemVoucher = viewModel::redeemVoucher
                        )
                    }

                    composable(Routes.NOTIFICATIONS) {
                        NotificationsScreen(
                            notifications = notifications,
                            onBack = { navController.popBackStack() },
                            onMarkReadAndOpenOrder = { notif ->
                                viewModel.markNotificationRead(notif.id)
                                if (!notif.relatedOrderId.isNullOrBlank()) {
                                    navController.navigate(Routes.orderDetail(notif.relatedOrderId))
                                }
                            }
                        )
                    }

                    composable(Routes.SELLER_DASHBOARD) {
                        SellerDashboardScreen(
                            stall = sellerStalls.firstOrNull(),
                            products = sellerProducts,
                            orders = sellerOrders,
                            unreadNotifications = unreadNotifications,
                            onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                            onOpenProducts = { navigateTab(Routes.SELLER_PRODUCTS) },
                            onOpenOrders = { navigateTab(Routes.SELLER_ORDERS) },
                            onOpenReports = { navigateTab(Routes.SELLER_REPORTS) },
                            onSaveStall = viewModel::saveStall
                        )
                    }

                    composable(Routes.SELLER_PRODUCTS) {
                        SellerProductsScreen(
                            stall = sellerStalls.firstOrNull(),
                            categories = categories,
                            products = sellerProducts,
                            slots = sellerSlots,
                            onSaveProduct = viewModel::saveProduct,
                            onToggleProductActive = viewModel::toggleProductActive,
                            onDeleteProduct = viewModel::deleteProduct,
                            onSaveSlot = viewModel::savePickupSlot,
                            onDeleteSlot = viewModel::deletePickupSlot
                        )
                    }

                    composable(Routes.SELLER_ORDERS) {
                        SellerOrdersScreen(
                            orders = sellerOrders,
                            onAdvanceOrder = viewModel::advanceSellerOrder,
                            onVerifyQrCode = viewModel::verifyQrPickupCode
                        )
                    }

                    composable(Routes.SELLER_REPORTS) {
                        SellerReportsScreen(orders = sellerOrders)
                    }

                    composable(Routes.ADMIN_DASHBOARD) {
                        AdminDashboardScreen(
                            users = allUsersAdmin,
                            sellers = allSellersAdmin,
                            stalls = allStallsAdmin,
                            categories = allCategoriesAdmin,
                            orders = allOrdersAdmin,
                            onUpdateUser = viewModel::adminUpdateUser,
                            onVerifySeller = viewModel::adminVerifySeller,
                            onSaveCategory = viewModel::saveCategory,
                            onDeleteCategory = viewModel::deleteCategory,
                            onSeedDefaults = viewModel::seedDefaultCategoriesAndVouchers
                        )
                    }

                    composable(Routes.PROFILE) {
                        ProfileScreen(
                            userProfile = userProfile,
                            isBootstrappedAdmin = viewModel.isBootstrappedAdmin,
                            activeRoleOverride = roleOverride,
                            isBackendUrlConfigured = BuildConfig.MIDTRANS_BACKEND_URL.startsWith("http"),
                            isSubmitting = isSubmitting,
                            onSwitchRolePreview = viewModel::setAdminPreviewRole,
                            onSaveProfile = viewModel::updateOwnProfile,
                            onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                            onOpenLaparPoints = { navController.navigate(Routes.LAPAR_POINTS) },
                            onOpenGroupOrder = { navController.navigate(Routes.GROUP_ORDER) },
                            onCheckMidtransHealth = viewModel::checkMidtransBackendHealth,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
