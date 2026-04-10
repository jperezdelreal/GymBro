package com.gymbro.feature.profile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.auth.GymBroUser
import com.gymbro.core.sync.service.SyncStatus
import com.gymbro.feature.profile.ProfileScreen
import com.gymbro.feature.profile.ProfileState
import org.junit.Rule
import org.junit.Test

class ProfileScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    @Test
    fun profileScreen_signedIn() {
        paparazzi.snapshot {
            GymBroTheme {
                ProfileScreen(
                    state = ProfileState(
                        user = GymBroUser(
                            uid = "user-123",
                            displayName = "Alex",
                            email = "alex@gymbro.com",
                            isAnonymous = false,
                        ),
                        isSignedIn = true,
                        syncStatus = SyncStatus.SUCCESS,
                        lastSyncTime = System.currentTimeMillis(),
                        autoSyncEnabled = true,
                        isLoading = false,
                        error = null,
                    ),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun profileScreen_signedOut() {
        paparazzi.snapshot {
            GymBroTheme {
                ProfileScreen(
                    state = ProfileState(
                        user = null,
                        isSignedIn = false,
                        syncStatus = SyncStatus.IDLE,
                        lastSyncTime = null,
                        autoSyncEnabled = false,
                        isLoading = false,
                        error = null,
                    ),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun profileScreen_anonymousUser() {
        paparazzi.snapshot {
            GymBroTheme {
                ProfileScreen(
                    state = ProfileState(
                        user = GymBroUser(
                            uid = "anon-456",
                            displayName = null,
                            email = null,
                            isAnonymous = true,
                        ),
                        isSignedIn = true,
                        syncStatus = SyncStatus.DISABLED,
                        lastSyncTime = null,
                        autoSyncEnabled = false,
                        isLoading = false,
                        error = null,
                    ),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun profileScreen_syncing() {
        paparazzi.snapshot {
            GymBroTheme {
                ProfileScreen(
                    state = ProfileState(
                        user = GymBroUser(
                            uid = "user-123",
                            displayName = "Alex",
                            email = "alex@gymbro.com",
                            isAnonymous = false,
                        ),
                        isSignedIn = true,
                        syncStatus = SyncStatus.SYNCING,
                        lastSyncTime = System.currentTimeMillis() - 300_000,
                        autoSyncEnabled = true,
                        isLoading = false,
                        error = null,
                    ),
                    onEvent = {},
                )
            }
        }
    }
}
