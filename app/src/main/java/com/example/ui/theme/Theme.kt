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

private val MindoraDarkColorScheme = darkColorScheme(
    primary = MindoraVioletAI,
    onPrimary = MindoraWhite,
    primaryContainer = MindoraVioletContainerDark,
    onPrimaryContainer = Color(0xFFDCD3FF),
    secondary = MindoraBlueSecondary,
    onSecondary = MindoraWhite,
    secondaryContainer = MindoraBlueContainerDark,
    onSecondaryContainer = Color(0xFFD0E2FF),
    tertiary = MindoraSuccess,
    onTertiary = MindoraWhite,
    background = MindoraDeepBlack,
    onBackground = MindoraWhite,
    surface = MindoraDarkSurface,
    onSurface = MindoraWhite,
    surfaceVariant = MindoraDarkCard,
    onSurfaceVariant = MindoraGray,
    outline = MindoraDarkBorder,
    outlineVariant = Color(0xFF232630),
    error = MindoraDanger,
    onError = MindoraWhite
)

private val MindoraLightColorScheme = lightColorScheme(
    primary = MindoraVioletAI,
    onPrimary = MindoraWhite,
    primaryContainer = MindoraVioletContainerLight,
    onPrimaryContainer = Color(0xFF2D1B8E),
    secondary = MindoraBlueSecondary,
    onSecondary = MindoraWhite,
    secondaryContainer = MindoraBlueContainerLight,
    onSecondaryContainer = Color(0xFF0B357A),
    tertiary = MindoraSuccess,
    onTertiary = MindoraWhite,
    background = MindoraSecondaryBg,
    onBackground = MindoraDeepBlack,
    surface = MindoraWhite,
    onSurface = MindoraDeepBlack,
    surfaceVariant = Color(0xFFEEF0F6),
    onSurfaceVariant = MindoraLightTextSecondary,
    outline = MindoraLightBorder,
    outlineVariant = Color(0xFFD8DCE8),
    error = MindoraDanger,
    onError = MindoraWhite
)

val MindoraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MindoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MindoraDarkColorScheme else MindoraLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MindoraTypography,
        shapes = MindoraShapes,
        content = content
    )
}
