package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppNotification
import com.example.model.CartItem
import com.example.model.FoodCategory
import com.example.model.GroupOrder
import com.example.model.GroupOrderItem
import com.example.model.LoyaltyPoints
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.model.PaymentRecord
import com.example.model.PaymentStatus
import com.example.model.PickupSlot
import com.example.model.Product
import com.example.model.SellerProfile
import com.example.model.Stall
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.Voucher
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LaparRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, uid: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- USER & SELLER PROFILE ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        require(uid.isNotBlank()) { "User ID kosong" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "users", uid)
        )
    }

    fun observeAllSellers(): Flow<List<SellerProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("sellers")
                .snapshots()
                .map { it.toObjects(SellerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "sellers", uid)
        )
    }

    fun observeSellerProfile(sellerId: String): Flow<SellerProfile?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("sellers").document(sellerId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(SellerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, "sellers/$sellerId", uid)
        )
    }

    suspend fun registerProfile(
        displayName: String,
        role: UserRole,
        nimOrUnit: String,
        phone: String,
        businessName: String = "",
        campusArea: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        val email = auth.currentUser?.email ?: ""
        val photoUrl = auth.currentUser?.photoUrl?.toString()
        val isBootstrappedAdmin = email.equals("raflisyawal2005@gmail.com", ignoreCase = true) &&
            (auth.currentUser?.isEmailVerified == true)
        val assignedRole = if (isBootstrappedAdmin && role == UserRole.ADMIN) {
            UserRole.ADMIN.value
        } else if (role == UserRole.SELLER) {
            UserRole.SELLER.value
        } else {
            UserRole.CUSTOMER.value
        }

        return try {
            val batch = db.batch()
            val userRef = db.collection("users").document(uid)
            val userMap = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName.trim().take(80),
                "email" to email,
                "role" to assignedRole,
                "nimOrUnit" to nimOrUnit.trim().take(80),
                "phone" to phone.trim().take(30),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUrl.isNullOrBlank()) {
                userMap["photoUrl"] = photoUrl.take(500)
            }
            batch.set(userRef, userMap)

            val lpRef = db.collection("loyalty_points").document(uid)
            batch.set(
                lpRef,
                mapOf(
                    "userId" to uid,
                    "points" to 0L,
                    "totalEarned" to 0L,
                    "totalRedeemed" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (assignedRole == UserRole.SELLER.value) {
                val safeBiz = businessName.trim().ifBlank { "Stan $displayName" }.take(100)
                val safeArea = campusArea.trim().ifBlank { "Kantin Utama Kampus" }.take(100)
                val sellerRef = db.collection("sellers").document(uid)
                batch.set(
                    sellerRef,
                    mapOf(
                        "sellerId" to uid,
                        "ownerName" to displayName.trim().take(80),
                        "businessName" to safeBiz,
                        "phone" to phone.trim().take(30),
                        "campusArea" to safeArea,
                        "verified" to false,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val stallRef = db.collection("stalls").document("stall_$uid")
                batch.set(
                    stallRef,
                    mapOf(
                        "sellerId" to uid,
                        "name" to safeBiz,
                        "description" to "Kantin pre-order kampus — pesan cepat tanpa antre.",
                        "campusLocation" to safeArea,
                        "openHours" to "08:00 - 16:00",
                        "bannerKey" to "img_hero_campus",
                        "rating" to 4.8,
                        "totalOrders" to 0L,
                        "verified" to false,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateOwnProfile(displayName: String, nimOrUnit: String, phone: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to displayName.trim().take(80),
                    "nimOrUnit" to nimOrUnit.trim().take(80),
                    "phone" to phone.trim().take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUser(userId: String, role: String, active: Boolean): Result<Unit> {
        return try {
            requireUserId()
            db.collection("users").document(userId).update(
                mapOf(
                    "role" to role,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun adminVerifySellerAndStall(sellerId: String, stallId: String?, verified: Boolean, active: Boolean): Result<Unit> {
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("sellers").document(sellerId),
                mapOf(
                    "verified" to verified,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (!stallId.isNullOrBlank()) {
                batch.update(
                    db.collection("stalls").document(stallId),
                    mapOf(
                        "verified" to verified,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "sellers/$sellerId")
            Result.failure(e)
        }
    }

    // --- STALLS ---

    fun observeActiveStalls(): Flow<List<Stall>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("stalls")
                .whereEqualTo("active", true)
                .snapshots()
                .map { it.toObjects(Stall::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "stalls", uid)
        )
    }

    fun observeSellerStalls(sellerId: String): Flow<List<Stall>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("stalls")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { it.toObjects(Stall::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "stalls", uid)
        )
    }

    fun observeAllStalls(): Flow<List<Stall>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("stalls")
                .snapshots()
                .map { it.toObjects(Stall::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "stalls", uid)
        )
    }

    suspend fun saveStall(
        stallId: String?,
        name: String,
        description: String,
        campusLocation: String,
        openHours: String,
        bannerKey: String,
        imageUrl: String?,
        active: Boolean
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        val targetId = stallId?.takeIf { it.isNotBlank() } ?: "stall_${uid}_${UUID.randomUUID().toString().take(6)}"
        return try {
            val docRef = db.collection("stalls").document(targetId)
            if (stallId.isNullOrBlank()) {
                val payload = mapOf(
                    "sellerId" to uid,
                    "name" to name.trim().take(100),
                    "description" to description.trim().take(500),
                    "campusLocation" to campusLocation.trim().take(120),
                    "openHours" to openHours.trim().take(60),
                    "bannerKey" to bannerKey.ifBlank { "img_hero_campus" },
                    "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(500),
                    "rating" to 4.8,
                    "totalOrders" to 0L,
                    "verified" to false,
                    "active" to active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "name" to name.trim().take(100),
                    "description" to description.trim().take(500),
                    "campusLocation" to campusLocation.trim().take(120),
                    "openHours" to openHours.trim().take(60),
                    "bannerKey" to bannerKey.ifBlank { "img_hero_campus" },
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!imageUrl.isNullOrBlank()) {
                    updates["imageUrl"] = imageUrl.take(500)
                }
                docRef.update(updates).await()
            }
            Result.success(targetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "stalls/$targetId")
            Result.failure(e)
        }
    }

    // --- CATEGORIES ---

    fun observeActiveCategories(): Flow<List<FoodCategory>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("categories")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FoodCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .logFirestoreErrors(OperationType.LIST, "categories", uid)
        )
    }

    fun observeAllCategories(): Flow<List<FoodCategory>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("categories")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FoodCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .logFirestoreErrors(OperationType.LIST, "categories", uid)
        )
    }

    suspend fun saveCategory(
        categoryId: String?,
        name: String,
        iconName: String,
        sortOrder: Long,
        active: Boolean
    ): Result<Unit> {
        val cleanSlug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "lainnya" }
        val targetId = categoryId?.takeIf { it.isNotBlank() } ?: "cat_$cleanSlug"
        return try {
            requireUserId()
            val ref = db.collection("categories").document(targetId)
            if (categoryId.isNullOrBlank()) {
                ref.set(
                    mapOf(
                        "name" to name.trim().take(50),
                        "slug" to cleanSlug.take(50),
                        "iconName" to iconName.ifBlank { "restaurant" },
                        "sortOrder" to sortOrder,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to name.trim().take(50),
                        "slug" to cleanSlug.take(50),
                        "iconName" to iconName.ifBlank { "restaurant" },
                        "sortOrder" to sortOrder,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "categories/$targetId")
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "categories/$categoryId")
            Result.failure(e)
        }
    }

    suspend fun seedDefaultCategoriesAndVouchers(): Result<Unit> {
        return try {
            requireUserId()
            val batch = db.batch()
            val defaults = listOf(
                Triple("cat_nasi", "Nasi", "rice_bowl" to 1L),
                Triple("cat_mi", "Mi", "ramen_dining" to 2L),
                Triple("cat_minuman", "Minuman", "local_cafe" to 3L),
                Triple("cat_camilan", "Camilan", "bakery_dining" to 4L),
                Triple("cat_lainnya", "Makanan Lainnya", "restaurant" to 5L)
            )
            defaults.forEach { (id, name, iconOrder) ->
                val ref = db.collection("categories").document(id)
                batch.set(
                    ref,
                    mapOf(
                        "name" to name,
                        "slug" to id.removePrefix("cat_"),
                        "iconName" to iconOrder.first,
                        "sortOrder" to iconOrder.second,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val voucherDefaults = listOf(
                mapOf(
                    "code" to "HEMAT5K",
                    "title" to "Diskon Makan Siang Rp 5.000",
                    "description" to "Tukarkan 50 Lapar Points untuk potongan Rp 5.000 (min. Rp 20.000).",
                    "discountAmount" to 5000L,
                    "minOrderAmount" to 20000L,
                    "pointsCost" to 50L,
                    "ownerUserId" to "",
                    "redeemed" to false,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "code" to "NGOPI3K",
                    "title" to "Potongan Minuman Rp 3.000",
                    "description" to "Tukarkan 30 Lapar Points untuk potongan Rp 3.000 (min. Rp 12.000).",
                    "discountAmount" to 3000L,
                    "minOrderAmount" to 12000L,
                    "pointsCost" to 30L,
                    "ownerUserId" to "",
                    "redeemed" to false,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            voucherDefaults.forEachIndexed { idx, v ->
                batch.set(db.collection("vouchers").document("tpl_voucher_${idx + 1}"), v)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "categories")
            Result.failure(e)
        }
    }

    // --- PRODUCTS ---

    fun observeActiveProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("products")
                .whereEqualTo("active", true)
                .snapshots()
                .map { it.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "products", uid)
        )
    }

    fun observeSellerProducts(sellerId: String): Flow<List<Product>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { it.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "products", uid)
        )
    }

    suspend fun saveProduct(
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
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        val targetId = productId?.takeIf { it.isNotBlank() } ?: "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val safeQuota = dailyQuota.coerceIn(0L, 10000L)
        val safeStock = remainingStock.coerceIn(0L, safeQuota)
        val safeDiscount = if (flashDealActive && discountPrice != null && discountPrice in 1..price) discountPrice else null

        return try {
            val docRef = db.collection("products").document(targetId)
            val common = mutableMapOf<String, Any>(
                "categoryId" to categoryId.ifBlank { "cat_nasi" },
                "categoryName" to categoryName.ifBlank { "Nasi" }.take(60),
                "name" to name.trim().take(100),
                "description" to description.trim().take(600),
                "price" to price.coerceAtLeast(100L),
                "flashDealActive" to flashDealActive,
                "dailyQuota" to safeQuota,
                "remainingStock" to safeStock,
                "active" to active,
                "imageKey" to imageKey.ifBlank { "img_food_nasi" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeDiscount != null) common["discountPrice"] = safeDiscount
            if (flashDealActive && !flashDealLabel.isNullOrBlank()) {
                common["flashDealLabel"] = flashDealLabel.trim().take(40)
            }
            if (!imageUrl.isNullOrBlank()) {
                common["imageUrl"] = imageUrl.take(500)
            }

            if (productId.isNullOrBlank()) {
                common["stallId"] = stallId
                common["sellerId"] = uid
                common["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(common).await()
            } else {
                if (safeDiscount == null) {
                    common["discountPrice"] = FieldValue.delete()
                }
                if (!flashDealActive || flashDealLabel.isNullOrBlank()) {
                    common["flashDealLabel"] = FieldValue.delete()
                }
                docRef.update(common).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products/$targetId")
            Result.failure(e)
        }
    }

    suspend fun toggleProductActive(productId: String, active: Boolean): Result<Unit> {
        return try {
            requireUserId()
            db.collection("products").document(productId).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "products/$productId")
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "products/$productId")
            Result.failure(e)
        }
    }

    // --- PICKUP SLOTS ---

    fun observeActivePickupSlots(): Flow<List<PickupSlot>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("pickup_slots")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PickupSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy({ it.date }, { it.startTime }))
                }
                .logFirestoreErrors(OperationType.LIST, "pickup_slots", uid)
        )
    }

    fun observeSellerPickupSlots(sellerId: String): Flow<List<PickupSlot>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("pickup_slots")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PickupSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy({ it.date }, { it.startTime }))
                }
                .logFirestoreErrors(OperationType.LIST, "pickup_slots", uid)
        )
    }

    suspend fun savePickupSlot(
        slotId: String?,
        stallId: String,
        date: String,
        startTime: String,
        endTime: String,
        capacity: Long,
        active: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        val targetId = slotId?.takeIf { it.isNotBlank() } ?: "slot_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val label = "${startTime.trim()} - ${endTime.trim()}"
        return try {
            val docRef = db.collection("pickup_slots").document(targetId)
            if (slotId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "stallId" to stallId,
                        "sellerId" to uid,
                        "date" to date.trim(),
                        "startTime" to startTime.trim(),
                        "endTime" to endTime.trim(),
                        "label" to label,
                        "capacity" to capacity.coerceIn(1L, 500L),
                        "bookedCount" to 0L,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "date" to date.trim(),
                        "startTime" to startTime.trim(),
                        "endTime" to endTime.trim(),
                        "label" to label,
                        "capacity" to capacity.coerceIn(1L, 500L),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "pickup_slots/$targetId")
            Result.failure(e)
        }
    }

    suspend fun deletePickupSlot(slotId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("pickup_slots").document(slotId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "pickup_slots/$slotId")
            Result.failure(e)
        }
    }

    // --- ORDERS & CHECKOUT TRANSACTION ---

    fun observeCustomerOrders(customerId: String): Flow<List<Order>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("orders")
                .whereEqualTo("customerId", customerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "orders", uid)
        )
    }

    fun observeSellerOrders(sellerId: String): Flow<List<Order>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy({ it.pickupDate }, { it.pickupSlotLabel }))
                }
                .logFirestoreErrors(OperationType.LIST, "orders", uid)
        )
    }

    fun observeAllOrders(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "orders", uid)
        )
    }

    fun observeCustomerPayments(customerId: String): Flow<List<PaymentRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("payments")
                .whereEqualTo("customerId", customerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "payments", uid)
        )
    }

    /**
     * Splits multi-stall cart items into separate orders per stall, reserves pickup slots atomically
     * via Firestore Transaction, creates a PaymentRecord for Midtrans Sandbox, and marks any applied voucher.
     */
    suspend fun placePreOrders(
        customerName: String,
        cartByStall: Map<String, List<CartItem>>,
        selectedSlotsByStall: Map<String, PickupSlot>,
        appliedVoucher: Voucher?,
        groupOrderCode: String? = null
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        if (cartByStall.isEmpty()) return Result.failure(IllegalArgumentException("Keranjang kosong."))

        val paymentGroupId = "PAY-${UUID.randomUUID().toString().replace("-", "").take(12).uppercase()}"
        return try {
            db.runTransaction { tx ->
                // 1. Validate & read all pickup slots and products first (Firestore Transaction reads before writes)
                val slotSnapshots = mutableMapOf<String, DocumentSnapshot>()
                for ((stallId, _) in cartByStall) {
                    val slot = selectedSlotsByStall[stallId]
                        ?: throw IllegalStateException("Pilih jadwal pengambilan untuk semua stan.")
                    val slotRef = db.collection("pickup_slots").document(slot.id)
                    val slotSnap = tx.get(slotRef)
                    if (!slotSnap.exists()) {
                        throw IllegalStateException("Slot pengambilan tidak ditemukan.")
                    }
                    val capacity = slotSnap.getLong("capacity") ?: 0L
                    val booked = slotSnap.getLong("bookedCount") ?: 0L
                    val active = slotSnap.getBoolean("active") ?: false
                    if (!active || booked >= capacity) {
                        throw IllegalStateException("Slot ${slot.label} sudah penuh. Pilih jam lain.")
                    }
                    slotSnapshots[stallId] = slotSnap
                }

                // 2. Verify product stock is sufficient
                for ((_, items) in cartByStall) {
                    for (item in items) {
                        val pSnap = tx.get(db.collection("products").document(item.product.id))
                        val stock = pSnap.getLong("remainingStock") ?: 0L
                        val active = pSnap.getBoolean("active") ?: false
                        if (!active || stock < item.quantity) {
                            throw IllegalStateException("Stok ${item.product.name} tersisa $stock porsi.")
                        }
                    }
                }

                // 3. Compute discounts (applied to first eligible stall order)
                val grandTotal = cartByStall.values.flatten().sumOf { it.lineTotal }
                val totalDiscount = if (appliedVoucher != null && grandTotal >= appliedVoucher.minOrderAmount) {
                    appliedVoucher.discountAmount.coerceAtMost(grandTotal)
                } else 0L

                val orderIds = mutableListOf<String>()
                var remainingDiscount = totalDiscount

                // 4. Write slot updates + split orders per stall
                for ((stallId, items) in cartByStall) {
                    val slotSnap = slotSnapshots[stallId]!!
                    val prevBooked = slotSnap.getLong("bookedCount") ?: 0L
                    tx.update(
                        slotSnap.reference,
                        mapOf(
                            "bookedCount" to prevBooked + 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )

                    val stall = items.first().stall
                    val stallSubtotal = items.sumOf { it.lineTotal }
                    val orderDiscount = remainingDiscount.coerceAtMost(stallSubtotal)
                    remainingDiscount -= orderDiscount
                    val finalAmount = (stallSubtotal - orderDiscount).coerceAtLeast(0L)

                    val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                    orderIds.add(orderId)
                    val orderCode = "LPR-${orderId.takeLast(6).uppercase()}"
                    val qrToken = "LAPAR-${orderId.takeLast(8).uppercase()}"

                    val orderItemsPayload = items.map { ci ->
                        mapOf(
                            "productId" to ci.product.id,
                            "productName" to ci.product.name,
                            "price" to ci.product.effectivePrice,
                            "quantity" to ci.quantity.toLong(),
                            "note" to ci.note.trim().take(120)
                        )
                    }

                    val slotDate = slotSnap.getString("date") ?: ""
                    val slotLabel = slotSnap.getString("label") ?: ""

                    val orderPayload = mutableMapOf<String, Any>(
                        "orderCode" to orderCode,
                        "customerId" to uid,
                        "customerName" to customerName.ifBlank { "Mahasiswa" }.take(80),
                        "sellerId" to stall.sellerId,
                        "stallId" to stallId,
                        "stallName" to stall.name.take(100),
                        "paymentGroupId" to paymentGroupId,
                        "items" to orderItemsPayload,
                        "totalAmount" to stallSubtotal,
                        "discountAmount" to orderDiscount,
                        "finalAmount" to finalAmount,
                        "pickupDate" to slotDate,
                        "pickupSlotId" to slotSnap.id,
                        "pickupSlotLabel" to slotLabel,
                        "status" to OrderStatus.PENDING_PAYMENT.value,
                        "paymentStatus" to PaymentStatus.PENDING.value,
                        "qrToken" to qrToken,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (orderDiscount > 0L && appliedVoucher != null) {
                        orderPayload["voucherCode"] = appliedVoucher.code
                    }
                    if (!groupOrderCode.isNullOrBlank()) {
                        orderPayload["groupOrderCode"] = groupOrderCode
                    }
                    tx.set(db.collection("orders").document(orderId), orderPayload)
                }

                // 5. Create PaymentRecord
                val netGross = (grandTotal - totalDiscount).coerceAtLeast(0L)
                tx.set(
                    db.collection("payments").document(paymentGroupId),
                    mapOf(
                        "paymentGroupId" to paymentGroupId,
                        "orderIds" to orderIds,
                        "customerId" to uid,
                        "grossAmount" to netGross,
                        "status" to PaymentStatus.PENDING.value,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 6. Mark voucher redeemed if used
                if (appliedVoucher != null && totalDiscount > 0L && appliedVoucher.ownerUserId == uid) {
                    tx.update(
                        db.collection("vouchers").document(appliedVoucher.id),
                        mapOf(
                            "redeemed" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 7. Create customer notification
                val notifRef = db.collection("notifications").document()
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Menunggu Pembayaran",
                        "message" to "Pesanan pre-order senilai Rp $netGross dibuat. Selesaikan pembayaran Midtrans sebelum batas waktu.",
                        "type" to "ORDER_CREATED",
                        "relatedOrderId" to orderIds.first(),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(paymentGroupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orders/$paymentGroupId")
            Result.failure(e)
        }
    }

    suspend fun cancelPendingOrder(order: Order): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        return try {
            db.collection("orders").document(order.id).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.value,
                    "paymentStatus" to PaymentStatus.CANCEL.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun sellerAdvanceOrderStatus(order: Order, nextStatus: OrderStatus): Result<Unit> {
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("orders").document(order.id),
                mapOf(
                    "status" to nextStatus.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val (notifTitle, notifMsg, notifType) = when (nextStatus) {
                OrderStatus.PREPARING -> Triple(
                    "Pesanan Sedang Diproses",
                    "${order.stallName} sedang menyiapkan pesanan ${order.orderCode}.",
                    "ORDER_PREPARING"
                )
                OrderStatus.READY_FOR_PICKUP -> Triple(
                    "Pesanan Siap Diambil!",
                    "Pesanan ${order.orderCode} sudah siap di ${order.stallName}. Tunjukkan QR Code Anda saat mengambil.",
                    "ORDER_READY"
                )
                OrderStatus.COMPLETED -> Triple(
                    "Pesanan Selesai",
                    "Terima kasih! Pesanan ${order.orderCode} telah diambil.",
                    "ORDER_COMPLETED"
                )
                else -> Triple("Pembaruan Pesanan", "Status pesanan ${order.orderCode} diperbarui.", "INFO")
            }
            val notifRef = db.collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "userId" to order.customerId,
                    "title" to notifTitle,
                    "message" to notifMsg,
                    "type" to notifType,
                    "relatedOrderId" to order.id,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    // --- NOTIFICATIONS ---

    fun observeNotifications(uid: String): Flow<List<AppNotification>> = flow {
        emitAll(
            db.collection("notifications")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "notifications", uid)
        )
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId).update(
                mapOf(
                    "read" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
            Result.failure(e)
        }
    }

    // --- LOYALTY POINTS & VOUCHERS ---

    fun observeLoyaltyPoints(uid: String): Flow<LoyaltyPoints?> = flow {
        emitAll(
            db.collection("loyalty_points").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(LoyaltyPoints::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, "loyalty_points/$uid", uid)
        )
    }

    fun observeCatalogVouchers(): Flow<List<Voucher>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("vouchers")
                .whereEqualTo("ownerUserId", "")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Voucher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.active }
                }
                .logFirestoreErrors(OperationType.LIST, "vouchers", uid)
        )
    }

    fun observeMyVouchers(uid: String): Flow<List<Voucher>> = flow {
        emitAll(
            db.collection("vouchers")
                .whereEqualTo("ownerUserId", uid)
                .snapshots()
                .map { it.toObjects(Voucher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "vouchers", uid)
        )
    }

    suspend fun redeemVoucherWithPoints(template: Voucher): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        return try {
            db.runTransaction { tx ->
                val lpRef = db.collection("loyalty_points").document(uid)
                val lpSnap = tx.get(lpRef)
                val currentPts = lpSnap.getLong("points") ?: 0L
                val totalRedeemed = lpSnap.getLong("totalRedeemed") ?: 0L
                if (currentPts < template.pointsCost) {
                    throw IllegalStateException("Poin Anda ($currentPts) belum cukup untuk voucher ini (${template.pointsCost} poin).")
                }
                tx.update(
                    lpRef,
                    mapOf(
                        "points" to currentPts - template.pointsCost,
                        "totalRedeemed" to totalRedeemed + template.pointsCost,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val newVoucherRef = db.collection("vouchers").document("v_${uid.take(6)}_${UUID.randomUUID().toString().take(8)}")
                tx.set(
                    newVoucherRef,
                    mapOf(
                        "code" to template.code,
                        "title" to template.title,
                        "description" to template.description,
                        "discountAmount" to template.discountAmount,
                        "minOrderAmount" to template.minOrderAmount,
                        "pointsCost" to template.pointsCost,
                        "ownerUserId" to uid,
                        "redeemed" to false,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "vouchers")
            Result.failure(e)
        }
    }

    // --- GROUP ORDERS ---

    fun observeOpenGroupOrders(): Flow<List<GroupOrder>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("group_orders")
                .whereEqualTo("status", "OPEN")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "group_orders", uid)
        )
    }

    fun observeMyGroupOrders(uid: String): Flow<List<GroupOrder>> = flow {
        emitAll(
            db.collection("group_orders")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "group_orders", uid)
        )
    }

    suspend fun createGroupOrder(
        hostName: String,
        stall: Stall,
        title: String,
        slot: PickupSlot,
        closesInMinutes: Int
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        val code = "GRP-${UUID.randomUUID().toString().take(5).uppercase()}"
        val docId = "grp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val closesAt = System.currentTimeMillis() + closesInMinutes * 60_000L
        return try {
            db.collection("group_orders").document(docId).set(
                mapOf(
                    "code" to code,
                    "hostUserId" to uid,
                    "hostName" to hostName.ifBlank { "Host" }.take(80),
                    "stallId" to stall.id,
                    "stallName" to stall.name.take(100),
                    "title" to title.trim().take(100),
                    "pickupDate" to slot.date,
                    "pickupSlotId" to slot.id,
                    "pickupSlotLabel" to slot.label,
                    "closesAtMillis" to closesAt,
                    "status" to "OPEN",
                    "memberUids" to listOf(uid),
                    "items" to emptyList<Map<String, Any>>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "group_orders/$docId")
            Result.failure(e)
        }
    }

    suspend fun addItemToGroupOrder(
        groupOrder: GroupOrder,
        userName: String,
        product: Product,
        quantity: Int,
        note: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk."))
        return try {
            val ref = db.collection("group_orders").document(groupOrder.id)
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                val current = snap.toObject(GroupOrder::class.java)
                    ?: throw IllegalStateException("Pesanan kelompok tidak ditemukan.")
                if (current.status != "OPEN") {
                    throw IllegalStateException("Pesanan kelompok sudah ditutup.")
                }
                val newMembers = (current.memberUids + uid).distinct().take(30)
                val newItems = (current.items + GroupOrderItem(
                    userId = uid,
                    userName = userName.ifBlank { "Anggota" }.take(80),
                    productId = product.id,
                    productName = product.name,
                    price = product.effectivePrice,
                    quantity = quantity.toLong(),
                    note = note.trim().take(100)
                )).map {
                    mapOf(
                        "userId" to it.userId,
                        "userName" to it.userName,
                        "productId" to it.productId,
                        "productName" to it.productName,
                        "price" to it.price,
                        "quantity" to it.quantity,
                        "note" to it.note
                    )
                }
                tx.update(
                    ref,
                    mapOf(
                        "memberUids" to newMembers,
                        "items" to newItems,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "group_orders/${groupOrder.id}")
            Result.failure(e)
        }
    }

    suspend fun updateGroupOrderStatus(groupId: String, status: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("group_orders").document(groupId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "group_orders/$groupId")
            Result.failure(e)
        }
    }
}
