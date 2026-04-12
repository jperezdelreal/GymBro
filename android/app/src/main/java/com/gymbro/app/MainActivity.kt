package com.gymbro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.app.navigation.GymBroNavGraph
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.preferences.UserPreferences.ThemePreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferences: UserPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val themePreference by userPreferences.themePreference.collectAsStateWithLifecycle(
                initialValue = ThemePreference.DARK
            )
            val darkTheme = when (themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }
            
            GymBroTheme(darkTheme = darkTheme) {
                GymBroNavGraph(
                    onFullyDrawn = { reportFullyDrawn() },
                )
            }
        }
    }
}
