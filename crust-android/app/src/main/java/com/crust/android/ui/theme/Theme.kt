package com.crust.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB5410C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFF77574A),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFF8F6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59C),
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF882900),
    secondary = Color(0xFFE7BDB0),
    background = Color(0xFF201A18),
    surface = Color(0xFF201A18),
)

@Composable
fun CrustTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
