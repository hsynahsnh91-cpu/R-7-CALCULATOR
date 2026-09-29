package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.AngleMode
import com.example.engine.HistoryRecord
import com.example.engine.R7Settings
import com.example.ui.theme.R7Orange
import com.example.ui.theme.R7PanelDark
import com.example.ui.theme.R7TextPrimary
import com.example.ui.theme.R7TextSecondary

@Composable
fun R7HistoryDialog(
    history: List<HistoryRecord>,
    onSelectRecord: (HistoryRecord) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var copiedToast by remember { mutableStateOf<String?>(null) }

    val filtered = history.filter {
        it.expression.contains(searchQuery, ignoreCase = true) ||
        it.result.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = R7PanelDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("r7_history_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل العمليات",
                        color = R7TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = onClearHistory, modifier = Modifier.testTag("btn_clear_history")) {
                            Icon(Icons.Default.Delete, contentDescription = "مسح الكل", tint = R7TextSecondary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = R7TextSecondary)
                        }
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في السجل...", color = R7TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = R7TextPrimary,
                        unfocusedTextColor = R7TextPrimary,
                        focusedBorderColor = R7Orange,
                        unfocusedBorderColor = Color(0xFF3C414D)
                    ),
                    singleLine = true
                )

                if (copiedToast != null) {
                    Text(
                        text = copiedToast!!,
                        color = R7Orange,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // List
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا عمليات بعد — نتيجتك الأولى ستظهر هنا.",
                            color = R7TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filtered) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF222630))
                                    .clickable { onSelectRecord(item) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.mode.uppercase(),
                                        color = R7TextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = item.expression,
                                        color = R7TextSecondary,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(item.result))
                                        copiedToast = "نُسخت النتيجة"
                                    }) {
                                        Text("نسخ", color = R7Orange, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = "= ${item.result}",
                                        color = R7TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun R7SettingsDialog(
    settings: R7Settings,
    onUpdateSettings: (R7Settings) -> Unit,
    onOpenCurrencyRates: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = R7PanelDark),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("r7_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الإعدادات",
                        color = R7TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = R7TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Angle Mode
                Text("وحدة قياس الزوايا", color = R7TextSecondary, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AngleMode.values().forEach { mode ->
                        FilterChip(
                            selected = settings.angleMode == mode,
                            onClick = { onUpdateSettings(settings.copy(angleMode = mode)) },
                            label = { Text(mode.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = R7Orange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الاهتزاز اللمسي", color = R7TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = settings.vibration,
                        onCheckedChange = { onUpdateSettings(settings.copy(vibration = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = R7Orange)
                    )
                }

                // Sound Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("صوت النقر الميكانيكي", color = R7TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = settings.sound,
                        onCheckedChange = { onUpdateSettings(settings.copy(sound = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = R7Orange)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Currency rates trigger
                OutlinedButton(
                    onClick = onOpenCurrencyRates,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = R7TextPrimary)
                ) {
                    Text("تعديل أسعار صرف العملات (محليًا)")
                }
            }
        }
    }
}

@Composable
fun R7AboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = R7PanelDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .testTag("r7_about_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("R-7 Calculator", color = R7TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("الإصدار 1.2.0 • MIT License", color = R7TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = R7TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "آلة حاسبة دقيقة للمهندسين والطلبة والمحاسبين. تصميم مادي مستوحى من أجهزة القياس العريقة مع محرك حسابي عشري دقيق.",
                    color = R7TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("سجل التغييرات", color = R7Orange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        ChangelogItem(
                            version = "1.2.0 (الحالي)",
                            date = "2026-09-28",
                            notes = "إضافة وضع المبرمج (HEX/DEC/OCT/BIN) بدقة 64 بت والعمليات المنطقية، ومحوّل الوحدات الشامل لـ 13 فئة هندسية وعلمية مع العملات المحلية."
                        )
                    }
                    item {
                        ChangelogItem(
                            version = "1.1.0",
                            date = "2026-04-10",
                            notes = "إضافة الوضع العلمي المتقدم مع الدوال المثلثية الدقيقة (DEG/RAD/GRAD)، واللوغاريتمات، والأقواس المتداخلة، ونظام السجل المستمر لـ 200 عملية."
                        )
                    }
                    item {
                        ChangelogItem(
                            version = "1.0.0",
                            date = "2025-11-20",
                            notes = "الإطلاق الأولي: محرك حسابي عشري بدقة تامة (0.1 + 0.2 = 0.3)، دلالة النسبة المئوية السياقية، والشاشة الفوسفورية ذات الترقيم الجدولي."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChangelogItem(version: String, date: String, notes: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(version, color = R7TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(date, color = R7TextSecondary, fontSize = 11.sp)
        }
        Text(notes, color = R7TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun R7CurrencyRatesDialog(
    rates: Map<String, Double>,
    onSaveRates: (Map<String, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val editedRates = remember { mutableStateMapOf<String, String>().apply {
        rates.forEach { (k, v) -> put(k, v.toString()) }
    }}

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = R7PanelDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أسعار الصرف (مقابل 1 USD)", color = R7TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = R7TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(editedRates.keys.toList()) { code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(code, color = R7TextPrimary, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = editedRates[code] ?: "1.0",
                                onValueChange = { editedRates[code] = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(130.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = R7TextPrimary,
                                    unfocusedTextColor = R7TextPrimary
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val parsed = editedRates.mapValues { it.value.toDoubleOrNull() ?: 1.0 }
                        onSaveRates(parsed)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = R7Orange)
                ) {
                    Text("حفظ الأسعار محليًا", color = Color.White)
                }
            }
        }
    }
}
