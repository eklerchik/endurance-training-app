package com.trainwise.app.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = lightColorScheme(
    primary = Color(0xFF183C2A), onPrimary = Color.White,
    secondary = Color(0xFF1F7A4C), background = Color(0xFFF5F7F4),
    surface = Color.White, onSurface = Color(0xFF17211B), outline = Color(0xFFE3E9E4)
)
@Composable fun TrainwiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
