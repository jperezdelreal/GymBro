package com.gymbro.core.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.gymbro.core.model.SleepData
import com.gymbro.core.model.SleepQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "HealthConnectService"

        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        )
    }

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.w(TAG, "Health Connect not available", e)
            null
        }
    }

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    fun isInstalled(): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        return status != HealthConnectClient.SDK_UNAVAILABLE
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return REQUIRED_PERMISSIONS.all { it in granted }
    }

    // --- Sleep Data (last 7 days) ---

    suspend fun readSleepData(days: Int = 7): List<SleepData> {
        val client = healthConnectClient ?: return emptyList()
        val now = Instant.now()
        val start = now.minus(Duration.ofDays(days.toLong()))

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now),
                ),
            )

            response.records.map { session ->
                val duration = Duration.between(session.startTime, session.endTime)
                val durationMinutes = duration.toMinutes().toDouble()

                var deep = 0.0
                var rem = 0.0
                var light = 0.0
                var awake = 0.0

                session.stages.forEach { stage ->
                    val stageMinutes = Duration.between(stage.startTime, stage.endTime)
                        .toMinutes().toDouble()
                    when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_DEEP -> deep += stageMinutes
                        SleepSessionRecord.STAGE_TYPE_REM -> rem += stageMinutes
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> light += stageMinutes
                        SleepSessionRecord.STAGE_TYPE_AWAKE -> awake += stageMinutes
                    }
                }

                SleepData(
                    durationMinutes = durationMinutes,
                    quality = SleepQuality.fromDurationHours(durationMinutes / 60.0),
                    startTime = session.startTime,
                    endTime = session.endTime,
                    deepSleepMinutes = deep,
                    remSleepMinutes = rem,
                    lightSleepMinutes = light,
                    awakeMinutes = awake,
                )
            }.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read sleep data", e)
            emptyList()
        }
    }

    // --- Heart Rate / HRV (today) ---

    suspend fun readHeartRateToday(): List<Double> {
        val client = healthConnectClient ?: return emptyList()
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(todayStart, now),
                ),
            )

            response.records.flatMap { record ->
                record.samples.map { it.beatsPerMinute.toDouble() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read heart rate", e)
            emptyList()
        }
    }

    suspend fun getRestingHeartRate(): Double? {
        val rates = readHeartRateToday()
        if (rates.isEmpty()) return null
        // Approximate resting HR as lowest 10th percentile
        val sorted = rates.sorted()
        val cutoff = (sorted.size * 0.1).toInt().coerceAtLeast(1)
        return sorted.take(cutoff).average()
    }

    suspend fun getHRV(): Double? {
        // HRV approximation from heart rate variability in samples
        val rates = readHeartRateToday()
        if (rates.size < 2) return null
        // RMSSD-style approximation from BPM differences
        val diffs = rates.zipWithNext { a, b -> (b - a) * (b - a) }
        return kotlin.math.sqrt(diffs.average())
    }

    // --- Steps (today) ---

    suspend fun readStepsToday(): Long {
        val client = healthConnectClient ?: return 0
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(todayStart, now),
                ),
            )

            response.records.sumOf { it.count }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read steps", e)
            0
        }
    }

    // --- Write workout session ---

    suspend fun writeWorkoutSession(
        title: String,
        startTime: Instant,
        endTime: Instant,
        exerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ): Boolean {
        val client = healthConnectClient ?: return false

        return try {
            val record = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime),
                exerciseType = exerciseType,
                title = title,
            )
            client.insertRecords(listOf(record))
            Log.i(TAG, "Workout session written to Health Connect")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write workout session", e)
            false
        }
    }
}
