package com.example.data

import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CreateSnapTokenRequest(
    val paymentGroupId: String
)

@JsonClass(generateAdapter = true)
data class CreateSnapTokenResponse(
    val snapToken: String? = null,
    val redirectUrl: String? = null,
    val error: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentStatusResponse(
    val paymentGroupId: String? = null,
    val status: String? = null,
    val paymentType: String? = null,
    val snapToken: String? = null,
    val redirectUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val ok: Boolean = false,
    val mode: String? = null,
    val midtransConfigured: Boolean = false
)

interface MidtransBackendApi {
    @GET("api/health")
    suspend fun health(): Response<HealthResponse>

    @POST("api/payments/create-snap-token")
    suspend fun createSnapToken(
        @Header("Authorization") bearerToken: String,
        @Body body: CreateSnapTokenRequest
    ): Response<CreateSnapTokenResponse>

    @GET("api/payments/status/{paymentGroupId}")
    suspend fun getPaymentStatus(
        @Header("Authorization") bearerToken: String,
        @Path("paymentGroupId") paymentGroupId: String
    ): Response<PaymentStatusResponse>
}

sealed interface MidtransSnapResult {
    data class Success(val snapToken: String, val redirectUrl: String) : MidtransSnapResult
    data class NotConfigured(val reason: String) : MidtransSnapResult
    data class Error(val message: String) : MidtransSnapResult
}

class MidtransService {
    private val backendUrl: String = BuildConfig.MIDTRANS_BACKEND_URL.trim()

    val isBackendUrlConfigured: Boolean
        get() = backendUrl.startsWith("http://") || backendUrl.startsWith("https://")

    private fun buildApi(): MidtransBackendApi? {
        if (!isBackendUrlConfigured) return null
        val normalizedBase = if (backendUrl.endsWith("/")) backendUrl else "$backendUrl/"
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBase)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MidtransBackendApi::class.java)
    }

    suspend fun requestSnapToken(paymentGroupId: String): MidtransSnapResult {
        val api = buildApi()
            ?: return MidtransSnapResult.NotConfigured(
                "MIDTRANS_BACKEND_URL belum diatur pada Secrets panel AI Studio."
            )

        return try {
            val token = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                ?: return MidtransSnapResult.Error("Sesi autentikasi Firebase tidak ditemukan.")
            val response = api.createSnapToken(
                bearerToken = "Bearer $token",
                body = CreateSnapTokenRequest(paymentGroupId = paymentGroupId)
            )
            if (response.isSuccessful) {
                val body = response.body()
                val snapToken = body?.snapToken
                val redirectUrl = body?.redirectUrl
                if (!snapToken.isNullOrBlank() && !redirectUrl.isNullOrBlank()) {
                    MidtransSnapResult.Success(snapToken, redirectUrl)
                } else {
                    MidtransSnapResult.Error(body?.message ?: "Snap Token tidak tersedia dari server.")
                }
            } else if (response.code() == 503) {
                MidtransSnapResult.NotConfigured(
                    "Backend terhubung, tetapi MIDTRANS_SERVER_KEY Sandbox belum diisi di file .env server."
                )
            } else {
                MidtransSnapResult.Error("Gagal membuat transaksi Midtrans (HTTP ${response.code()}).")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MidtransSnapResult.Error(
                "Tidak dapat menjangkau server Midtrans backend (${e.localizedMessage ?: "koneksi gagal"})."
            )
        }
    }

    suspend fun checkHealth(): Result<HealthResponse> {
        val api = buildApi()
            ?: return Result.failure(IllegalStateException("MIDTRANS_BACKEND_URL belum dikonfigurasi."))
        return try {
            val res = api.health()
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IllegalStateException("Server mengembalikan status HTTP ${res.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
