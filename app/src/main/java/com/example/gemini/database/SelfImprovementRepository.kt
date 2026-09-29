package com.example.gemini.database

import android.content.Context
import com.example.gemini.PrivacySanitizer
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class SelfImprovementRepository(context: Context) {

    private val db = SelfImprovementDatabase.getInstance(context)
    private val dao = db.dao()

    val allFactsFlow: Flow<List<MemoryFactEntity>> = dao.getAllFactsFlow()
    val allPatternsFlow: Flow<List<LearnedPatternEntity>> = dao.getAllPatternsFlow()
    val allFeedbackFlow: Flow<List<FeedbackSignalEntity>> = dao.getAllFeedbackFlow()

    suspend fun getRelevantContextPrompt(query: String): String {
        val facts = dao.getAllFacts()
        if (facts.isEmpty()) return ""

        val lowerQuery = query.lowercase()
        // Select facts that match keywords or high-value preferences
        val matchingFacts = facts.filter { fact ->
            fact.category == "preference" ||
            lowerQuery.contains(fact.factKey.lowercase()) ||
            lowerQuery.contains(fact.factValue.lowercase())
        }.take(5)

        if (matchingFacts.isEmpty()) return ""

        val sb = StringBuilder("سياق المحادثات السابقة الموثق والمعتمد:\n")
        for (f in matchingFacts) {
            sb.append("- ${f.factKey}: ${f.factValue}\n")
            // Increment access count
            dao.updateFact(f.copy(accessCount = f.accessCount + 1))
        }
        return sb.toString().trim()
    }

    suspend fun getAdaptiveInstructionsForDomain(domain: String): String {
        val patterns = dao.getPatternsByDomain(domain)
        if (patterns.isEmpty()) return ""

        val sb = StringBuilder()
        for (p in patterns) {
            if (p.failureCount > 0 && p.adaptivePromptInstruction.isNotBlank()) {
                sb.append("• توجيه تحسين دقيق: ${p.adaptivePromptInstruction} (استراتيجية التحقق: ${p.verificationStrategy})\n")
            }
        }
        return sb.toString().trim()
    }

    suspend fun saveFact(key: String, value: String, category: String = "context") {
        if (PrivacySanitizer.containsSensitiveData(key) || PrivacySanitizer.containsSensitiveData(value)) {
            return // Skip sensitive info
        }
        val cleanKey = PrivacySanitizer.sanitize(key.trim())
        val cleanVal = PrivacySanitizer.sanitize(value.trim())

        if (cleanKey.isBlank() || cleanVal.isBlank()) return

        val existing = dao.getFactByKey(cleanKey)
        if (existing != null) {
            dao.updateFact(existing.copy(factValue = cleanVal, timestamp = System.currentTimeMillis()))
        } else {
            dao.insertFact(
                MemoryFactEntity(
                    factKey = cleanKey,
                    factValue = cleanVal,
                    category = category
                )
            )
        }
    }

    suspend fun recordFeedback(
        messageId: String,
        userQuery: String,
        aiAnswer: String,
        feedbackType: String,
        userCorrection: String? = null
    ) {
        val cleanQuery = PrivacySanitizer.sanitize(userQuery.take(200))
        val cleanAnswer = PrivacySanitizer.sanitize(aiAnswer.take(200))
        val cleanCorrection = userCorrection?.let { PrivacySanitizer.sanitize(it.take(300)) }

        val signal = FeedbackSignalEntity(
            messageId = messageId,
            userQuery = cleanQuery,
            aiAnswerSnippet = cleanAnswer,
            feedbackType = feedbackType,
            userCorrection = cleanCorrection,
            wasValidated = false
        )
        dao.insertFeedback(signal)

        // Update learned patterns according to feedback
        updatePatternFromFeedback(cleanQuery, feedbackType, cleanCorrection)
    }

    private suspend fun updatePatternFromFeedback(query: String, feedbackType: String, correction: String?) {
        val lower = query.lowercase()
        val patternKey = when {
            lower.contains("توازي") || lower.contains("parallel") -> "parallel_circuits"
            lower.contains("توالي") || lower.contains("series") -> "series_circuits"
            lower.contains("%") || lower.contains("نسبة") || lower.contains("percent") -> "percentage_calculation"
            lower.contains("مشتقة") || lower.contains("derivative") -> "calculus_derivative"
            lower.contains("تكامل") || lower.contains("integral") -> "calculus_integral"
            lower.contains("تحويل") || lower.contains("convert") -> "unit_conversion"
            else -> "general_query"
        }

        val domain = when (patternKey) {
            "parallel_circuits", "series_circuits" -> "PHYSICS_ENGINEERING"
            "percentage_calculation", "calculus_derivative", "calculus_integral" -> "MATH"
            "unit_conversion" -> "UNIT_CONVERSION"
            else -> "GENERAL"
        }

        val existing = dao.getPattern(patternKey)
        if (feedbackType == "THUMBS_UP") {
            if (existing != null) {
                dao.insertOrUpdatePattern(
                    existing.copy(
                        successCount = existing.successCount + 1,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                dao.insertOrUpdatePattern(
                    LearnedPatternEntity(
                        patternKey = patternKey,
                        domain = domain,
                        successCount = 1,
                        failureCount = 0,
                        verificationStrategy = "VERIFY_STANDARD",
                        adaptivePromptInstruction = "الحفاظ على الدقة والوضوح المعتاد"
                    )
                )
            }
        } else if (feedbackType in listOf("THUMBS_DOWN", "REPORT_INCORRECT", "REQUEST_BETTER")) {
            val strategy = when (domain) {
                "MATH" -> "VERIFY_WITH_R7_ENGINE_DOUBLE_CHECK"
                "PHYSICS_ENGINEERING" -> "CHECK_FORMULA_AND_RECIPROCAL_SUMS"
                "UNIT_CONVERSION" -> "VERIFY_EXACT_CONVERSION_FACTORS"
                else -> "CHECK_CONSISTENCY_AND_DETAIL"
            }

            val instruction = when (patternKey) {
                "parallel_circuits" -> "احرص على استخدام قانون مقلوب المقاومات بدقة: 1/Req = 1/R1 + 1/R2 ثم إيجاد المقلوب النهائي مع ذكر وحدات الأوم."
                "percentage_calculation" -> "عند حساب النسبة المئوية المركبة (مثل A + B%)، تأكد من حساب قيمة الزيادة (A * B/100) وإضافتها للأصل."
                "unit_conversion" -> "أعد التحقق من معامل التحويل الرسمي ومطابقة الوحدات الفيزيائية بدقة."
                else -> "قدّم شرحًا خطوة بخطوة وتأكد من صحة النتائج المنطقية قبل الإخراج."
            }

            if (existing != null) {
                dao.insertOrUpdatePattern(
                    existing.copy(
                        failureCount = existing.failureCount + 1,
                        verificationStrategy = strategy,
                        adaptivePromptInstruction = instruction,
                        reliability = if (existing.failureCount > 2) "MEDIUM" else "HIGH",
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                dao.insertOrUpdatePattern(
                    LearnedPatternEntity(
                        patternKey = patternKey,
                        domain = domain,
                        successCount = 0,
                        failureCount = 1,
                        verificationStrategy = strategy,
                        adaptivePromptInstruction = instruction,
                        reliability = "HIGH"
                    )
                )
            }
        }
    }

    suspend fun getMetrics(): FeedbackMetrics {
        val total = dao.getTotalFeedbackCount()
        val positive = dao.getPositiveFeedbackCount()
        val negative = dao.getNegativeFeedbackCount()
        val facts = dao.getAllFacts().size
        val patterns = dao.getAllPatterns().size
        return FeedbackMetrics(
            totalFeedback = total,
            positiveFeedback = positive,
            negativeFeedback = negative,
            memoryFactsCount = facts,
            learnedPatternsCount = patterns
        )
    }

    suspend fun clearMemory() {
        dao.clearAllFacts()
    }

    suspend fun clearPatterns() {
        dao.clearAllPatterns()
    }
}

data class FeedbackMetrics(
    val totalFeedback: Int,
    val positiveFeedback: Int,
    val negativeFeedback: Int,
    val memoryFactsCount: Int,
    val learnedPatternsCount: Int
)
