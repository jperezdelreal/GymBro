package com.gymbro.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.feature.exerciselibrary.CreateExerciseRoute
import com.gymbro.feature.exerciselibrary.ExerciseLibraryRoute
import com.gymbro.feature.history.HistoryDetailRoute
import com.gymbro.feature.history.HistoryListRoute
import com.gymbro.feature.onboarding.OnboardingRoute
import com.gymbro.feature.profile.ProfileRoute
import com.gymbro.feature.programs.ProgramsRoute
import com.gymbro.feature.progress.ProgressRoute
import com.gymbro.feature.analytics.AnalyticsRoute
import com.gymbro.feature.recovery.RecoveryRoute
import com.gymbro.feature.settings.SettingsRoute
import com.gymbro.feature.workout.ActiveWorkoutRoute
import com.gymbro.feature.workout.SmartWorkoutRoute
import com.gymbro.feature.workout.WorkoutSummaryScreen
import com.gymbro.core.model.PersonalRecord
import androidx.hilt.navigation.compose.hiltViewModel

private val AccentGreen = Color(0xFF00FF87)

private enum class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LIBRARY("exercise_library", "Library", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    PROGRESS("progress", "Progress", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Outlined.ShowChart),
    RECOVERY("recovery", "Recovery", Icons.Filled.MonitorHeart, Icons.Outlined.MonitorHeart),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
}

@Composable
fun GymBroNavGraph(
    userPreferences: UserPreferences = hiltViewModel<GymBroNavGraphViewModel>().userPreferences,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in BottomNavTab.entries.map { it.route }

    // TODO: Implement onboarding completion tracking
    val startDestination = "exercise_library"

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable("onboarding") {
            OnboardingRoute(
                onNavigateToMain = {
                    navController.navigate("exercise_library") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }
        composable("exercise_library") {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        GymBroBottomNavBar(
                            currentRoute = currentDestination?.route,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
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
                        onNavigateToCreateExercise = {
                            navController.navigate("create_exercise")
                        },
                    )
                }
            }
        }
        composable("exercise_detail/{exerciseId}") {
            PlaceholderScreen(title = "Exercise Detail")
        }
        composable("create_exercise") {
            CreateExerciseRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
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
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("summary_prs", prs)
                    navController.navigate(
                        "workout_summary/$duration/$volume/$sets/$exercises"
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
        composable("smart_workout") {
            SmartWorkoutRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartWorkout = { exercises ->
                    navController.navigate("active_workout")
                    exercises.forEach { exercise ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("smart_workout_exercise_${exercise.id}", exercise)
                    }
                },
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
            route = "workout_summary/{duration}/{volume}/{sets}/{exercises}",
            arguments = listOf(
                navArgument("duration") { type = NavType.LongType },
                navArgument("volume") { type = NavType.FloatType },
                navArgument("sets") { type = NavType.IntType },
                navArgument("exercises") { type = NavType.IntType },
            ),
        ) { backStackEntry ->
            val duration = backStackEntry.arguments?.getLong("duration") ?: 0L
            val volume = backStackEntry.arguments?.getFloat("volume")?.toDouble() ?: 0.0
            val sets = backStackEntry.arguments?.getInt("sets") ?: 0
            val exercises = backStackEntry.arguments?.getInt("exercises") ?: 0
            val prs = navController.previousBackStackEntry?.savedStateHandle?.get<List<PersonalRecord>>("summary_prs") ?: emptyList()

            WorkoutSummaryScreen(
                durationSeconds = duration,
                totalVolume = volume,
                totalSets = sets,
                exerciseCount = exercises,
                personalRecords = prs,
                onDone = {
                    navController.navigate("exercise_library") {
                        popUpTo("exercise_library") { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = "history/{workoutId}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
            HistoryDetailRoute(
                workoutId = workoutId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("history") {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        GymBroBottomNavBar(
                            currentRoute = currentDestination?.route,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    HistoryListRoute(
                        onNavigateBack = { },
                        onNavigateToDetail = { workoutId ->
                            navController.navigate("history/$workoutId")
                        },
                    )
                }
            }
        }
        composable("progress") {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        GymBroBottomNavBar(
                            currentRoute = currentDestination?.route,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ProgressRoute(
                        onNavigateToAnalytics = { navController.navigate("analytics") }
                    )
                }
            }
        }
        composable("analytics") {
            AnalyticsRoute(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable("recovery") {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        GymBroBottomNavBar(
                            currentRoute = currentDestination?.route,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    RecoveryRoute()
                }
            }
        }
        composable("programs") {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        GymBroBottomNavBar(
                            currentRoute = currentDestination?.route,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ProgramsRoute(
                        onNavigateToCreateTemplate = { templateId ->
                            // TODO: Navigate to create/edit template screen
                        },
                        onNavigateToActiveWorkout = { template ->
                            navController.navigate("active_workout")
                        },
                    )
                }
            }
        }
        composable("coach") {
            PlaceholderScreen(title = "Coach")
        }
        composable("profile") {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        GymBroBottomNavBar(
                            currentRoute = currentDestination?.route,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ProfileRoute(
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                    )
                }
            }
        }
        composable("settings") {
            SettingsRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
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

@Composable
private fun GymBroBottomNavBar(
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    NavigationBar(
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White,
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentGreen,
                    selectedTextColor = AccentGreen,
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = AccentGreen.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
