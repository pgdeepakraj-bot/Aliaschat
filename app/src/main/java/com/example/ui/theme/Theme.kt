package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFFD9FDD3),
    secondary = ChatCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1F2C34),
    onSecondaryContainer = Color(0xFFE9EDEF),
    tertiary = EmeraldLight,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = TextMutedDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008069),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9FDD3),
    onPrimaryContainer = Color(0xFF005C4B),
    secondary = Color(0xFF027EB5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F3F9),
    onSecondaryContainer = Color(0xFF111B21),
    tertiary = EmeraldPrimary,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFF8696A0)
)

@Composable
fun AliasChatTheme(
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
