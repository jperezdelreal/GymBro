package com.gymbro.feature.onboarding.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.preferences.UserPreferences.ExperienceLevel
import com.gymbro.core.preferences.UserPreferences.TrainingGoal
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.feature.onboarding.OnboardingScreen
import com.gymbro.feature.onboarding.OnboardingState
import org.junit.Rule
import org.junit.Test

class OnboardingScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    @Test
    fun onboardingScreen_initialState() {
        paparazzi.snapshot {
            GymBroTheme {
                OnboardingScreen(
                    state = OnboardingState(
                        currentPage = 0,
                        selectedUnit = WeightUnit.KG,
                        userName = "",
                        selectedGoal = TrainingGoal.HYPERTROPHY,
                    ),
                    onEvent = {}
                )
            }
        }
    }

    @Test
    fun onboardingScreen_withUserName() {
        paparazzi.snapshot {
            GymBroTheme {
                OnboardingScreen(
                    state = OnboardingState(
                        currentPage = 1,
                        selectedUnit = WeightUnit.KG,
                        userName = "Alex",
                        selectedGoal = TrainingGoal.HYPERTROPHY,
                    ),
                    onEvent = {}
                )
            }
        }
    }

    @Test
    fun onboardingScreen_lbsSelected() {
        paparazzi.snapshot {
            GymBroTheme {
                OnboardingScreen(
                    state = OnboardingState(
                        currentPage = 2,
                        selectedUnit = WeightUnit.LBS,
                        userName = "Alex",
                        selectedGoal = TrainingGoal.STRENGTH,
                    ),
                    onEvent = {}
                )
            }
        }
    }
}
