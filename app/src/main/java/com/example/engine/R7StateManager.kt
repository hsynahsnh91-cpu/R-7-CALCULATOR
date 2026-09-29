package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryRecord(
    val id: String,
    val expression: String,
    val result: String,
    val mode: String,
    val timestamp: Long
)

data class R7Settings(
    val theme: String = "dark",
    val lang: String = "ar",
    val sound: Boolean = false,
    val vibration: Boolean = true,
    val angleMode: AngleMode = AngleMode.DEG,
    val converterDecimals: Int = 1
)

class R7StateManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("r7:v1", Context.MODE_PRIVATE)

    fun loadSettings(): R7Settings {
        val theme = prefs.getString("theme", "dark") ?: "dark"
        val lang = prefs.getString("lang", "ar") ?: "ar"
        val sound = prefs.getBoolean("sound", false)
        val vibration = prefs.getBoolean("vibration", true)
        val angleModeStr = prefs.getString("angleMode", "DEG") ?: "DEG"
        val angleMode = try { AngleMode.valueOf(angleModeStr) } catch (_: Exception) { AngleMode.DEG }
        val decimals = prefs.getInt("converterDecimals", 1)

        return R7Settings(
            theme = theme,
            lang = lang,
            sound = sound,
            vibration = vibration,
            angleMode = angleMode,
            converterDecimals = decimals
        )
    }

    fun saveSettings(settings: R7Settings) {
        prefs.edit()
            .putString("theme", settings.theme)
            .putString("lang", settings.lang)
            .putBoolean("sound", settings.sound)
            .putBoolean("vibration", settings.vibration)
            .putString("angleMode", settings.angleMode.name)
            .putInt("converterDecimals", settings.converterDecimals)
            .apply()
    }

    fun loadMemory(): String {
        return prefs.getString("memory", "0") ?: "0"
    }

    fun saveMemory(value: String) {
        prefs.edit().putString("memory", value).apply()
    }

    fun loadHistory(): List<HistoryRecord> {
        val raw = prefs.getString("history", "[]") ?: "[]"
        val list = mutableListOf<HistoryRecord>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HistoryRecord(
                        id = obj.optString("id", i.toString()),
                        expression = obj.optString("expression", ""),
                        result = obj.optString("result", ""),
                        mode = obj.optString("mode", "standard"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveHistory(history: List<HistoryRecord>) {
        val limited = history.take(200)
        val arr = JSONArray()
        for (item in limited) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("expression", item.expression)
            obj.put("result", item.result)
            obj.put("mode", item.mode)
            obj.put("timestamp", item.timestamp)
            arr.put(obj)
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    fun loadCurrencyRates(): Map<String, Double> {
        val raw = prefs.getString("currencyRates", "") ?: ""
        if (raw.isEmpty()) return UnitConverter.defaultCurrencyRates

        val map = mutableMapOf<String, Double>()
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optDouble(k, 1.0)
            }
        } catch (_: Exception) {
            return UnitConverter.defaultCurrencyRates
        }
        return map
    }

    fun saveCurrencyRates(rates: Map<String, Double>) {
        val obj = JSONObject()
        for ((k, v) in rates) {
            obj.put(k, v)
        }
        prefs.edit().putString("currencyRates", obj.toString()).apply()
    }
}
