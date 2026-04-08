package com.gymbro.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

@Composable
fun GymBroTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = GymBroDarkColorScheme,
        typography = GymBroTypography,
        content = content,
    )
}
