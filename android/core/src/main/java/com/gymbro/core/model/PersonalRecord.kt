package com.gymbro.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val type: RecordType,
    val value: Double,
    val date: Instant,
    val previousValue: Double?,
) : Parcelable

enum class RecordType(val displayName: String, val emoji: String) {
    MAX_WEIGHT("Max Weight", "🏋️"),
    MAX_REPS("Max Reps", "🔁"),
    MAX_VOLUME("Max Volume", "📦"),
    MAX_E1RM("Est. 1RM", "🏆"),
}
