package com.example.gemini

import android.content.Context
import com.example.gemini.database.SelfImprovementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ProcessedResponse(
    val finalText: String,
    val modelUsed: String = "R-7 Secure AI",
    val wasFallback: Boolean = false,
    val isOffline: Boolean = false
)

class SelfCorrectionEngine(
    private val context: Context,
    private val backendClient: R7BackendClient,
    private val repository: SelfImprovementRepository
) {
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    suspend fun processUserMessage(
        userQuery: String,
        history: List<ChatMessage>,
        role: ChatbotRole
    ): Result<ProcessedResponse> {
        val trimmed = userQuery.trim()

        // 1. Question Analysis
        val analysis = QuestionClassifier.analyze(trimmed)

        // If explicitly ambiguous with clarification prompt available
        if (analysis.isAmbiguous && analysis.clarificationPrompt != null) {
            return Result.success(
                ProcessedResponse(
                    finalText = analysis.clarificationPrompt,
                    modelUsed = "R-7 Local Engine",
                    wasFallback = false
                )
            )
        }

        // 2. LOCAL REASONING & CALCULATOR FIRST
        // High-precision local solving for parallel/series circuits, derivatives, percentages, conversions, arithmetic
        val localSolution = R7LocalReasoningEngine.trySolve(trimmed, analysis.language)
        if (localSolution != null) {
            backgroundScope.launch {
                extractAndStoreMemoryFacts(trimmed, localSolution)
            }
            return Result.success(
                ProcessedResponse(
                    finalText = localSolution,
                    modelUsed = "R-7 Local Engine",
                    wasFallback = false
                )
            )
        }

        // 3. SECURE BACKEND INVOCATION
        val subject = when (analysis.domain) {
            QuestionDomain.MATH -> "math"
            QuestionDomain.PHYSICS_ENGINEERING -> "physics"
            QuestionDomain.PROGRAMMING -> "programming"
            QuestionDomain.UNIT_CONVERSION -> "conversion"
            else -> "general"
        }

        val languageCode = if (analysis.language == DetectedLanguage.ENGLISH) "en" else "ar"

        val apiResult = backendClient.solve(
            question = trimmed,
            subject = subject,
            mode = role.mode,
            language = languageCode,
            history = history
        )

        return if (apiResult.isSuccess) {
            apiResult.mapCatching { solveResponse ->
                val draft = solveResponse.answer ?: ""
                val verification = VerificationEngine.verifyAnswer(trimmed, draft, analysis)
                val finalAnswer = if (verification.correctionApplied && verification.correctedText != null) {
                    verification.correctedText
                } else {
                    draft
                }

                backgroundScope.launch {
                    extractAndStoreMemoryFacts(trimmed, finalAnswer)
                }

                ProcessedResponse(
                    finalText = finalAnswer,
                    modelUsed = solveResponse.ai_version ?: "R-7 Secure AI",
                    wasFallback = (solveResponse.verification_status == "fallback_verified")
                )
            }
        } else {
            val ex = apiResult.exceptionOrNull()
            // If network or remote backend is unreachable, provide graceful fallback
            val isArabic = (analysis.language != DetectedLanguage.ENGLISH)
            val fallbackMsg = if (isArabic) {
                "تعذر الحصول على رد من الخادم حالياً. جميع ميزات الآلة الحاسبة والتحويلات والحسابات العلمية تعمل بكامل كفاءتها."
            } else {
                "Unable to reach the server at this time. All calculator modes, conversions, and scientific features are fully operational."
            }

            Result.failure(
                R7BackendException(
                    message = ex?.message ?: fallbackMsg,
                    errorCode = (ex as? R7BackendException)?.errorCode ?: "SERVER_UNAVAILABLE",
                    isOffline = (ex as? R7BackendException)?.isOffline ?: false,
                    httpCode = (ex as? R7BackendException)?.httpCode ?: 0
                )
            )
        }
    }

    private suspend fun extractAndStoreMemoryFacts(query: String, answer: String) {
        val lowerQ = query.lowercase()
        if (lowerQ.contains("بالفهرنهايت") || lowerQ.contains("fahrenheit")) {
            repository.saveFact("وحدة الحرارة المفضلة", "فهرنهايت", "preference")
        } else if (lowerQ.contains("بالمئوية") || lowerQ.contains("celsius")) {
            repository.saveFact("وحدة الحرارة المفضلة", "مئوية", "preference")
        }
        if (lowerQ.contains("مقاومة") || lowerQ.contains("دائرة") || lowerQ.contains("circuit")) {
            repository.saveFact("موضوع العمل الحالي", "تحليل الدوائر الكهربائية والمقاومات", "context")
        } else if (lowerQ.contains("تفاضل") || lowerQ.contains("مشتقة") || lowerQ.contains("derivative")) {
            repository.saveFact("موضوع العمل الحالي", "حساب التفاضل والاشتقاق", "context")
        }
    }
}
