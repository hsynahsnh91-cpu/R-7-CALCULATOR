package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gemini.*
import com.example.gemini.database.SelfImprovementRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class R7BackendIntegrationTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun `test R7BackendConfigManager enforces HTTPS for production endpoints`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configManager = R7BackendConfigManager(context)

        // Default URL must be HTTPS and set to the GitHub backend address
        val defaultUrl = configManager.getEffectiveBackendUrl()
        assertTrue("Production URL must start with https://", defaultUrl.startsWith("https://"))
        assertEquals("https://github.com/hsynahsnh91-cpu/R-7.git", defaultUrl)

        // Custom URL without scheme should be upgraded to https
        configManager.customBackendUrl = "api.mycalculator.com"
        assertEquals("https://api.mycalculator.com", configManager.getEffectiveBackendUrl())

        // Custom HTTP URL should be upgraded to https (unless local emulator)
        configManager.customBackendUrl = "http://api.production.com"
        assertEquals("https://api.production.com", configManager.getEffectiveBackendUrl())

        // Reset to default
        configManager.resetToDefaults()
        assertFalse(configManager.isUsingCustomEndpoint())
        assertEquals("https://github.com/hsynahsnh91-cpu/R-7.git", configManager.getEffectiveBackendUrl())
    }

    @Test
    fun `test R7SolveRequest and R7SolveResponse serialization roundtrip`() {
        val request = R7SolveRequest(
            question = "25 * 25",
            subject = "math",
            mode = "solve",
            language = "ar"
        )

        val reqAdapter = moshi.adapter(R7SolveRequest::class.java)
        val json = reqAdapter.toJson(request)
        assertTrue(json.contains("\"question\":\"25 * 25\""))
        assertTrue(json.contains("\"subject\":\"math\""))

        val respJson = """
            {
              "success": true,
              "answer": "625",
              "subject": "math",
              "ai_version": "r7-ai-1.0.0",
              "request_id": "r7-2026-test1234",
              "confidence": "verified",
              "verification_status": "verified",
              "processing_time_ms": 5
            }
        """.trimIndent()

        val respAdapter = moshi.adapter(R7SolveResponse::class.java)
        val response = respAdapter.fromJson(respJson)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("625", response.answer)
        assertEquals("r7-ai-1.0.0", response.ai_version)
        assertEquals("r7-2026-test1234", response.request_id)
        assertEquals("verified", response.confidence)
    }

    @Test
    fun `test R7LocalReasoningEngine solves parallel resistance step by step`() {
        val query = "احسب مقاومة مكافئة لدائرتين على التوازي بقيمة 10 و 20 أوم"
        val solution = R7LocalReasoningEngine.trySolve(query, DetectedLanguage.ARABIC)

        assertNotNull(solution)
        assertTrue("Solution should contain 6.67 Ω", solution!!.contains("6.67"))
        assertTrue("Solution should show formula steps", solution.contains("Req = (R1 × R2) / (R1 + R2)"))
    }

    @Test
    fun `test SelfCorrectionEngine solves parallel resistance without remote server error`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backendClient = R7BackendClient(context)
        val repository = SelfImprovementRepository(context)
        val engine = SelfCorrectionEngine(context, backendClient, repository)

        val result = engine.processUserMessage(
            userQuery = "احسب مقاومة مكافئة لدائرتين على التوازي بقيمة 10 و 20 أوم",
            history = emptyList(),
            role = ChatbotRole.GENERAL_FLASH
        )

        assertTrue("Should succeed without error", result.isSuccess)
        val processed = result.getOrNull()
        assertNotNull(processed)
        assertTrue("Answer should contain 6.67", processed!!.finalText.contains("6.67"))
        assertEquals("R-7 Local Engine", processed.modelUsed)
    }

    @Test
    fun `test SelfCorrectionEngine solves pure arithmetic locally without backend call`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backendClient = R7BackendClient(context)
        val repository = SelfImprovementRepository(context)
        val engine = SelfCorrectionEngine(context, backendClient, repository)

        val result = engine.processUserMessage(
            userQuery = "25 * 25",
            history = emptyList(),
            role = ChatbotRole.GENERAL_FLASH
        )

        assertTrue(result.isSuccess)
        val processed = result.getOrNull()
        assertNotNull(processed)
        assertTrue("Local answer should contain 625", processed!!.finalText.contains("625"))
        assertEquals("R-7 Local Engine", processed.modelUsed)
    }

    @Test
    fun `test Security Audit - BuildConfig does NOT expose GEMINI_API_KEY`() {
        val buildConfigClass = BuildConfig::class.java
        val fields = buildConfigClass.declaredFields.map { it.name }

        // Must NOT contain GEMINI_API_KEY
        assertFalse(
            "CRITICAL SECURITY: BuildConfig must NEVER expose GEMINI_API_KEY",
            fields.contains("GEMINI_API_KEY")
        )

        // Must contain R7_BACKEND_URL
        assertTrue(
            "BuildConfig should expose public R7_BACKEND_URL",
            fields.contains("R7_BACKEND_URL")
        )
    }
}
