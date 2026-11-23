package ru.player.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.math.abs

fun hslColor(h: Float, s: Float, l: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((hh / 60f) % 2f - 1f))
    val m = l - c / 2f
    val r: Float
    val g: Float
    val b: Float
    when {
        hh < 60f -> { r = c; g = x; b = 0f }
        hh < 120f -> { r = x; g = c; b = 0f }
        hh < 180f -> { r = 0f; g = c; b = x }
        hh < 240f -> { r = 0f; g = x; b = c }
        hh < 300f -> { r = x; g = 0f; b = c }
        else -> { r = c; g = 0f; b = x }
    }
    return Color(r + m, g + m, b + m)
}

fun schemeFromHue(h: Float, dark: Boolean): ColorScheme {
    val t = h + 60f
    return if (!dark) {
        lightColorScheme(
            primary = hslColor(h, 0.55f, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hslColor(h, 0.75f, 0.90f),
            onPrimaryContainer = hslColor(h, 0.85f, 0.12f),
            secondary = hslColor(h, 0.25f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hslColor(h, 0.35f, 0.89f),
            onSecondaryContainer = hslColor(h, 0.45f, 0.12f),
            tertiary = hslColor(t, 0.30f, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hslColor(t, 0.45f, 0.89f),
            onTertiaryContainer = hslColor(t, 0.55f, 0.12f),
            background = hslColor(h, 0.30f, 0.98f),
            onBackground = hslColor(h, 0.15f, 0.10f),
            surface = hslColor(h, 0.30f, 0.98f),
            onSurface = hslColor(h, 0.15f, 0.10f),
            surfaceVariant = hslColor(h, 0.25f, 0.90f),
            onSurfaceVariant = hslColor(h, 0.15f, 0.30f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslColor(h, 0.30f, 0.96f),
            surfaceContainer = hslColor(h, 0.28f, 0.94f),
            surfaceContainerHigh = hslColor(h, 0.26f, 0.92f),
            surfaceContainerHighest = hslColor(h, 0.24f, 0.90f),
            outline = hslColor(h, 0.10f, 0.47f),
            outlineVariant = hslColor(h, 0.20f, 0.80f),
            inverseSurface = hslColor(h, 0.10f, 0.20f),
            inverseOnSurface = hslColor(h, 0.20f, 0.95f),
            inversePrimary = hslColor(h, 0.85f, 0.80f)
        )
    } else {
        darkColorScheme(
            primary = hslColor(h, 0.85f, 0.80f),
            onPrimary = hslColor(h, 0.80f, 0.20f),
            primaryContainer = hslColor(h, 0.55f, 0.30f),
            onPrimaryContainer = hslColor(h, 0.85f, 0.90f),
            secondary = hslColor(h, 0.30f, 0.80f),
            onSecondary = hslColor(h, 0.40f, 0.20f),
            secondaryContainer = hslColor(h, 0.25f, 0.30f),
            onSecondaryContainer = hslColor(h, 0.35f, 0.90f),
            tertiary = hslColor(t, 0.40f, 0.80f),
            onTertiary = hslColor(t, 0.50f, 0.20f),
            tertiaryContainer = hslColor(t, 0.30f, 0.30f),
            onTertiaryContainer = hslColor(t, 0.40f, 0.90f),
            background = hslColor(h, 0.15f, 0.07f),
            onBackground = hslColor(h, 0.15f, 0.90f),
            surface = hslColor(h, 0.15f, 0.07f),
            onSurface = hslColor(h, 0.15f, 0.90f),
            surfaceVariant = hslColor(h, 0.12f, 0.25f),
            onSurfaceVariant = hslColor(h, 0.15f, 0.80f),
            surfaceContainerLowest = hslColor(h, 0.15f, 0.05f),
            surfaceContainerLow = hslColor(h, 0.15f, 0.09f),
            surfaceContainer = hslColor(h, 0.15f, 0.11f),
            surfaceContainerHigh = hslColor(h, 0.14f, 0.15f),
            surfaceContainerHighest = hslColor(h, 0.13f, 0.19f),
            outline = hslColor(h, 0.08f, 0.60f),
            outlineVariant = hslColor(h, 0.12f, 0.25f),
            inverseSurface = hslColor(h, 0.15f, 0.90f),
            inverseOnSurface = hslColor(h, 0.15f, 0.20f),
            inversePrimary = hslColor(h, 0.55f, 0.40f)
        )
    }
}

@Composable
fun PlayerTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    var scheme: ColorScheme = if (settings.dynamicColor) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        schemeFromHue(settings.hue, dark)
    }
    if (dark && settings.amoled) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0B0B0B),
            surfaceContainer = Color(0xFF121212)
        )
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
