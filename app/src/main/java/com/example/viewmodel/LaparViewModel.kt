package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LaparRepository
import com.example.data.MidtransService
import com.example.data.MidtransSnapResult
import com.example.model.AppNotification
import com.example.model.CartItem
import com.example.model.FoodCategory
import com.example.model.GroupOrder
import com.example.model.LoyaltyPoints
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PaymentRecord
import com.example.model.PickupSlot
import com.example.model.Product
import com.example.model.SellerProfile
import com.example.model.Stall
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.Voucher
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MidtransUiState {
    data object Idle : MidtransUiState
    data object Loading : MidtransUiState
    data class Ready(val snapToken: String, val redirectUrl: String) : MidtransUiState
    data class NotConfigured(val reason: String) : MidtransUiState
    data class Error(val message: String) : MidtransUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class LaparViewModel(
    private val repository: LaparRepository,
    private val midtransService: MidtransService,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    private val _feedback = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val feedback = _feedback.asSharedFlow()

    private fun <T> Flow<T>.asUiState(tag: String, fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "$tag stream error", rootError)
                emit(UiState.Error(rootError.message ?: fallbackMsg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private fun <T> Flow<List<T>>.asListState(tag: String): StateFlow<List<T>> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "$tag list error", firestoreError ?: error)
            emit(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .asUiState("UserProfile", "Gagal memuat profil pengguna.")

    val isBootstrappedAdmin: Boolean =
        currentUserEmail.equals("raflisyawal2005@gmail.com", ignoreCase = true)

    private val _activeRoleOverride = MutableStateFlow<UserRole?>(null)
    val activeRoleOverride = _activeRoleOverride.asStateFlow()

    fun setAdminPreviewRole(role: UserRole?) {
        _activeRoleOverride.value = role
    }

    // Shared Catalog Streams
    val activeStalls: StateFlow<List<Stall>> = repository.observeActiveStalls().asListState("Stalls")
    val activeCategories: StateFlow<List<FoodCategory>> = repository.observeActiveCategories().asListState("Categories")
    val activeProducts: StateFlow<List<Product>> = repository.observeActiveProducts().asListState("Products")
    val activePickupSlots: StateFlow<List<PickupSlot>> = repository.observeActivePickupSlots().asListState("PickupSlots")

    // Customer Streams
    val customerOrders: StateFlow<List<Order>> = repository.observeCustomerOrders(currentUserId).asListState("CustomerOrders")
    val customerPayments: StateFlow<List<PaymentRecord>> = repository.observeCustomerPayments(currentUserId).asListState("CustomerPayments")
    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId).asListState("Notifications")
    val loyaltyPoints: StateFlow<LoyaltyPoints?> = repository.observeLoyaltyPoints(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
    val catalogVouchers: StateFlow<List<Voucher>> = repository.observeCatalogVouchers().asListState("CatalogVouchers")
    val myVouchers: StateFlow<List<Voucher>> = repository.observeMyVouchers(currentUserId).asListState("MyVouchers")
    val openGroupOrders: StateFlow<List<GroupOrder>> = repository.observeOpenGroupOrders().asListState("OpenGroups")
    val myGroupOrders: StateFlow<List<GroupOrder>> = repository.observeMyGroupOrders(currentUserId).asListState("MyGroups")

    // Role-gated Seller Streams
    private val isSellerActiveFlow: Flow<Boolean> = userProfileState.map { state ->
        val role = (state as? UiState.Success)?.data?.role
        role == UserRole.SELLER.value || isBootstrappedAdmin
    }

    val sellerStalls: StateFlow<List<Stall>> = isSellerActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeSellerStalls(currentUserId) else flowOf(emptyList()) }
        .asListState("SellerStalls")

    val sellerProducts: StateFlow<List<Product>> = isSellerActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeSellerProducts(currentUserId) else flowOf(emptyList()) }
        .asListState("SellerProducts")

    val sellerPickupSlots: StateFlow<List<PickupSlot>> = isSellerActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeSellerPickupSlots(currentUserId) else flowOf(emptyList()) }
        .asListState("SellerSlots")

    val sellerOrders: StateFlow<List<Order>> = isSellerActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeSellerOrders(currentUserId) else flowOf(emptyList()) }
        .asListState("SellerOrders")

    // Role-gated Admin Streams
    private val isAdminActiveFlow: Flow<Boolean> = userProfileState.map { state ->
        val role = (state as? UiState.Success)?.data?.role
        role == UserRole.ADMIN.value || isBootstrappedAdmin
    }

    val allUsersAdmin: StateFlow<List<UserProfile>> = isAdminActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeAllUsers() else flowOf(emptyList()) }
        .asListState("AdminUsers")

    val allSellersAdmin: StateFlow<List<SellerProfile>> = isAdminActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeAllSellers() else flowOf(emptyList()) }
        .asListState("AdminSellers")

    val allStallsAdmin: StateFlow<List<Stall>> = isAdminActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeAllStalls() else flowOf(emptyList()) }
        .asListState("AdminStalls")

    val allCategoriesAdmin: StateFlow<List<FoodCategory>> = isAdminActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeAllCategories() else flowOf(emptyList()) }
        .asListState("AdminCategories")

    val allOrdersAdmin: StateFlow<List<Order>> = isAdminActiveFlow
        .flatMapLatest { enabled -> if (enabled) repository.observeAllOrders() else flowOf(emptyList()) }
        .asListState("AdminOrders")

    // Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _maxPriceFilter = MutableStateFlow<Long?>(null)
    val maxPriceFilter = _maxPriceFilter.asStateFlow()

    private val _selectedStallFilterId = MutableStateFlow<String?>(null)
    val selectedStallFilterId = _selectedStallFilterId.asStateFlow()

    fun updateSearchQuery(q: String) { _searchQuery.value = q }
    fun selectCategoryFilter(catId: String?) { _selectedCategoryId.value = catId }
    fun updateMaxPriceFilter(maxPrice: Long?) { _maxPriceFilter.value = maxPrice }
    fun updateStallFilter(stallId: String?) { _selectedStallFilterId.value = stallId }

    // Multi-stall Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems = _cartItems.asStateFlow()

    private val _selectedSlotsByStall = MutableStateFlow<Map<String, PickupSlot>>(emptyMap())
    val selectedSlotsByStall = _selectedSlotsByStall.asStateFlow()

    private val _selectedVoucher = MutableStateFlow<Voucher?>(null)
    val selectedVoucher = _selectedVoucher.asStateFlow()

    private val _activeGroupOrderCode = MutableStateFlow<String?>(null)
    val activeGroupOrderCode = _activeGroupOrderCode.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    private val _midtransUiState = MutableStateFlow<MidtransUiState>(MidtransUiState.Idle)
    val midtransUiState = _midtransUiState.asStateFlow()

    fun addToCart(product: Product, stall: Stall, quantity: Int, note: String) {
        if (product.remainingStock <= 0L) {
            emitFeedback("Maaf, kuota ${product.name} hari ini sudah habis.")
            return
        }
        val current = _cartItems.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.product.id == product.id && it.note == note.trim() }
        if (existingIdx >= 0) {
            val prev = current[existingIdx]
            val nextQty = (prev.quantity + quantity).coerceAtMost(product.remainingStock.toInt())
            current[existingIdx] = prev.copy(quantity = nextQty)
        } else {
            current.add(
                CartItem(
                    product = product,
                    stall = stall,
                    quantity = quantity.coerceIn(1, product.remainingStock.toInt()),
                    note = note.trim()
                )
            )
        }
        _cartItems.value = current
        emitFeedback("${product.name} ditambahkan ke keranjang.")
    }

    fun updateCartQuantity(productId: String, note: String, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == productId && it.note == note }
        if (idx >= 0) {
            val item = current[idx]
            val next = item.quantity + delta
            if (next <= 0) {
                current.removeAt(idx)
            } else {
                current[idx] = item.copy(quantity = next.coerceAtMost(item.product.remainingStock.toInt().coerceAtLeast(1)))
            }
            _cartItems.value = current
        }
    }

    fun selectPickupSlotForStall(stallId: String, slot: PickupSlot) {
        _selectedSlotsByStall.value = _selectedSlotsByStall.value + (stallId to slot)
    }

    fun selectVoucher(voucher: Voucher?) {
        _selectedVoucher.value = voucher
    }

    fun attachGroupOrderCode(code: String?) {
        _activeGroupOrderCode.value = code?.takeIf { it.isNotBlank() }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedSlotsByStall.value = emptyMap()
        _selectedVoucher.value = null
        _activeGroupOrderCode.value = null
    }

    // --- ACTIONS ---

    fun registerProfile(
        displayName: String,
        role: UserRole,
        nimOrUnit: String,
        phone: String,
        businessName: String,
        campusArea: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.registerProfile(displayName, role, nimOrUnit, phone, businessName, campusArea)
            _isSubmitting.value = false
            res.onSuccess { emitFeedback("Profil berhasil dibuat! Selamat datang di Lapar.id.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal mendaftarkan profil.") }
        }
    }

    fun updateOwnProfile(displayName: String, nimOrUnit: String, phone: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.updateOwnProfile(displayName, nimOrUnit, phone)
                .onSuccess { emitFeedback("Profil diperbarui.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal memperbarui profil.") }
            _isSubmitting.value = false
        }
    }

    fun checkoutAndCreatePayment(onPaymentCreated: (String) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            emitFeedback("Keranjang masih kosong.")
            return
        }
        val grouped = items.groupBy { it.stall.id }
        val slots = _selectedSlotsByStall.value
        val missingStall = grouped.keys.firstOrNull { slots[it] == null }
        if (missingStall != null) {
            emitFeedback("Pilih jadwal pengambilan untuk setiap stan terlebih dahulu.")
            return
        }

        val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.placePreOrders(
                customerName = profileName,
                cartByStall = grouped,
                selectedSlotsByStall = slots,
                appliedVoucher = _selectedVoucher.value,
                groupOrderCode = _activeGroupOrderCode.value
            )
            _isSubmitting.value = false
            res.onSuccess { paymentGroupId ->
                clearCart()
                emitFeedback("Pesanan dibuat! Silakan lanjutkan pembayaran Midtrans.")
                onPaymentCreated(paymentGroupId)
                initiateMidtransSnap(paymentGroupId)
            }.onFailure { err ->
                emitFeedback(err.localizedMessage ?: "Gagal memproses checkout.")
            }
        }
    }

    fun initiateMidtransSnap(paymentGroupId: String) {
        viewModelScope.launch {
            _midtransUiState.value = MidtransUiState.Loading
            when (val res = midtransService.requestSnapToken(paymentGroupId)) {
                is MidtransSnapResult.Success -> {
                    _midtransUiState.value = MidtransUiState.Ready(res.snapToken, res.redirectUrl)
                }
                is MidtransSnapResult.NotConfigured -> {
                    _midtransUiState.value = MidtransUiState.NotConfigured(res.reason)
                }
                is MidtransSnapResult.Error -> {
                    _midtransUiState.value = MidtransUiState.Error(res.message)
                }
            }
        }
    }

    fun resetMidtransState() {
        _midtransUiState.value = MidtransUiState.Idle
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            repository.cancelPendingOrder(order)
                .onSuccess { emitFeedback("Pesanan ${order.orderCode} dibatalkan.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal membatalkan pesanan.") }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun redeemVoucher(voucher: Voucher) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.redeemVoucherWithPoints(voucher)
                .onSuccess { emitFeedback("Voucher ${voucher.code} berhasil ditukarkan!") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menukar poin.") }
            _isSubmitting.value = false
        }
    }

    // Group Order actions
    fun createGroupOrder(stall: Stall, title: String, slot: PickupSlot, closesInMinutes: Int, onCreated: (String) -> Unit) {
        val name = (userProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createGroupOrder(name, stall, title, slot, closesInMinutes)
                .onSuccess { code ->
                    emitFeedback("Group Order $code dibuat!")
                    onCreated(code)
                }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal membuat Group Order.") }
            _isSubmitting.value = false
        }
    }

    fun joinGroupAddItem(group: GroupOrder, product: Product, quantity: Int, note: String) {
        val name = (userProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName
        viewModelScope.launch {
            repository.addItemToGroupOrder(group, name, product, quantity, note)
                .onSuccess { emitFeedback("${product.name} ditambahkan ke grup ${group.code}.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menambahkan ke grup.") }
        }
    }

    fun checkoutGroupOrder(group: GroupOrder, onPaymentCreated: (String) -> Unit) {
        val stall = activeStalls.value.firstOrNull { it.id == group.stallId }
        val slot = activePickupSlots.value.firstOrNull { it.id == group.pickupSlotId }
        val productsMap = activeProducts.value.associateBy { it.id }
        if (stall == null || slot == null) {
            emitFeedback("Stan atau slot pengambilan grup tidak aktif.")
            return
        }
        val groupedItems = group.items.groupBy { it.productId }.mapNotNull { (pid, list) ->
            val p = productsMap[pid] ?: return@mapNotNull null
            val qty = list.sumOf { it.quantity.toInt() }
            val combinedNotes = list.map { "${it.userName}: ${it.note.ifBlank { "-" }}" }.joinToString("; ")
            CartItem(p, stall, qty, combinedNotes)
        }
        if (groupedItems.isEmpty()) {
            emitFeedback("Belum ada menu yang dipilih dalam grup ini.")
            return
        }
        val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.placePreOrders(
                customerName = profileName,
                cartByStall = mapOf(stall.id to groupedItems),
                selectedSlotsByStall = mapOf(stall.id to slot),
                appliedVoucher = null,
                groupOrderCode = group.code
            )
            _isSubmitting.value = false
            res.onSuccess { paymentGroupId ->
                repository.updateGroupOrderStatus(group.id, "COMPLETED")
                onPaymentCreated(paymentGroupId)
                initiateMidtransSnap(paymentGroupId)
            }.onFailure { emitFeedback(it.localizedMessage ?: "Gagal checkout grup.") }
        }
    }

    fun closeGroupOrder(groupId: String) {
        viewModelScope.launch {
            repository.updateGroupOrderStatus(groupId, "LOCKED")
                .onSuccess { emitFeedback("Group Order ditutup untuk anggota baru.") }
        }
    }

    // Seller actions
    fun saveStall(
        stallId: String?,
        name: String,
        description: String,
        campusLocation: String,
        openHours: String,
        bannerKey: String,
        imageUrl: String?,
        active: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.saveStall(stallId, name, description, campusLocation, openHours, bannerKey, imageUrl, active)
                .onSuccess { emitFeedback("Informasi stan tersimpan.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menyimpan stan.") }
            _isSubmitting.value = false
        }
    }

    fun saveProduct(
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
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.saveProduct(
                productId, stallId, categoryId, categoryName, name, description,
                price, discountPrice, flashDealActive, flashDealLabel,
                dailyQuota, remainingStock, active, imageUrl, imageKey
            ).onSuccess { emitFeedback("Produk $name tersimpan.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menyimpan produk.") }
            _isSubmitting.value = false
        }
    }

    fun toggleProductActive(productId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleProductActive(productId, active)
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal ubah status produk.") }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess { emitFeedback("Produk dihapus.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menghapus produk.") }
        }
    }

    fun savePickupSlot(
        slotId: String?,
        stallId: String,
        date: String,
        startTime: String,
        endTime: String,
        capacity: Long,
        active: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.savePickupSlot(slotId, stallId, date, startTime, endTime, capacity, active)
                .onSuccess { emitFeedback("Slot pengambilan tersimpan.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menyimpan slot.") }
            _isSubmitting.value = false
        }
    }

    fun deletePickupSlot(slotId: String) {
        viewModelScope.launch {
            repository.deletePickupSlot(slotId)
                .onSuccess { emitFeedback("Slot dihapus.") }
        }
    }

    fun advanceSellerOrder(order: Order, nextStatus: OrderStatus) {
        viewModelScope.launch {
            repository.sellerAdvanceOrderStatus(order, nextStatus)
                .onSuccess { emitFeedback("Pesanan ${order.orderCode} -> ${nextStatus.label}") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal memperbarui status pesanan.") }
        }
    }

    fun verifyQrPickupCode(rawTokenOrCode: String, onVerified: (Order) -> Unit) {
        val needle = rawTokenOrCode.trim()
        val match = sellerOrders.value.firstOrNull {
            it.qrToken.equals(needle, ignoreCase = true) ||
                it.orderCode.equals(needle, ignoreCase = true)
        }
        if (match == null) {
            emitFeedback("Kode QR '$needle' tidak ditemukan pada pesanan stan Anda.")
            return
        }
        if (match.status != OrderStatus.READY_FOR_PICKUP.value) {
            emitFeedback("Pesanan ${match.orderCode} berstatus '${OrderStatus.fromRaw(match.status).label}'. Pastikan status 'Siap Diambil'.")
            return
        }
        viewModelScope.launch {
            repository.sellerAdvanceOrderStatus(match, OrderStatus.COMPLETED)
                .onSuccess {
                    emitFeedback("QR Terverifikasi! Pesanan ${match.orderCode} selesai diambil.")
                    onVerified(match)
                }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menyelesaikan pengambilan.") }
        }
    }

    // Admin actions
    fun adminUpdateUser(userId: String, role: String, active: Boolean) {
        viewModelScope.launch {
            repository.adminUpdateUser(userId, role, active)
                .onSuccess { emitFeedback("Akun pengguna diperbarui.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal memperbarui pengguna.") }
        }
    }

    fun adminVerifySeller(sellerId: String, stallId: String?, verified: Boolean, active: Boolean) {
        viewModelScope.launch {
            repository.adminVerifySellerAndStall(sellerId, stallId, verified, active)
                .onSuccess { emitFeedback(if (verified) "UMKM diverifikasi." else "Status UMKM diperbarui.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal memperbarui verifikasi UMKM.") }
        }
    }

    fun saveCategory(categoryId: String?, name: String, iconName: String, sortOrder: Long, active: Boolean) {
        viewModelScope.launch {
            repository.saveCategory(categoryId, name, iconName, sortOrder, active)
                .onSuccess { emitFeedback("Kategori $name tersimpan.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal menyimpan kategori.") }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess { emitFeedback("Kategori dihapus.") }
        }
    }

    fun seedDefaultCategoriesAndVouchers() {
        viewModelScope.launch {
            repository.seedDefaultCategoriesAndVouchers()
                .onSuccess { emitFeedback("Kategori default & katalog voucher berhasil diinisialisasi.") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Gagal inisialisasi kategori.") }
        }
    }

    fun checkMidtransBackendHealth(onResult: (String) -> Unit) {
        viewModelScope.launch {
            midtransService.checkHealth()
                .onSuccess { h ->
                    val status = if (h.midtransConfigured) {
                        "Terhubung (Mode ${h.mode ?: "sandbox"}, Server Key aktif)"
                    } else {
                        "Backend aktif, namun MIDTRANS_SERVER_KEY Sandbox belum diisi di .env server."
                    }
                    onResult(status)
                }
                .onFailure { err ->
                    onResult(err.localizedMessage ?: "Backend belum terhubung.")
                }
        }
    }

    private fun emitFeedback(msg: String) {
        _feedback.tryEmit(msg)
    }

    companion object {
        private const val TAG = "LaparVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
