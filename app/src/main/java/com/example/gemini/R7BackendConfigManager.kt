package com.example.gemini

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class R7BackendConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("r7_backend_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_BACKEND_URL = "custom_backend_url"
        private const val KEY_CUSTOM_APP_KEY = "custom_app_key"

        // Default production backend URL injected from BuildConfig or production Cloud Run
        val DEFAULT_BACKEND_URL: String = try {
            BuildConfig.R7_BACKEND_URL
        } catch (_: Exception) {
            "https://r7-ai-backend-719083488800.europe-west2.run.app"
        }

        val DEFAULT_APP_KEY: String = try {
            BuildConfig.R7_APP_KEY
        } catch (_: Exception) {
            "r7-app-production-key-v1"
        }
    }

    var customBackendUrl: String
        get() = prefs.getString(KEY_CUSTOM_BACKEND_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_BACKEND_URL, value.trim()).apply()

    var customAppKey: String
        get() = prefs.getString(KEY_CUSTOM_APP_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_APP_KEY, value.trim()).apply()

    fun getEffectiveBackendUrl(): String {
        val custom = customBackendUrl.trim()
        val url = if (custom.isNotBlank()) custom.trimEnd('/') else DEFAULT_BACKEND_URL.trimEnd('/')
        // Ensure HTTPS protocol for secure production communication
        return when {
            url.startsWith("http://10.0.2.2") || url.startsWith("http://localhost") -> url // Dev emulator only
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            url.startsWith("https://") -> url
            else -> "https://$url"
        }
    }

    fun getEffectiveAppKey(): String {
        val custom = customAppKey.trim()
        return if (custom.isNotBlank()) custom else DEFAULT_APP_KEY
    }

    fun isUsingCustomEndpoint(): Boolean = customBackendUrl.isNotBlank()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
