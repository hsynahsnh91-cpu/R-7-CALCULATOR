package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AngleMode

@Composable
fun R7Display(
    expression: String,
    result: String,
    livePreview: String?,
    error: String?,
    angleMode: AngleMode,
    hasMemory: Boolean,
    modeLabel: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val lcdBg = if (isDarkTheme) Color(0xFF101410) else Color(0xFF26282E)
    val lcdFg = if (isDarkTheme) Color(0xFF9DB89A) else Color(0xFFF5F1E6)
    val lcdDim = if (isDarkTheme) Color(0xFF5A7258) else Color(0xFF8E9094)
    val lcdError = Color(0xFFE57373)
    val lcdBadgeBg = if (isDarkTheme) Color(0xFF1C241C) else Color(0xFF383B44)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(lcdBg)
            .border(1.5.dp, Color(0xFF2E333D), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("r7_display")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status bar (Top)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Angle Mode Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(lcdBadgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = angleMode.name,
                            color = lcdFg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (hasMemory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(lcdBadgeBg)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "M",
                                color = lcdFg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = modeLabel,
                        color = lcdDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Error indicator if present
                if (error != null) {
                    Text(
                        text = error,
                        color = lcdError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                } else if (!livePreview.isNullOrEmpty()) {
                    Text(
                        text = "= $livePreview",
                        color = lcdDim,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expression Row (dir=LTR always)
            Text(
                text = if (expression.isEmpty()) " " else expression,
                color = lcdDim,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Result Row (dir=LTR always)
            Text(
                text = if (error != null) error else (if (result.isEmpty()) "0" else result),
                color = if (error != null) lcdError else lcdFg,
                fontSize = if (result.length > 11) 24.sp else 34.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("r7_result_text")
            )
        }
    }
}
