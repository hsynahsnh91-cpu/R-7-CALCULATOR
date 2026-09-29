package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.*
import com.example.ui.theme.R7ChassisDark
import com.example.ui.theme.R7Orange
import com.example.ui.theme.R7PanelDark
import com.example.ui.theme.R7TextPrimary
import com.example.ui.theme.R7TextSecondary
import java.math.BigDecimal
import java.math.BigInteger

enum class CalcMode(val id: String, val labelAr: String) {
    STANDARD("standard", "قياسي"),
    SCIENTIFIC("scientific", "علمي"),
    PROGRAMMER("programmer", "مبرمج"),
    CONVERTER("converter", "محوّل")
}

@Composable
fun R7CalculatorApp(
    context: Context = LocalContext.current,
    modifier: Modifier = Modifier
) {
    val stateManager = remember { R7StateManager(context) }
    val audio = remember { R7Audio(context) }

    var settings by remember { mutableStateOf(stateManager.loadSettings()) }
    var history by remember { mutableStateOf(stateManager.loadHistory()) }
    var memoryValue by remember { mutableStateOf(stateManager.loadMemory()) }
    var currencyRates by remember { mutableStateOf(stateManager.loadCurrencyRates()) }

    var activeMode by remember { mutableStateOf(CalcMode.STANDARD) }
    var showChatScreen by remember { mutableStateOf(false) }

    // Mode independent states (§3 requirement)
    var standardExpr by remember { mutableStateOf("") }
    var standardResult by remember { mutableStateOf("0") }
    var standardError by remember { mutableStateOf<String?>(null) }

    var scientificExpr by remember { mutableStateOf("") }
    var scientificResult by remember { mutableStateOf("0") }
    var scientificError by remember { mutableStateOf<String?>(null) }

    // Programmer mode state
    var progBase by remember { mutableStateOf(NumBase.HEX) }
    var progInput by remember { mutableStateOf("0") }
    var progOp by remember { mutableStateOf<String?>(null) }
    var progStoredVal by remember { mutableStateOf<BigInteger?>(null) }
    val progEngine = remember { ProgrammerEngine() }
    var progBaseValues by remember { mutableStateOf(progEngine.getBaseRepresentations()) }

    val converter = remember(currencyRates) { UnitConverter(currencyRates) }

    // Dialogs state
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    // Feedback helper
    fun triggerFeedback() {
        if (settings.vibration) audio.vibrate(8L)
        if (settings.sound) audio.playClick()
    }

    if (showChatScreen) {
        BackHandler { showChatScreen = false }
        R7GeminiChatScreen(
            onBack = { showChatScreen = false },
            onInsertIntoCalculator = { insertedValue ->
                if (activeMode == CalcMode.SCIENTIFIC) {
                    scientificExpr += insertedValue
                } else {
                    standardExpr += insertedValue
                }
                showChatScreen = false
            }
        )
        return
    }

    // Key Handler for Standard and Scientific
    fun handleMathKey(key: String, isScientific: Boolean) {
        triggerFeedback()
        var currentExpr = if (isScientific) scientificExpr else standardExpr

        when (key) {
            "C" -> {
                if (isScientific) {
                    scientificExpr = ""
                    scientificResult = "0"
                    scientificError = null
                } else {
                    standardExpr = ""
                    standardResult = "0"
                    standardError = null
                }
            }
            "BACKSPACE" -> {
                if (currentExpr.isNotEmpty()) {
                    currentExpr = currentExpr.dropLast(1)
                    if (isScientific) scientificExpr = currentExpr else standardExpr = currentExpr
                }
            }
            "=" -> {
                if (currentExpr.isNotEmpty()) {
                    val eval = R7MathEngine.evaluate(
                        currentExpr,
                        settings.angleMode,
                        lastAns = BigDecimal(if (isScientific) scientificResult else standardResult).abs()
                    )
                    if (eval.error != null) {
                        if (isScientific) scientificError = eval.error else standardError = eval.error
                    } else {
                        val res = eval.formattedResult
                        if (isScientific) {
                            scientificResult = res
                            scientificError = null
                        } else {
                            standardResult = res
                            standardError = null
                        }
                        // Add to history
                        val newRec = HistoryRecord(
                            id = System.currentTimeMillis().toString(),
                            expression = currentExpr,
                            result = res,
                            mode = if (isScientific) "scientific" else "standard",
                            timestamp = System.currentTimeMillis()
                        )
                        val updated = listOf(newRec) + history
                        history = updated
                        stateManager.saveHistory(updated)
                    }
                }
            }
            "±" -> {
                if (currentExpr.startsWith("-")) {
                    currentExpr = currentExpr.substring(1)
                } else {
                    currentExpr = "-$currentExpr"
                }
                if (isScientific) scientificExpr = currentExpr else standardExpr = currentExpr
            }
            "MC" -> {
                memoryValue = "0"
                stateManager.saveMemory("0")
            }
            "MR" -> {
                currentExpr += memoryValue
                if (isScientific) scientificExpr = currentExpr else standardExpr = currentExpr
            }
            "M+" -> {
                val curVal = BigDecimal(if (isScientific) scientificResult else standardResult)
                val mem = (BigDecimal(memoryValue) + curVal).toPlainString()
                memoryValue = mem
                stateManager.saveMemory(mem)
            }
            "M-" -> {
                val curVal = BigDecimal(if (isScientific) scientificResult else standardResult)
                val mem = (BigDecimal(memoryValue) - curVal).toPlainString()
                memoryValue = mem
                stateManager.saveMemory(mem)
            }
            else -> {
                if (currentExpr.isEmpty() && key in listOf("+", "-", "×", "÷", "^")) {
                    val prevRes = if (isScientific) scientificResult else standardResult
                    if (prevRes != "0") {
                        currentExpr = prevRes + key
                    } else {
                        currentExpr += key
                    }
                } else {
                    currentExpr += key
                }
                if (isScientific) {
                    scientificExpr = currentExpr
                    scientificError = null
                } else {
                    standardExpr = currentExpr
                    standardError = null
                }
            }
        }
    }

    // Key Handler for Programmer Mode
    fun handleProgrammerKey(key: String) {
        triggerFeedback()
        when (key) {
            "C" -> {
                progInput = "0"
                progOp = null
                progStoredVal = null
                progEngine.setValue(BigInteger.ZERO)
                progBaseValues = progEngine.getBaseRepresentations()
            }
            "BACKSPACE" -> {
                progInput = if (progInput.length <= 1) "0" else progInput.dropLast(1)
                val num = progEngine.parseInput(progInput, progBase)
                progEngine.setValue(num)
                progBaseValues = progEngine.getBaseRepresentations()
            }
            "NOT" -> {
                val cur = progEngine.parseInput(progInput, progBase)
                val res = progEngine.not(cur)
                progEngine.setValue(res)
                progInput = when (progBase) {
                    NumBase.HEX -> res.toString(16).uppercase()
                    NumBase.DEC -> res.toString(10)
                    NumBase.OCT -> res.toString(8)
                    NumBase.BIN -> res.toString(2)
                }
                progBaseValues = progEngine.getBaseRepresentations()
            }
            "AND", "OR", "XOR", "SHL", "SHR", "+", "-", "×", "÷" -> {
                progStoredVal = progEngine.parseInput(progInput, progBase)
                progOp = key
                progInput = "0"
            }
            "=" -> {
                if (progOp != null && progStoredVal != null) {
                    val bVal = progEngine.parseInput(progInput, progBase)
                    try {
                        val res = progEngine.executeOp(progStoredVal!!, progOp!!, bVal)
                        progEngine.setValue(res)
                        progInput = when (progBase) {
                            NumBase.HEX -> res.toString(16).uppercase()
                            NumBase.DEC -> res.toString(10)
                            NumBase.OCT -> res.toString(8)
                            NumBase.BIN -> res.toString(2)
                        }
                        progBaseValues = progEngine.getBaseRepresentations()
                    } catch (_: Exception) {}
                    progOp = null
                    progStoredVal = null
                }
            }
            else -> {
                progInput = if (progInput == "0") key else progInput + key
                val num = progEngine.parseInput(progInput, progBase)
                progEngine.setValue(num)
                progBaseValues = progEngine.getBaseRepresentations()
            }
        }
    }

    val hasMem = memoryValue != "0"

    Scaffold(
        containerColor = R7ChassisDark,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "R-7",
                        color = Color(0xFFE8842C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1.2.0",
                        color = R7TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dedicated AI Chat Button with Orange Glow
                    IconButton(
                        onClick = { showChatScreen = true },
                        modifier = Modifier.testTag("btn_gemini_chat")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "مساعد الذكاء الاصطناعي Gemini",
                            tint = R7Orange
                        )
                    }

                    IconButton(onClick = { showHistoryDialog = true }, modifier = Modifier.testTag("btn_history")) {
                        Icon(Icons.Default.Refresh, contentDescription = "السجل", tint = R7TextSecondary)
                    }
                    IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.testTag("btn_settings")) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = R7TextSecondary)
                    }
                    IconButton(onClick = { showAboutDialog = true }, modifier = Modifier.testTag("btn_about")) {
                        Icon(Icons.Default.Info, contentDescription = "حول", tint = R7TextSecondary)
                    }
                }
            }

            // Mode Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(R7PanelDark)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CalcMode.values().forEach { mode ->
                    val isSelected = activeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF333844) else Color.Transparent)
                            .clickable {
                                triggerFeedback()
                                activeMode = mode
                            }
                            .padding(vertical = 8.dp)
                            .testTag("tab_${mode.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.labelAr,
                            color = if (isSelected) R7TextPrimary else R7TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Unit (for Standard, Scientific, Programmer)
            if (activeMode != CalcMode.CONVERTER) {
                val currentExpr = when (activeMode) {
                    CalcMode.STANDARD -> standardExpr
                    CalcMode.SCIENTIFIC -> scientificExpr
                    CalcMode.PROGRAMMER -> if (progOp != null) "${progStoredVal?.toString(progBase.radix)?.uppercase()} $progOp" else ""
                    else -> ""
                }
                val currentResult = when (activeMode) {
                    CalcMode.STANDARD -> standardResult
                    CalcMode.SCIENTIFIC -> scientificResult
                    CalcMode.PROGRAMMER -> progInput
                    else -> "0"
                }
                val currentError = when (activeMode) {
                    CalcMode.STANDARD -> standardError
                    CalcMode.SCIENTIFIC -> scientificError
                    else -> null
                }

                R7Display(
                    expression = currentExpr,
                    result = currentResult,
                    livePreview = null,
                    error = currentError,
                    angleMode = settings.angleMode,
                    hasMemory = hasMem,
                    modeLabel = activeMode.labelAr,
                    isDarkTheme = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeMode) {
                    CalcMode.STANDARD -> {
                        StandardKeypad(onKey = { handleMathKey(it, isScientific = false) })
                    }
                    CalcMode.SCIENTIFIC -> {
                        ScientificKeypad(onKey = { handleMathKey(it, isScientific = true) })
                    }
                    CalcMode.PROGRAMMER -> {
                        ProgrammerKeypad(
                            currentBase = progBase,
                            onBaseSelect = { b ->
                                progBase = b
                                val v = progEngine.currentValue
                                progInput = when (b) {
                                    NumBase.HEX -> v.toString(16).uppercase()
                                    NumBase.DEC -> v.toString(10)
                                    NumBase.OCT -> v.toString(8)
                                    NumBase.BIN -> v.toString(2)
                                }
                            },
                            baseValues = progBaseValues,
                            onKey = { handleProgrammerKey(it) }
                        )
                    }
                    CalcMode.CONVERTER -> {
                        R7ConverterScreen(
                            converter = converter,
                            onValueTapped = { triggerFeedback() }
                        )
                    }
                }
            }
        }
    }

    // Modals
    if (showHistoryDialog) {
        R7HistoryDialog(
            history = history,
            onSelectRecord = { rec ->
                when (activeMode) {
                    CalcMode.STANDARD -> {
                        standardExpr = rec.result
                        standardResult = rec.result
                    }
                    CalcMode.SCIENTIFIC -> {
                        scientificExpr = rec.result
                        scientificResult = rec.result
                    }
                    else -> {}
                }
                showHistoryDialog = false
            },
            onClearHistory = {
                history = emptyList()
                stateManager.saveHistory(emptyList())
            },
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showSettingsDialog) {
        R7SettingsDialog(
            settings = settings,
            onUpdateSettings = {
                settings = it
                stateManager.saveSettings(it)
            },
            onOpenCurrencyRates = { showCurrencyDialog = true },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAboutDialog) {
        R7AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showCurrencyDialog) {
        R7CurrencyRatesDialog(
            rates = currencyRates,
            onSaveRates = {
                currencyRates = it
                stateManager.saveCurrencyRates(it)
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }
}
