package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = R7Orange,
    secondary = R7LcdGreen,
    tertiary = R7PanelDark,
    background = R7ChassisDark,
    surface = R7PanelDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = R7TextPrimary,
    onSurface = R7TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = R7Orange,
    secondary = R7PanelLight,
    tertiary = R7LcdIvory,
    background = R7ChassisLight,
    surface = R7PanelLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = R7TextPrimaryLight,
    onSurface = R7TextPrimaryLight
)

@Composable
fun R7Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
