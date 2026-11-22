package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = InkPrimary,
    primaryContainer = ElectricVioletDim,
    onPrimaryContainer = InkPrimary,
    secondary = CyberCyan,
    onSecondary = StudioObsidian,
    secondaryContainer = StudioPanelElevated,
    onSecondaryContainer = CyberCyan,
    tertiary = KeyframeAmber,
    onTertiary = StudioObsidian,
    background = StudioObsidian,
    onBackground = InkPrimary,
    surface = StudioPanelDark,
    onSurface = InkPrimary,
    surfaceVariant = StudioPanelElevated,
    onSurfaceVariant = InkSecondary,
    outline = StudioHairline,
    outlineVariant = StudioPanelHover,
    error = RoseAlert,
    onError = InkPrimary
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AfterMotionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = AfterMotionTypography,
        shapes = StudioShapes,
        content = content
    )
}
