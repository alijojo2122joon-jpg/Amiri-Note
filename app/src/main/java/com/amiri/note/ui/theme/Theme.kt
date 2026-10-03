package com.amiri.note.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Liquid-glass colour scheme. `background` is transparent on purpose: every
 * screen sits on top of [com.amiri.note.ui.common.GlassBackground], and panels
 * are translucent white so the colourful backdrop shows through.
 */
private val GlassColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xCC4F7CFF),
    onPrimaryContainer = Color.White,
    secondary = BlobTeal,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = Color(0x594F7CFF),
    onSecondaryContainer = Color.White,
    background = Color.Transparent,
    onBackground = TextPrimary,
    surface = GlassFill,
    onSurface = TextPrimary,
    surfaceVariant = Color(0x1FFFFFFF),
    onSurfaceVariant = TextSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.Transparent,
    surfaceContainerLow = GlassFill,
    surfaceContainer = GlassDialog,
    surfaceContainerHigh = GlassDialog,
    surfaceContainerHighest = GlassDialog,
    outline = Color(0x40FFFFFF),
    outlineVariant = Color(0x1FFFFFFF),
    error = FailRed,
    onError = Color.White
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun AmiriTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val c = WindowCompat.getInsetsController(window, view)
            c.isAppearanceLightStatusBars = false
            c.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = GlassColors,
        typography = AppTypography,
        content = content
    )
}
