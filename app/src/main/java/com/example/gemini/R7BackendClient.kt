package com.example.gemini

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class R7BackendClient(private val context: Context) {
    val configManager = R7BackendConfigManager(context)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Production timeouts: connect 15s, read 30s, write 15s
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) // Handled explicitly below
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun solve(
        question: String,
        subject: String = "math",
        mode: String = "solve",
        language: String = "ar",
        history: List<ChatMessage> = emptyList()
    ): Result<R7SolveResponse> = withContext(Dispatchers.IO) {
        // 1. Offline Network Check
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(
                R7BackendException(
                    message = if (language == "ar")
                        "المساعد الذكي يحتاج إلى اتصال بالإنترنت، لكن الآلة الحاسبة الأساسية ما زالت تعمل بكامل طاقتها."
                    else
                        "The AI assistant requires an active internet connection, but the main calculator continues to work fully offline.",
                    isOffline = true,
                    errorCode = "OFFLINE"
                )
            )
        }

        val baseUrl = configManager.getEffectiveBackendUrl()
        val appKey = configManager.getEffectiveAppKey()
        val solveUrl = "$baseUrl/api/v1/ai/solve"

        // Build history turns
        val historyTurns = history.filter { !it.isError && it.text.isNotBlank() }.takeLast(6).map {
            R7HistoryTurn(
                role = if (it.role == "user") "user" else "model",
                text = it.text.trim()
            )
        }

        val requestPayload = R7SolveRequest(
            question = question.trim(),
            subject = subject,
            mode = mode,
            language = language,
            history = if (historyTurns.isNotEmpty()) historyTurns else null
        )

        val adapter = moshi.adapter(R7SolveRequest::class.java)
        val jsonBody = adapter.toJson(requestPayload)

        val maxAttempts = 2
        var lastErrorMsg = "فشل الاتصال بالخادم الآمن"
        var lastHttpCode = 0
        var lastErrorCode: String? = null

        for (attempt in 1..maxAttempts) {
            try {
                val httpRequest = Request.Builder()
                    .url(solveUrl)
                    .addHeader("X-R7-App-Key", appKey)
                    .addHeader("Accept", "application/json")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(httpRequest).execute()
                val code = response.code
                lastHttpCode = code
                val bodyStr = response.body?.string() ?: ""

                val respAdapter = moshi.adapter(R7SolveResponse::class.java)

                if (response.isSuccessful) {
                    val resultObj = respAdapter.fromJson(bodyStr)
                    if (resultObj != null && resultObj.success && !resultObj.answer.isNullOrBlank()) {
                        return@withContext Result.success(resultObj)
                    }
                } else {
                    val errorObj = try { respAdapter.fromJson(bodyStr) } catch (_: Exception) { null }
                    lastErrorCode = errorObj?.error_code
                    lastErrorMsg = errorObj?.message ?: "خطأ في استجابة الخادم ($code)"

                    Log.w("R7BackendClient", "Attempt $attempt returned HTTP $code: $lastErrorMsg ($lastErrorCode)")

                    // Permanent client errors (400, 401, 403, 404): DO NOT retry
                    if (code in 400..404) {
                        return@withContext Result.failure(
                            R7BackendException(
                                message = lastErrorMsg,
                                errorCode = lastErrorCode,
                                httpCode = code
                            )
                        )
                    }

                    // Transient errors (429, 500, 502, 503, 504)
                    if (attempt < maxAttempts) {
                        val backoffDelay = (attempt * 1500L) + Random.nextLong(200L, 500L)
                        delay(backoffDelay)
                        continue
                    }
                }
            } catch (e: Exception) {
                Log.w("R7BackendClient", "Exception on attempt $attempt calling $solveUrl", e)
                lastErrorMsg = when (e) {
                    is UnknownHostException -> "تعذر الوصول إلى عنوان الخادم. تأكد من اتصالك بالإنترنت."
                    is SocketTimeoutException -> "استغرق الخادم وقتًا أطول من المعتاد للاستجابة (انتهت المهلة)."
                    is IOException -> "حدث خطأ في شبكة الاتصال أثناء إرسال الطلب."
                    else -> e.message ?: "حدث خطأ غير متوقع في الاتصال."
                }

                if (attempt < maxAttempts && (e is SocketTimeoutException || e is IOException)) {
                    delay(1200L)
                    continue
                }
                break
            }
        }

        Result.failure(
            R7BackendException(
                message = lastErrorMsg,
                errorCode = lastErrorCode ?: "BACKEND_UNAVAILABLE",
                httpCode = lastHttpCode
            )
        )
    }

    suspend fun checkHealth(): Result<R7HealthResponse> = withContext(Dispatchers.IO) {
        val baseUrl = configManager.getEffectiveBackendUrl()
        val healthUrl = "$baseUrl/health"

        try {
            val request = Request.Builder()
                .url(healthUrl)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val adapter = moshi.adapter(R7HealthResponse::class.java)
                val health = adapter.fromJson(body)
                if (health != null && health.status == "ok") {
                    return@withContext Result.success(health)
                }
            }
            Result.failure(Exception("استجابة غير صالحة من فحص الخادم (${response.code})"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "تعذر الوصول إلى فحص صحة الخادم"))
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
