package com.example.gemini

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

object ConfidenceManager {

    fun determineConfidence(
        analysis: QuestionAnalysis,
        verification: VerificationResult,
        wasFallback: Boolean
    ): ConfidenceLevel {
        return when {
            // If mathematically verified by R-7 engine
            verification.verifiedMathResult != null && (verification.isValid || verification.correctionApplied) -> {
                ConfidenceLevel.HIGH
            }
            // Simple question without error
            analysis.complexity == QuestionComplexity.SIMPLE && verification.isValid -> {
                ConfidenceLevel.HIGH
            }
            // Fallback model or intermediate complexity without math check
            wasFallback || analysis.complexity == QuestionComplexity.INTERMEDIATE -> {
                ConfidenceLevel.MEDIUM
            }
            // Complex or ambiguous
            analysis.complexity == QuestionComplexity.COMPLEX || analysis.isAmbiguous -> {
                ConfidenceLevel.MEDIUM
            }
            else -> ConfidenceLevel.HIGH
        }
    }
}
