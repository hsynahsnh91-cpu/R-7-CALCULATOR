package com.example.gemini

enum class QuestionDomain {
    MATH,
    PHYSICS_ENGINEERING,
    PROGRAMMING,
    UNIT_CONVERSION,
    GENERAL
}

enum class QuestionIntent {
    CALCULATION,
    EXPLANATION,
    COMPARISON,
    FORMULA_DERIVATION,
    CODE_SOLUTION,
    CLARIFICATION,
    FACTUAL
}

enum class QuestionComplexity {
    SIMPLE,
    INTERMEDIATE,
    COMPLEX
}

enum class DetectedLanguage {
    ARABIC,
    ENGLISH,
    MIXED
}

data class QuestionAnalysis(
    val rawQuery: String,
    val domain: QuestionDomain,
    val intent: QuestionIntent,
    val complexity: QuestionComplexity,
    val language: DetectedLanguage,
    val requiresCalculation: Boolean,
    val requiresVerification: Boolean,
    val isAmbiguous: Boolean,
    val clarificationPrompt: String? = null,
    val extractedMathExpression: String? = null
)

object QuestionClassifier {

    private val ARABIC_CHAR_REGEX = Regex("""[\u0600-\u06FF]""")
    private val LATIN_CHAR_REGEX = Regex("""[a-zA-Z]""")

    // Basic arithmetic and expression regexes
    private val MATH_KEYWORDS_AR = listOf("احسب", "اوجد", "تكامل", "مشتقة", "معادلة", "مقاومة", "مجموع", "ضرب", "قسمة", "نسبة مئوية", "جذر", "لوغاريتم", "زاوية", "مثلثات")
    private val MATH_KEYWORDS_EN = listOf("calculate", "compute", "integral", "derivative", "equation", "resistance", "sum", "multiply", "divide", "percentage", "sqrt", "log", "sin", "cos", "tan")

    private val UNIT_KEYWORDS_AR = listOf("تحويل", "كم يساوي", "فهرنهايت", "مئوية", "كيلو", "متر", "رطل", "غرام", "دولار", "ريال", "درهم", "يورو")
    private val UNIT_KEYWORDS_EN = listOf("convert", "how many", "celsius", "fahrenheit", "kg", "meter", "pound", "gram", "usd", "sar", "eur")

    private val CODE_KEYWORDS = listOf("code", "كود", "دالة", "function", "javascript", "python", "kotlin", "html", "css", "api", "bug", "خطأ برمجية", "class", "async")

    fun analyze(query: String): QuestionAnalysis {
        val trimmed = query.trim()
        val lower = trimmed.lowercase()

        // 1. Detect Language
        val hasArabic = ARABIC_CHAR_REGEX.containsMatchIn(trimmed)
        val hasEnglish = LATIN_CHAR_REGEX.containsMatchIn(trimmed)
        val language = when {
            hasArabic && hasEnglish -> DetectedLanguage.MIXED
            hasArabic -> DetectedLanguage.ARABIC
            else -> DetectedLanguage.ENGLISH
        }

        // 2. Detect Domain
        val isMath = MATH_KEYWORDS_AR.any { lower.contains(it) } ||
                     MATH_KEYWORDS_EN.any { lower.contains(it) } ||
                     Regex("""[\d\s+\-*/^=÷×()√]+""").find(trimmed)?.value?.length ?: 0 > 5

        val isUnit = UNIT_KEYWORDS_AR.any { lower.contains(it) } ||
                     UNIT_KEYWORDS_EN.any { lower.contains(it) }

        val isCode = CODE_KEYWORDS.any { lower.contains(it) }

        val isPhysics = lower.contains("مقاومة") || lower.contains("جهد") || lower.contains("تيار") ||
                         lower.contains("دائرة") || lower.contains("resistance") || lower.contains("voltage") ||
                         lower.contains("current") || lower.contains("circuit") || lower.contains("سرعة") || lower.contains("تسارع")

        val domain = when {
            isPhysics -> QuestionDomain.PHYSICS_ENGINEERING
            isMath -> QuestionDomain.MATH
            isUnit -> QuestionDomain.UNIT_CONVERSION
            isCode -> QuestionDomain.PROGRAMMING
            else -> QuestionDomain.GENERAL
        }

        // 3. Detect Intent
        val intent = when {
            lower.contains("احسب") || lower.contains("calculate") || lower.contains("compute") -> QuestionIntent.CALCULATION
            lower.contains("اشرح") || lower.contains("explain") || lower.contains("كيف") || lower.contains("how") -> QuestionIntent.EXPLANATION
            lower.contains("قارن") || lower.contains("compare") || lower.contains("الفرق") || lower.contains("difference") -> QuestionIntent.COMPARISON
            isCode -> QuestionIntent.CODE_SOLUTION
            lower.contains("اشتقاق") || lower.contains("derive") || lower.contains("قانون") -> QuestionIntent.FORMULA_DERIVATION
            else -> QuestionIntent.FACTUAL
        }

        // 4. Complexity & Ambiguity
        val isExtremelyShort = trimmed.split("""\s+""".toRegex()).size <= 2 && !trimmed.contains("=") && !trimmed.contains("+")
        val isAmbiguous = isExtremelyShort && (trimmed == "احسب" || trimmed == "calculate" || trimmed == "مساعدة" || trimmed == "help")

        val complexity = when {
            trimmed.length < 25 && !lower.contains("تفاضل") && !lower.contains("derivative") -> QuestionComplexity.SIMPLE
            trimmed.length < 100 -> QuestionComplexity.INTERMEDIATE
            else -> QuestionComplexity.COMPLEX
        }

        // 5. Math extraction attempt
        val extractedMath = extractMathExpression(trimmed)

        val requiresCalculation = isMath || isPhysics || isUnit || extractedMath != null
        val requiresVerification = isMath || isPhysics || isUnit

        val clarification = if (isAmbiguous) {
            if (language == DetectedLanguage.ARABIC) {
                "سؤالك موجز جدًا؛ يُرجى توضيح المسألة أو المعادلة التي ترغب في حسابها أو شرحها بدقة."
            } else {
                "Your question is very brief; please specify the problem or equation you would like solved."
            }
        } else null

        return QuestionAnalysis(
            rawQuery = trimmed,
            domain = domain,
            intent = intent,
            complexity = complexity,
            language = language,
            requiresCalculation = requiresCalculation,
            requiresVerification = requiresVerification,
            isAmbiguous = isAmbiguous,
            clarificationPrompt = clarification,
            extractedMathExpression = extractedMath
        )
    }

    private fun extractMathExpression(text: String): String? {
        // Find arithmetic substrings like 10 + 20, 50 * 15%, 1/(1/10 + 1/20), etc.
        val mathMatch = Regex("""(?:\(?\d+(?:\.\d+)?\)?\s*[\+\-\*\/×÷\^]\s*)+\(?\d+(?:\.\d+)?\)?""").find(text)
        return mathMatch?.value?.trim()
    }
}
