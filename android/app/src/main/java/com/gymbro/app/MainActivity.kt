package com.gymbro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gymbro.app.navigation.GymBroNavGraph
import com.gymbro.app.ui.theme.GymBroTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            GymBroTheme {
                GymBroNavGraph(
                    onFullyDrawn = { reportFullyDrawn() },
                )
            }
        }
    }
}
