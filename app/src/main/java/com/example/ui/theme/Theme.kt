package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QawmiColorScheme = lightColorScheme(
    primary = QawmiDeepGreen,
    onPrimary = Color.White,
    primaryContainer = QawmiMintSurface,
    onPrimaryContainer = QawmiTextDark,
    secondary = QawmiMediumGreen,
    onSecondary = Color.White,
    secondaryContainer = QawmiMintSurface,
    onSecondaryContainer = QawmiTextDark,
    tertiary = QawmiSeekerBlue,
    onTertiary = Color.White,
    background = QawmiSoftBg,
    onBackground = QawmiTextDark,
    surface = QawmiCardWhite,
    onSurface = QawmiTextDark,
    surfaceVariant = QawmiMintSurface,
    onSurfaceVariant = QawmiTextSecondary,
    outline = QawmiMintBorder,
    error = QawmiAlertRed
)

@Composable
fun ApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = QawmiColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    ApplicationTheme(content = content)
}
