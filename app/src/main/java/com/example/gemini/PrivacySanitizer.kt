package com.example.gemini

/**
 * Sanitizes text to prevent storing private credentials, API keys, passwords,
 * tokens, or sensitive personal data in the long-term memory system.
 */
object PrivacySanitizer {

    private val SENSITIVE_PATTERNS = listOf(
        // API Keys & Tokens
        Regex("""(?i)\b(key|secret|token|password|passwd|pwd|auth|bearer|apikey)\s*(?:is|[:=])\s*\S+"""),
        Regex("""(?i)\bAIza[0-9A-Za-z\-_]{30,45}\b"""), // Google API Key
        Regex("""(?i)\b[a-zA-Z0-9_-]{20,}\.[a-zA-Z0-9_-]{20,}\b"""), // JWT / bearer like
        // Credit card numbers
        Regex("""\b(?:\d{4}[-\s]?){3}\d{4}\b"""),
        // Email addresses
        Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"""),
        // Phone numbers (international and local formats)
        Regex("""\b(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b""")
    )

    fun sanitize(text: String): String {
        var result = text
        for (pattern in SENSITIVE_PATTERNS) {
            result = result.replace(pattern, "[REDACTED_SENSITIVE_DATA]")
        }
        return result
    }

    fun containsSensitiveData(text: String): Boolean {
        return SENSITIVE_PATTERNS.any { it.containsMatchIn(text) }
    }
}
