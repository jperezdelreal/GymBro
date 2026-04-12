package com.gymbro.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gymbro.core.R
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.RecordType

@Composable
fun MuscleGroup.localizedName(): String = stringResource(
    when (this) {
        MuscleGroup.CHEST -> R.string.muscle_group_chest
        MuscleGroup.BACK -> R.string.muscle_group_back
        MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_group_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_group_triceps
        MuscleGroup.QUADRICEPS -> R.string.muscle_group_quadriceps
        MuscleGroup.HAMSTRINGS -> R.string.muscle_group_hamstrings
        MuscleGroup.GLUTES -> R.string.muscle_group_glutes
        MuscleGroup.CALVES -> R.string.muscle_group_calves
        MuscleGroup.CORE -> R.string.muscle_group_core
        MuscleGroup.FOREARMS -> R.string.muscle_group_forearms
        MuscleGroup.FULL_BODY -> R.string.muscle_group_full_body
    },
)

@Composable
fun ExerciseCategory.localizedName(): String = stringResource(
    when (this) {
        ExerciseCategory.COMPOUND -> R.string.exercise_category_compound
        ExerciseCategory.ISOLATION -> R.string.exercise_category_isolation
        ExerciseCategory.ACCESSORY -> R.string.exercise_category_accessory
        ExerciseCategory.CARDIO -> R.string.exercise_category_cardio
    },
)

@Composable
fun Equipment.localizedName(): String = stringResource(
    when (this) {
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
        Equipment.KETTLEBELL -> R.string.equipment_kettlebell
        Equipment.BAND -> R.string.equipment_band
        Equipment.OTHER -> R.string.equipment_other
    },
)

@Composable
fun RecordType.localizedName(): String = stringResource(
    when (this) {
        RecordType.MAX_WEIGHT -> R.string.record_type_max_weight
        RecordType.MAX_REPS -> R.string.record_type_max_reps
        RecordType.MAX_VOLUME -> R.string.record_type_max_volume
        RecordType.MAX_E1RM -> R.string.record_type_max_e1rm
    },
)
