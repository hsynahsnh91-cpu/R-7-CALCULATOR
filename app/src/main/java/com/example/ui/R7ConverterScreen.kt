package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.UnitConverter
import com.example.ui.theme.R7Orange
import com.example.ui.theme.R7PanelDark
import com.example.ui.theme.R7TextPrimary
import com.example.ui.theme.R7TextSecondary

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun R7ConverterScreen(
    converter: UnitConverter,
    onValueTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = UnitConverter.categories
    var selectedCategoryId by remember { mutableStateOf("length") }
    val currentCategory = categories.find { it.id == selectedCategoryId } ?: categories.first()

    var fromUnitId by remember(selectedCategoryId) { mutableStateOf(currentCategory.units.first().id) }
    var toUnitId by remember(selectedCategoryId) {
        mutableStateOf(currentCategory.units.getOrNull(1)?.id ?: currentCategory.units.first().id)
    }

    var inputAmountStr by remember { mutableStateOf("100") }

    val fromUnit = currentCategory.units.find { it.id == fromUnitId } ?: currentCategory.units.first()
    val toUnit = currentCategory.units.find { it.id == toUnitId } ?: currentCategory.units.first()

    val numericInput = inputAmountStr.toDoubleOrNull() ?: 0.0
    val convertedResult = remember(numericInput, selectedCategoryId, fromUnitId, toUnitId) {
        converter.convert(numericInput, selectedCategoryId, fromUnitId, toUnitId, decimals = 1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("r7_converter_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Category selector scroll
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat.id == selectedCategoryId
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryId = cat.id },
                    label = { Text(cat.nameAr, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = R7Orange,
                        selectedLabelColor = Color.White,
                        containerColor = R7PanelDark,
                        labelColor = R7TextSecondary
                    )
                )
            }
        }

        // From Unit Card
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = R7PanelDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("من:", color = R7TextSecondary, fontSize = 11.sp)
                var expandedFrom by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedFrom,
                    onExpandedChange = { expandedFrom = !expandedFrom }
                ) {
                    TextField(
                        value = "${fromUnit.nameAr} (${fromUnit.id})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedTextColor = R7TextPrimary,
                            unfocusedTextColor = R7TextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFrom,
                        onDismissRequest = { expandedFrom = false }
                    ) {
                        currentCategory.units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.nameAr} (${unit.id})") },
                                onClick = {
                                    fromUnitId = unit.id
                                    expandedFrom = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = inputAmountStr,
                    color = R7TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // To Unit Card
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = R7PanelDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("إلى:", color = R7TextSecondary, fontSize = 11.sp)
                var expandedTo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedTo,
                    onExpandedChange = { expandedTo = !expandedTo }
                ) {
                    TextField(
                        value = "${toUnit.nameAr} (${toUnit.id})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedTextColor = R7TextPrimary,
                            unfocusedTextColor = R7TextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTo,
                        onDismissRequest = { expandedTo = false }
                    ) {
                        currentCategory.units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.nameAr} (${unit.id})") },
                                onClick = {
                                    toUnitId = unit.id
                                    expandedTo = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = convertedResult.toString(),
                    color = R7Orange,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Quick Input Keypad for converter
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("7", { inputAmountStr = if (inputAmountStr == "0") "7" else inputAmountStr + "7" }, Modifier.weight(1f))
            R7Key("8", { inputAmountStr = if (inputAmountStr == "0") "8" else inputAmountStr + "8" }, Modifier.weight(1f))
            R7Key("9", { inputAmountStr = if (inputAmountStr == "0") "9" else inputAmountStr + "9" }, Modifier.weight(1f))
            R7Key("C", { inputAmountStr = "0" }, Modifier.weight(1f), type = KeyType.CLEAR)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("4", { inputAmountStr = if (inputAmountStr == "0") "4" else inputAmountStr + "4" }, Modifier.weight(1f))
            R7Key("5", { inputAmountStr = if (inputAmountStr == "0") "5" else inputAmountStr + "5" }, Modifier.weight(1f))
            R7Key("6", { inputAmountStr = if (inputAmountStr == "0") "6" else inputAmountStr + "6" }, Modifier.weight(1f))
            R7Key("⌫", {
                inputAmountStr = if (inputAmountStr.length <= 1) "0" else inputAmountStr.dropLast(1)
            }, Modifier.weight(1f), type = KeyType.FN)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("1", { inputAmountStr = if (inputAmountStr == "0") "1" else inputAmountStr + "1" }, Modifier.weight(1f))
            R7Key("2", { inputAmountStr = if (inputAmountStr == "0") "2" else inputAmountStr + "2" }, Modifier.weight(1f))
            R7Key("3", { inputAmountStr = if (inputAmountStr == "0") "3" else inputAmountStr + "3" }, Modifier.weight(1f))
            R7Key("0", { if (inputAmountStr != "0") inputAmountStr += "0" }, Modifier.weight(1f))
        }
    }
}
