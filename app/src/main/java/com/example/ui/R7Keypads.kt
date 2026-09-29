package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.engine.*
import com.example.ui.theme.R7Orange

enum class KeyType { NUM, OP, FN, EQUAL, CLEAR, MEMORY }

@Composable
fun R7Key(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: KeyType = KeyType.NUM,
    enabled: Boolean = true,
    testTag: String = "key_$label"
) {
    val bg = when {
        !enabled -> Color(0xFF1E2128)
        type == KeyType.EQUAL -> R7Orange
        type == KeyType.OP -> Color(0xFF333842)
        type == KeyType.CLEAR -> Color(0xFF3A3030)
        type == KeyType.FN -> Color(0xFF282C35)
        type == KeyType.MEMORY -> Color(0xFF232730)
        else -> Color(0xFF2B2F38)
    }

    val fg = when {
        !enabled -> Color(0xFF555963)
        type == KeyType.EQUAL -> Color.White
        type == KeyType.CLEAR -> Color(0xFFF28B82)
        type == KeyType.OP -> Color(0xFFF0F1F3)
        else -> Color(0xFFE2E4E8)
    }

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = if (label.length > 3) 14.sp else 19.sp,
            fontWeight = if (type == KeyType.EQUAL || type == KeyType.OP) FontWeight.Bold else FontWeight.Medium,
            fontFamily = if (type == KeyType.NUM) FontFamily.Monospace else FontFamily.SansSerif
        )
    }
}

@Composable
fun StandardKeypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Memory & Clear
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("MC", { onKey("MC") }, Modifier.weight(1f), type = KeyType.MEMORY)
            R7Key("MR", { onKey("MR") }, Modifier.weight(1f), type = KeyType.MEMORY)
            R7Key("M-", { onKey("M-") }, Modifier.weight(1f), type = KeyType.MEMORY)
            R7Key("M+", { onKey("M+") }, Modifier.weight(1f), type = KeyType.MEMORY)
        }
        // Row 2
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("C", { onKey("C") }, Modifier.weight(1f), type = KeyType.CLEAR)
            R7Key("±", { onKey("±") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("%", { onKey("%") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("÷", { onKey("÷") }, Modifier.weight(1f), type = KeyType.OP)
        }
        // Row 3
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("7", { onKey("7") }, Modifier.weight(1f))
            R7Key("8", { onKey("8") }, Modifier.weight(1f))
            R7Key("9", { onKey("9") }, Modifier.weight(1f))
            R7Key("×", { onKey("×") }, Modifier.weight(1f), type = KeyType.OP)
        }
        // Row 4
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("4", { onKey("4") }, Modifier.weight(1f))
            R7Key("5", { onKey("5") }, Modifier.weight(1f))
            R7Key("6", { onKey("6") }, Modifier.weight(1f))
            R7Key("-", { onKey("-") }, Modifier.weight(1f), type = KeyType.OP)
        }
        // Row 5
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("1", { onKey("1") }, Modifier.weight(1f))
            R7Key("2", { onKey("2") }, Modifier.weight(1f))
            R7Key("3", { onKey("3") }, Modifier.weight(1f))
            R7Key("+", { onKey("+") }, Modifier.weight(1f), type = KeyType.OP)
        }
        // Row 6
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("0", { onKey("0") }, Modifier.weight(1f))
            R7Key(".", { onKey(".") }, Modifier.weight(1f))
            R7Key("⌫", { onKey("BACKSPACE") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("=", { onKey("=") }, Modifier.weight(1f), type = KeyType.EQUAL)
        }
    }
}

@Composable
fun ScientificKeypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Sci Functions Row 1
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("sin", { onKey("sin(") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("cos", { onKey("cos(") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("tan", { onKey("tan(") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("ln", { onKey("ln(") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("log", { onKey("log(") }, Modifier.weight(1f), type = KeyType.FN)
        }
        // Sci Functions Row 2
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("√", { onKey("sqrt(") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("xʸ", { onKey("^") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("x²", { onKey("^2") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("n!", { onKey("!") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("1/x", { onKey("1/") }, Modifier.weight(1f), type = KeyType.FN)
        }
        // Sci Functions Row 3
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("(", { onKey("(") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key(")", { onKey(")") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("π", { onKey("π") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("e", { onKey("e") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("ANS", { onKey("ANS") }, Modifier.weight(1f), type = KeyType.FN)
        }

        // Standard Block
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("C", { onKey("C") }, Modifier.weight(1f), type = KeyType.CLEAR)
            R7Key("±", { onKey("±") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("%", { onKey("%") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("÷", { onKey("÷") }, Modifier.weight(1f), type = KeyType.OP)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("7", { onKey("7") }, Modifier.weight(1f))
            R7Key("8", { onKey("8") }, Modifier.weight(1f))
            R7Key("9", { onKey("9") }, Modifier.weight(1f))
            R7Key("×", { onKey("×") }, Modifier.weight(1f), type = KeyType.OP)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("4", { onKey("4") }, Modifier.weight(1f))
            R7Key("5", { onKey("5") }, Modifier.weight(1f))
            R7Key("6", { onKey("6") }, Modifier.weight(1f))
            R7Key("-", { onKey("-") }, Modifier.weight(1f), type = KeyType.OP)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("1", { onKey("1") }, Modifier.weight(1f))
            R7Key("2", { onKey("2") }, Modifier.weight(1f))
            R7Key("3", { onKey("3") }, Modifier.weight(1f))
            R7Key("+", { onKey("+") }, Modifier.weight(1f), type = KeyType.OP)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("0", { onKey("0") }, Modifier.weight(1f))
            R7Key(".", { onKey(".") }, Modifier.weight(1f))
            R7Key("⌫", { onKey("BACKSPACE") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("=", { onKey("=") }, Modifier.weight(1f), type = KeyType.EQUAL)
        }
    }
}

@Composable
fun ProgrammerKeypad(
    currentBase: NumBase,
    onBaseSelect: (NumBase) -> Unit,
    baseValues: BaseValues,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Base Switcher Rows (HEX / DEC / OCT / BIN with immediate values)
        NumBase.values().forEach { b ->
            val isSelected = currentBase == b
            val v = when (b) {
                NumBase.HEX -> baseValues.hex
                NumBase.DEC -> baseValues.dec
                NumBase.OCT -> baseValues.oct
                NumBase.BIN -> baseValues.bin
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF303642) else Color(0xFF1C2028))
                    .clickable { onBaseSelect(b) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(b.label, color = if (isSelected) R7Orange else Color(0xFF8E95A3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(v, color = Color(0xFFE2E4E8), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bitwise operations row
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("AND", { onKey("AND") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("OR", { onKey("OR") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("XOR", { onKey("XOR") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("NOT", { onKey("NOT") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("SHL", { onKey("SHL") }, Modifier.weight(1f), type = KeyType.FN)
            R7Key("SHR", { onKey("SHR") }, Modifier.weight(1f), type = KeyType.FN)
        }

        // Hex Keys A-F (enabled only in HEX)
        val isHex = currentBase == NumBase.HEX
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("A", { onKey("A") }, Modifier.weight(1f), enabled = isHex)
            R7Key("B", { onKey("B") }, Modifier.weight(1f), enabled = isHex)
            R7Key("C", { onKey("C") }, Modifier.weight(1f), enabled = isHex)
            R7Key("D", { onKey("D") }, Modifier.weight(1f), enabled = isHex)
            R7Key("E", { onKey("E") }, Modifier.weight(1f), enabled = isHex)
            R7Key("F", { onKey("F") }, Modifier.weight(1f), enabled = isHex)
        }

        // Numeric Keys Block
        val allowDigits89 = currentBase == NumBase.HEX || currentBase == NumBase.DEC
        val allowDigits27 = currentBase != NumBase.BIN

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("CLR", { onKey("C") }, Modifier.weight(1f), type = KeyType.CLEAR)
            R7Key("7", { onKey("7") }, Modifier.weight(1f), enabled = allowDigits27)
            R7Key("8", { onKey("8") }, Modifier.weight(1f), enabled = allowDigits89)
            R7Key("9", { onKey("9") }, Modifier.weight(1f), enabled = allowDigits89)
            R7Key("+", { onKey("+") }, Modifier.weight(1f), type = KeyType.OP)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("4", { onKey("4") }, Modifier.weight(1f), enabled = allowDigits27)
            R7Key("5", { onKey("5") }, Modifier.weight(1f), enabled = allowDigits27)
            R7Key("6", { onKey("6") }, Modifier.weight(1f), enabled = allowDigits27)
            R7Key("-", { onKey("-") }, Modifier.weight(1f), type = KeyType.OP)
            R7Key("×", { onKey("×") }, Modifier.weight(1f), type = KeyType.OP)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("1", { onKey("1") }, Modifier.weight(1f))
            R7Key("2", { onKey("2") }, Modifier.weight(1f), enabled = allowDigits27)
            R7Key("3", { onKey("3") }, Modifier.weight(1f), enabled = allowDigits27)
            R7Key("0", { onKey("0") }, Modifier.weight(1f))
            R7Key("÷", { onKey("÷") }, Modifier.weight(1f), type = KeyType.OP)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            R7Key("⌫", { onKey("BACKSPACE") }, Modifier.weight(2f), type = KeyType.FN)
            R7Key("=", { onKey("=") }, Modifier.weight(3f), type = KeyType.EQUAL)
        }
    }
}
