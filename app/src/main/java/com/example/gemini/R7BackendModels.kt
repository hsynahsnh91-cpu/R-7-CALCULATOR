package com.example.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class R7SolveRequest(
    @field:Json(name = "question") val question: String,
    @field:Json(name = "subject") val subject: String = "math",
    @field:Json(name = "mode") val mode: String = "solve",
    @field:Json(name = "language") val language: String = "ar",
    @field:Json(name = "history") val history: List<R7HistoryTurn>? = null
)

@JsonClass(generateAdapter = true)
data class R7HistoryTurn(
    @field:Json(name = "role") val role: String,
    @field:Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class R7SolveResponse(
    @field:Json(name = "success") val success: Boolean,
    @field:Json(name = "answer") val answer: String? = null,
    @field:Json(name = "subject") val subject: String? = null,
    @field:Json(name = "ai_version") val ai_version: String? = null,
    @field:Json(name = "request_id") val request_id: String? = null,
    @field:Json(name = "confidence") val confidence: String? = null,
    @field:Json(name = "verification_status") val verification_status: String? = null,
    @field:Json(name = "processing_time_ms") val processing_time_ms: Long? = null,
    @field:Json(name = "error_code") val error_code: String? = null,
    @field:Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class R7HealthResponse(
    @field:Json(name = "status") val status: String,
    @field:Json(name = "service") val service: String,
    @field:Json(name = "version") val version: String
)

interface ChatbotRoleWrapper {
    val id: String
    val titleAr: String
    val mode: String
    val systemInstruction: String
    val description: String
}

enum class ChatbotRole(
    override val id: String,
    override val titleAr: String,
    override val mode: String,
    override val systemInstruction: String,
    override val description: String
) : ChatbotRoleWrapper {
    GENERAL_FLASH(
        id = "flash",
        titleAr = "المساعد العام",
        mode = "solve",
        systemInstruction = "أنت مساعد R-7 الحسابي والهندسي. مهمتك الإجابة بوضوح وسرعة ودقة عن العمليات الحسابية، النِّسَب، التحويلات، والمعادلات اليومية والمالية والهندسية.",
        description = "للمهام الحسابية والاستفسارات العامة بدقة وسرعة"
    ),
    FAST_LITE(
        id = "lite",
        titleAr = "المجيب السريع",
        mode = "fast",
        systemInstruction = "أنت المجيب السريع لآلة R-7. قدّم إجابات مباشرة ومختصرة جدًا ودقيقة للقيم الحسابية، الثوابت، والصيغ السريعة.",
        description = "للإجابات الفورية فائقة السرعة"
    ),
    COMPLEX_PRO(
        id = "pro",
        titleAr = "خبير الهندسة والرياضيات",
        mode = "pro",
        systemInstruction = "أنت خبير الرياضيات المتقدمة والهندسة المساعد لآلة حاسبة R-7. مهمتك حل المسائل الهندسية، التفاضل والتكامل، المعادلات، ومسائل الفيزياء بدقة فائقة وشرح مفصّل وخطوة بخطوة.",
        description = "للمسائل الهندسية والرياضية المتقدمة"
    )
}

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isOffline: Boolean = false,
    val failedPrompt: String? = null,
    val feedbackGiven: String? = null
)

class R7BackendException(
    message: String,
    val errorCode: String? = null,
    val isOffline: Boolean = false,
    val httpCode: Int = 0
) : Exception(message)
