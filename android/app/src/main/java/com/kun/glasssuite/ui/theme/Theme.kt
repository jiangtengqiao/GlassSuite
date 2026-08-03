package com.kun.glasssuite.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/** 预设主题色板 */
val AccentPresets = listOf(
    "玻璃蓝" to 0xFF4D7CFE,
    "极光紫" to 0xFF7B5CFF,
    "薄荷绿" to 0xFF00B578,
    "海盐蓝" to 0xFF3D7FFF,
    "琥珀橙" to 0xFFFF8000,
    "樱粉" to 0xFFFF6B9D,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4D7CFE),
    onPrimary = Color.White,
    secondary = Color(0xFF4D7CFE),
    surface = Color(0xFFFDFDFD),
    background = Color(0xFFF7F7F9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4D7CFE),
    onPrimary = Color.White,
    secondary = Color(0xFF4D7CFE),
    surface = Color(0xFF17171A),
    background = Color(0xFF101014),
)

@Composable
fun AppTheme(
    accentHex: String,
    dark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val useDark = dark ?: isSystemInDarkTheme()
    val accent = remember(accentHex) {
        runCatching { Color(android.graphics.Color.parseColor(accentHex)) }
            .getOrDefault(Color(0xFF4D7CFE))
    }
    val base = if (useDark) DarkColors else LightColors
    val colors = base.copy(primary = accent, secondary = accent, tertiary = accent)
    MaterialTheme(colorScheme = colors, content = content)
}
