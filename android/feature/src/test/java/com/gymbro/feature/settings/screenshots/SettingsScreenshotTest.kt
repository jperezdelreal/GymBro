package com.gymbro.feature.settings.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.preferences.UserPreferences.TrainingPhase
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.feature.settings.SettingsScreen
import com.gymbro.feature.settings.SettingsState
import org.junit.Rule
import org.junit.Test

class SettingsScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    @Test
    fun settingsScreen_defaultState() {
        paparazzi.snapshot {
            GymBroTheme {
                SettingsScreen(
                    state = SettingsState(
                        weightUnit = WeightUnit.KG,
                        trainingPhase = TrainingPhase.MAINTENANCE,
                        defaultRestTimer = 90,
                        autoStartRestTimer = true,
                        notificationsEnabled = false,
                        isHealthConnectAvailable = true,
                        isHealthConnectConnected = false,
                        appVersion = "1.0.0",
                        isLoading = false,
                    ),
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }
    }

    @Test
    fun settingsScreen_lbsWithBulkPhase() {
        paparazzi.snapshot {
            GymBroTheme {
                SettingsScreen(
                    state = SettingsState(
                        weightUnit = WeightUnit.LBS,
                        trainingPhase = TrainingPhase.BULK,
                        defaultRestTimer = 120,
                        autoStartRestTimer = false,
                        notificationsEnabled = true,
                        isHealthConnectAvailable = true,
                        isHealthConnectConnected = true,
                        appVersion = "1.0.0",
                        isLoading = false,
                    ),
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }
    }

    @Test
    fun settingsScreen_cutPhaseWithShortRest() {
        paparazzi.snapshot {
            GymBroTheme {
                SettingsScreen(
                    state = SettingsState(
                        weightUnit = WeightUnit.KG,
                        trainingPhase = TrainingPhase.CUT,
                        defaultRestTimer = 60,
                        autoStartRestTimer = true,
                        notificationsEnabled = true,
                        isHealthConnectAvailable = false,
                        isHealthConnectConnected = false,
                        appVersion = "1.0.0",
                        isLoading = false,
                    ),
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }
    }
}
