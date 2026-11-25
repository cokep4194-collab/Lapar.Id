package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val value: String, val label: String) {
    CUSTOMER("customer", "Mahasiswa (Customer)"),
    SELLER("seller", "UMKM Kantin (Seller)"),
    ADMIN("admin", "Administrator");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class OrderStatus(val value: String, val label: String) {
    PENDING_PAYMENT("PENDING_PAYMENT", "Menunggu Pembayaran"),
    PAID("PAID", "Pesanan Diterima"),
    PREPARING("PREPARING", "Sedang Diproses"),
    READY_FOR_PICKUP("READY_FOR_PICKUP", "Siap Diambil"),
    COMPLETED("COMPLETED", "Selesai"),
    EXPIRED("EXPIRED", "Kedaluwarsa"),
    CANCELLED("CANCELLED", "Dibatalkan");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: PENDING_PAYMENT
    }
}

enum class PaymentStatus(val value: String, val label: String) {
    PENDING("PENDING", "Menunggu Pembayaran"),
    SETTLEMENT("SETTLEMENT", "Lunas (Terverifikasi)"),
    EXPIRE("EXPIRE", "Kedaluwarsa"),
    CANCEL("CANCEL", "Dibatalkan");

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "customer",
    val nimOrUnit: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SellerProfile(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val ownerName: String = "",
    val businessName: String = "",
    val phone: String = "",
    val campusArea: String = "",
    val verified: Boolean = false,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Stall(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val name: String = "",
    val description: String = "",
    val campusLocation: String = "",
    val openHours: String = "08:00 - 16:00",
    val bannerKey: String = "img_hero_campus",
    val imageUrl: String? = null,
    val rating: Double = 4.8,
    val totalOrders: Long = 0L,
    val verified: Boolean = false,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FoodCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val slug: String = "",
    val iconName: String = "restaurant",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val stallId: String = "",
    val sellerId: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val name: String = "",
    val description: String = "",
    val price: Long = 0L,
    val discountPrice: Long? = null,
    val flashDealActive: Boolean = false,
    val flashDealLabel: String? = null,
    val dailyQuota: Long = 0L,
    val remainingStock: Long = 0L,
    val active: Boolean = true,
    val imageUrl: String? = null,
    val imageKey: String = "img_food_nasi",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectivePrice: Long
        get() = if (flashDealActive && discountPrice != null && discountPrice in 1..price) {
            discountPrice
        } else {
            price
        }
}

@Keep
@IgnoreExtraProperties
data class PickupSlot(
    @DocumentId val id: String = "",
    val stallId: String = "",
    val sellerId: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val label: String = "",
    val capacity: Long = 10L,
    val bookedCount: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val remainingCapacity: Long
        get() = (capacity - bookedCount).coerceAtLeast(0L)
}

@Keep
@IgnoreExtraProperties
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val price: Long = 0L,
    val quantity: Long = 1L,
    val note: String = ""
)

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderCode: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val sellerId: String = "",
    val stallId: String = "",
    val stallName: String = "",
    val paymentGroupId: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Long = 0L,
    val discountAmount: Long = 0L,
    val finalAmount: Long = 0L,
    val voucherCode: String? = null,
    val pickupDate: String = "",
    val pickupSlotId: String = "",
    val pickupSlotLabel: String = "",
    val status: String = OrderStatus.PENDING_PAYMENT.value,
    val paymentStatus: String = PaymentStatus.PENDING.value,
    val qrToken: String = "",
    val groupOrderCode: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val paymentGroupId: String = "",
    val orderIds: List<String> = emptyList(),
    val customerId: String = "",
    val grossAmount: Long = 0L,
    val snapToken: String? = null,
    val redirectUrl: String? = null,
    val paymentType: String? = null,
    val midtransTransactionId: String? = null,
    val status: String = PaymentStatus.PENDING.value,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO",
    val relatedOrderId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Voucher(
    @DocumentId val id: String = "",
    val code: String = "",
    val title: String = "",
    val description: String = "",
    val discountAmount: Long = 0L,
    val minOrderAmount: Long = 0L,
    val pointsCost: Long = 0L,
    val ownerUserId: String = "",
    val redeemed: Boolean = false,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LoyaltyPoints(
    @DocumentId val id: String = "",
    val userId: String = "",
    val points: Long = 0L,
    val totalEarned: Long = 0L,
    val totalRedeemed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupOrderItem(
    val userId: String = "",
    val userName: String = "",
    val productId: String = "",
    val productName: String = "",
    val price: Long = 0L,
    val quantity: Long = 1L,
    val note: String = ""
)

@Keep
@IgnoreExtraProperties
data class GroupOrder(
    @DocumentId val id: String = "",
    val code: String = "",
    val hostUserId: String = "",
    val hostName: String = "",
    val stallId: String = "",
    val stallName: String = "",
    val title: String = "",
    val pickupDate: String = "",
    val pickupSlotId: String = "",
    val pickupSlotLabel: String = "",
    val closesAtMillis: Long = 0L,
    val status: String = "OPEN",
    val memberUids: List<String> = emptyList(),
    val items: List<GroupOrderItem> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val product: Product,
    val stall: Stall,
    val quantity: Int,
    val note: String
) {
    val lineTotal: Long get() = product.effectivePrice * quantity
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
