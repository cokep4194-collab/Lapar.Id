package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LaparDarkColorScheme = darkColorScheme(
    primary = LaparOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5D1903),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = LaparAmber,
    onSecondary = Color(0xFF281800),
    secondaryContainer = Color(0xFF472A00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = LaparMint,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = Color(0xFF005138),
    onTertiaryContainer = Color(0xFFBDF4DD),
    error = LaparCrimson,
    errorContainer = Color(0xFF5C1111),
    onErrorContainer = Color(0xFFFEE2E2),
    background = LaparDarkBg,
    onBackground = LaparDarkText,
    surface = LaparDarkSurface,
    onSurface = LaparDarkText,
    surfaceVariant = LaparDarkSurfaceVariant,
    onSurfaceVariant = LaparDarkTextMuted,
    outline = LaparDarkOutline
)

private val LaparLightColorScheme = lightColorScheme(
    primary = LaparOrange,
    onPrimary = Color.White,
    primaryContainer = LaparOrangeContainer,
    onPrimaryContainer = LaparOnOrangeContainer,
    secondary = LaparAmber,
    onSecondary = Color.White,
    secondaryContainer = LaparAmberContainer,
    onSecondaryContainer = LaparOnAmberContainer,
    tertiary = LaparMint,
    onTertiary = Color.White,
    tertiaryContainer = LaparMintContainer,
    onTertiaryContainer = LaparOnMintContainer,
    error = LaparCrimson,
    errorContainer = LaparCrimsonContainer,
    onErrorContainer = LaparOnCrimsonContainer,
    background = LaparCream,
    onBackground = LaparInk,
    surface = LaparPureWhite,
    onSurface = LaparInk,
    surfaceVariant = LaparSurfaceVariant,
    onSurfaceVariant = LaparInkMuted,
    outline = LaparOutline
)

val LaparShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun LaparIdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LaparDarkColorScheme else LaparLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LaparTypography,
        shapes = LaparShapes,
        content = content
    )
}
