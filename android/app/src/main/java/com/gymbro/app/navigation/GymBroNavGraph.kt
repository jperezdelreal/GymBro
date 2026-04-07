package com.gymbro.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.exerciselibrary.ExerciseLibraryRoute
import com.gymbro.feature.workout.ActiveWorkoutRoute
import com.gymbro.feature.workout.WorkoutSummaryScreen

private val AccentGreen = Color(0xFF00FF87)

@Composable
fun GymBroNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "exercise_library",
    ) {
        composable("exercise_library") {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate("active_workout") },
                        containerColor = AccentGreen,
                        contentColor = Color.Black,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Start Workout", modifier = Modifier.size(28.dp))
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ExerciseLibraryRoute(
                        onNavigateToDetail = { exerciseId ->
                            navController.navigate("exercise_detail/$exerciseId")
                        },
                    )
                }
            }
        }
        composable("exercise_detail/{exerciseId}") {
            PlaceholderScreen(title = "Exercise Detail")
        }
        composable("active_workout") { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val pickedId = savedStateHandle.get<String>("picked_exercise_id")
            val pickedName = savedStateHandle.get<String>("picked_exercise_name")
            val pickedMuscle = savedStateHandle.get<String>("picked_exercise_muscle")
            val pickedCategory = savedStateHandle.get<String>("picked_exercise_category")
            val pickedEquipment = savedStateHandle.get<String>("picked_exercise_equipment")

            val pickedExercise = if (pickedId != null && pickedName != null) {
                savedStateHandle.remove<String>("picked_exercise_id")
                savedStateHandle.remove<String>("picked_exercise_name")
                savedStateHandle.remove<String>("picked_exercise_muscle")
                savedStateHandle.remove<String>("picked_exercise_category")
                savedStateHandle.remove<String>("picked_exercise_equipment")
                Exercise(
                    id = java.util.UUID.fromString(pickedId),
                    name = pickedName,
                    muscleGroup = MuscleGroup.entries.find { it.name == pickedMuscle } ?: MuscleGroup.FULL_BODY,
                    category = ExerciseCategory.entries.find { it.name == pickedCategory } ?: ExerciseCategory.COMPOUND,
                    equipment = Equipment.entries.find { it.name == pickedEquipment } ?: Equipment.OTHER,
                )
            } else null

            ActiveWorkoutRoute(
                onNavigateToExercisePicker = {
                    navController.navigate("exercise_picker")
                },
                onNavigateToSummary = { duration, volume, sets, exercises, prs ->
                    navController.navigate(
                        "workout_summary/$duration/$volume/$sets/$exercises/$prs"
                    ) {
                        popUpTo("active_workout") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                pickedExercise = pickedExercise,
            )
        }
        composable("exercise_picker") {
            ExerciseLibraryRoute(
                onNavigateToDetail = { },
                onExercisePicked = { exercise ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_exercise_id", exercise.id.toString())
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_exercise_name", exercise.name)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_exercise_muscle", exercise.muscleGroup.name)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_exercise_category", exercise.category.name)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_exercise_equipment", exercise.equipment.name)
                    navController.popBackStack()
                },
                isPickerMode = true,
            )
        }
        composable(
            route = "workout_summary/{duration}/{volume}/{sets}/{exercises}/{prs}",
            arguments = listOf(
                navArgument("duration") { type = NavType.LongType },
                navArgument("volume") { type = NavType.FloatType },
                navArgument("sets") { type = NavType.IntType },
                navArgument("exercises") { type = NavType.IntType },
                navArgument("prs") { type = NavType.IntType },
            ),
        ) { backStackEntry ->
            val duration = backStackEntry.arguments?.getLong("duration") ?: 0L
            val volume = backStackEntry.arguments?.getFloat("volume")?.toDouble() ?: 0.0
            val sets = backStackEntry.arguments?.getInt("sets") ?: 0
            val exercises = backStackEntry.arguments?.getInt("exercises") ?: 0
            val prs = backStackEntry.arguments?.getInt("prs") ?: 0

            WorkoutSummaryScreen(
                durationSeconds = duration,
                totalVolume = volume,
                totalSets = sets,
                exerciseCount = exercises,
                prsCount = prs,
                onDone = {
                    navController.navigate("exercise_library") {
                        popUpTo("exercise_library") { inclusive = true }
                    }
                },
            )
        }
        composable("history") {
            PlaceholderScreen(title = "History")
        }
        composable("programs") {
            PlaceholderScreen(title = "Programs")
        }
        composable("coach") {
            PlaceholderScreen(title = "Coach")
        }
        composable("profile") {
            PlaceholderScreen(title = "Profile")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
