package com.gymbro.core.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "gymbro_user_prefs",
        Context.MODE_PRIVATE,
    )

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    fun getPreferredUnit(): WeightUnit {
        val unitName = prefs.getString(KEY_PREFERRED_UNIT, WeightUnit.KG.name)
        return WeightUnit.entries.find { it.name == unitName } ?: WeightUnit.KG
    }

    fun setPreferredUnit(unit: WeightUnit) {
        prefs.edit().putString(KEY_PREFERRED_UNIT, unit.name).apply()
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun setUserName(name: String?) {
        if (name.isNullOrBlank()) {
            prefs.edit().remove(KEY_USER_NAME).apply()
        } else {
            prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
        }
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_PREFERRED_UNIT = "preferred_unit"
        private const val KEY_USER_NAME = "user_name"
    }
}

enum class WeightUnit {
    KG,
    LBS
}
