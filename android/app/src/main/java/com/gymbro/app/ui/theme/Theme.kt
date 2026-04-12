package com.gymbro.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.gymbro.core.ui.theme.*

private val GymBroDarkColorScheme = darkColorScheme(
    primary = AccentGreenStart,
    secondary = AccentCyanStart,
    tertiary = AccentAmberStart,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    surfaceTint = SurfacePrimary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = AccentRed,
    onError = OnError,
)

private val GymBroLightColorScheme = lightColorScheme(
    primary = AccentGreenStart,
    secondary = AccentCyanStart,
    tertiary = AccentAmberStart,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    surfaceTint = SurfacePrimaryLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = AccentRed,
    onError = Color(0xFFFFFFFF),
)

@Composable
fun GymBroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GymBroDarkColorScheme else GymBroLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GymBroTypography,
        content = content,
    )
}
