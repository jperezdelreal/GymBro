package com.gymbro.core.haptic

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun vibrateRestTimerFinished() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 200, 100, 200, 100, 400)
            val amplitudes = intArrayOf(0, 128, 0, 128, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 200, 100, 200, 100, 400)
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun playRestTimerFinishedSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            ringtone?.play()
        } catch (e: Exception) {
            // Silent failure — sound is optional
        }
    }
}
