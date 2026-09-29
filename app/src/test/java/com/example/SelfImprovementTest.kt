package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gemini.ConfidenceLevel
import com.example.gemini.ConfidenceManager
import com.example.gemini.DetectedLanguage
import com.example.gemini.PrivacySanitizer
import com.example.gemini.QuestionClassifier
import com.example.gemini.QuestionComplexity
import com.example.gemini.QuestionDomain
import com.example.gemini.QuestionIntent
import com.example.gemini.VerificationEngine
import com.example.gemini.database.SelfImprovementRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SelfImprovementTest {

    @Test
    fun `test QuestionClassifier detects Arabic parallel resistance question`() {
        val query = "احسب مقاومة مكافئة لدائرتين على التوازي بقيمة 10 و 20 أوم"
        val analysis = QuestionClassifier.analyze(query)

        assertEquals(QuestionDomain.PHYSICS_ENGINEERING, analysis.domain)
        assertEquals(QuestionIntent.CALCULATION, analysis.intent)
        assertEquals(DetectedLanguage.ARABIC, analysis.language)
        assertTrue(analysis.requiresCalculation)
        assertTrue(analysis.requiresVerification)
        assertFalse(analysis.isAmbiguous)
    }

    @Test
    fun `test QuestionClassifier detects derivative calculus question`() {
        val query = "احسب مشتقة f(x) = x^3 - 5x + 2"
        val analysis = QuestionClassifier.analyze(query)

        assertEquals(QuestionDomain.MATH, analysis.domain)
        assertEquals(DetectedLanguage.MIXED, analysis.language)
        assertTrue(analysis.requiresCalculation)
    }

    @Test
    fun `test QuestionClassifier detects ambiguity and provides clarification`() {
        val query = "احسب"
        val analysis = QuestionClassifier.analyze(query)

        assertTrue(analysis.isAmbiguous)
        assertNotNull(analysis.clarificationPrompt)
    }

    @Test
    fun `test PrivacySanitizer redacts sensitive API keys and tokens`() {
        val sensitive = "My key is AIzaSyD1234567890abcdefghijklmnopqrst and password is secret123"
        val sanitized = PrivacySanitizer.sanitize(sensitive)

        assertFalse(sanitized.contains("AIzaSyD1234567890abcdefghijklmnopqrst"))
        assertTrue(sanitized.contains("[REDACTED_SENSITIVE_DATA]"))
    }

    @Test
    fun `test VerificationEngine verifies parallel resistor math accurately`() {
        val query = "احسب مقاومة مكافئة لدائرتين على التوازي 10 و 20 أوم"
        val draftWithCorrect = "المقاومة المكافئة على التوازي هي 6.67 أوم."
        val analysis = QuestionClassifier.analyze(query)

        val result = VerificationEngine.verifyAnswer(query, draftWithCorrect, analysis)
        assertTrue(result.isValid)
        assertNotNull(result.verifiedMathResult)
    }

    @Test
    fun `test VerificationEngine auto-corrects incorrect parallel resistance`() {
        val query = "احسب مقاومة مكافئة لدائرتين على التوازي 10 و 20 أوم"
        val draftWithWrong = "المقاومة المكافئة هي 30 أوم."
        val analysis = QuestionClassifier.analyze(query)

        val result = VerificationEngine.verifyAnswer(query, draftWithWrong, analysis)
        assertFalse(result.isValid)
        assertTrue(result.correctionApplied)
        assertNotNull(result.correctedText)
        assertTrue(result.correctedText!!.contains("6.67") || result.correctedText!!.contains("6.6666"))
    }

    @Test
    fun `test ConfidenceManager assigns HIGH confidence when verified`() {
        val query = "احسب 2 + 2"
        val analysis = QuestionClassifier.analyze(query)
        val verification = VerificationEngine.verifyAnswer(query, "الناتج هو 4", analysis)

        val confidence = ConfidenceManager.determineConfidence(analysis, verification, wasFallback = false)
        assertEquals(ConfidenceLevel.HIGH, confidence)
    }

    @Test
    fun `test SelfImprovementRepository saves non-sensitive facts and handles feedback`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SelfImprovementRepository(context)

        repo.saveFact("وحدة الحرارة المفضلة", "مئوية", "preference")
        val prompt = repo.getRelevantContextPrompt("ما هي وحدة الحرارة؟")
        assertTrue(prompt.contains("مئوية"))

        // Record feedback
        repo.recordFeedback("msg_1", "احسب التوازي", "6.67", "THUMBS_UP")
        val metrics = repo.getMetrics()
        assertTrue(metrics.totalFeedback >= 1)
        assertTrue(metrics.positiveFeedback >= 1)
    }
}
