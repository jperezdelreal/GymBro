package com.gymbro.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
import com.gymbro.feature.exerciselibrary.ExerciseDetailRoute
import com.gymbro.feature.exerciselibrary.ExerciseLibraryRoute
import com.gymbro.feature.home.HomeRoute
import com.gymbro.feature.history.HistoryDetailRoute
import com.gymbro.feature.history.HistoryListRoute
import com.gymbro.feature.onboarding.OnboardingRoute
import com.gymbro.feature.profile.ProfileRoute
import com.gymbro.feature.programs.PlanDayDetailRoute
import com.gymbro.feature.programs.ProgramsRoute
import com.gymbro.feature.progress.ProgressRoute
import com.gymbro.feature.analytics.AnalyticsRoute
import com.gymbro.feature.coach.CoachChatRoute
import com.gymbro.feature.recovery.RecoveryRoute
import com.gymbro.feature.settings.SettingsRoute
import com.gymbro.feature.workout.ActiveWorkoutRoute
import com.gymbro.feature.workout.SmartWorkoutRoute
import com.gymbro.feature.workout.WorkoutSummaryScreen
import com.gymbro.core.model.PersonalRecord
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.service.WorkoutResultStore

private val AccentGreen = Color(0xFF00FF87)

private enum class BottomNavTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    PROGRAMS("programs", R.string.nav_programs, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    HISTORY("history", R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
}

@Composable
fun GymBroNavGraph(
    userPreferences: UserPreferences = hiltViewModel<GymBroNavGraphViewModel>().userPreferences,
    workoutResultStore: WorkoutResultStore = hiltViewModel<GymBroNavGraphViewModel>().workoutResultStore,
    onFullyDrawn: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in BottomNavTab.entries.map { it.route }

    // Use null initial value to distinguish "not yet loaded" from "false"
    val hasCompletedOnboarding by userPreferences.hasCompletedOnboarding.collectAsStateWithLifecycle(initialValue = null)

    // Wait for DataStore to load before deciding start destination — avoids flashing wrong screen
    val resolvedOnboarding = hasCompletedOnboarding
    if (resolvedOnboarding == null) {
        // Show nothing while preferences load — splash screen covers this gap
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val startDestination = if (resolvedOnboarding) "home" else "onboarding"

    // Report fully drawn once we know which screen to show
    LaunchedEffect(Unit) {
        onFullyDrawn()
    }

    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
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
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = { navController.navigate("active_workout") },
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.testTag("workout_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.workout_start), modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
        ) {
        composable("onboarding") {
            OnboardingRoute(
                onNavigateToMain = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }
        composable("home") {
            HomeRoute(
                onNavigateToActiveWorkout = {
                    navController.navigate("active_workout")
                },
                onNavigateToPrograms = {
                    navController.navigate("programs") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToWorkoutDetail = { workoutId ->
                    navController.navigate("history/$workoutId")
                },
            )
        }
        composable("exercise_library") {
            ExerciseLibraryRoute(
                onNavigateToDetail = { exerciseId ->
                    navController.navigate("exercise_detail/$exerciseId")
                },
                onNavigateToCreateExercise = {
                    navController.navigate("create_exercise")
                },
            )
        }
        composable(
            route = "exercise_detail/{exerciseId}",
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.StringType },
            ),
        ) {
            ExerciseDetailRoute(
                onNavigateBack = { navController.popBackStack() },
            )
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
                    workoutResultStore.setPersonalRecords(prs)
                    navController.navigate(
                        "workout_summary/$duration/$volume/$sets/$exercises"
                    ) {
                        popUpTo("active_workout") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCoach = {
                    navController.navigate("coach")
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
            val prs = remember { workoutResultStore.consumePersonalRecords() }
            val weightUnit by userPreferences.weightUnit.collectAsStateWithLifecycle(initialValue = UserPreferences.WeightUnit.KG)
            val weightUnitLabel = if (weightUnit == UserPreferences.WeightUnit.LBS) "lb" else "kg"

            WorkoutSummaryScreen(
                durationSeconds = duration,
                totalVolume = volume,
                totalSets = sets,
                exerciseCount = exercises,
                personalRecords = prs,
                weightUnitLabel = weightUnitLabel,
                onDone = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
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
            HistoryListRoute(
                onNavigateBack = { },
                onNavigateToDetail = { workoutId ->
                    navController.navigate("history/$workoutId")
                },
                onNavigateToActiveWorkout = {
                    navController.navigate("active_workout")
                },
            )
        }
        composable("progress") {
            ProgressRoute(
                onNavigateToAnalytics = { navController.navigate("analytics") },
                onNavigateToCoach = { prompt ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("coach_initial_prompt", prompt)
                    navController.navigate("coach")
                },
                onNavigateToActiveWorkout = {
                    navController.navigate("active_workout")
                }
            )
        }
        composable("analytics") {
            AnalyticsRoute(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable("recovery") {
            RecoveryRoute()
        }
        composable("programs") {
            ProgramsRoute(
                onNavigateToCreateTemplate = { /* Template editing not yet implemented */ },
                onNavigateToActiveWorkout = { template ->
                    navController.navigate("active_workout")
                },
                onNavigateToPlanDayDetail = { dayNumber ->
                    navController.navigate("programs/day/$dayNumber")
                },
            )
        }
        composable(
            route = "programs/day/{dayNumber}",
            arguments = listOf(
                navArgument("dayNumber") { type = NavType.IntType },
            ),
        ) { backStackEntry ->
            val dayNumber = backStackEntry.arguments?.getInt("dayNumber") ?: 1
            PlanDayDetailRoute(
                dayNumber = dayNumber,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToActiveWorkout = { navController.navigate("active_workout") },
            )
        }
        composable("coach") { backStackEntry ->
            val initialPrompt = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("coach_initial_prompt")
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.remove<String>("coach_initial_prompt")
            
            CoachChatRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                initialPrompt = initialPrompt,
            )
        }
        composable("profile") {
            ProfileRoute(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToCoach = {
                    navController.navigate("coach")
                },
                onNavigateToExerciseLibrary = {
                    navController.navigate("exercise_library")
                },
            )
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
}

@Composable
private fun GymBroBottomNavBar(
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    val selectedIndex = BottomNavTab.entries.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
    val indicatorOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "bottomNavIndicator"
    )
    
    androidx.compose.foundation.layout.Box {
        NavigationBar(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White,
        ) {
            BottomNavTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                val label = stringResource(tab.labelResId)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.testTag("nav_${tab.route}"),
                    icon = {
                        Icon(
                            if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = label,
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentGreen,
                        selectedTextColor = AccentGreen,
                        unselectedIconColor = Color(0xFF9E9E9E),
                        unselectedTextColor = Color(0xFF9E9E9E),
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
        
        // Animated indicator bar at the top of the bottom nav
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopStart)
        ) {
            val tabWidth = size.width / BottomNavTab.entries.size
            val indicatorWidth = tabWidth * 0.5f
            val indicatorHeight = 3.dp.toPx()
            val xOffset = (indicatorOffset * tabWidth) + (tabWidth - indicatorWidth) / 2
            
            drawRect(
                color = AccentGreen,
                topLeft = androidx.compose.ui.geometry.Offset(xOffset, 0f),
                size = androidx.compose.ui.geometry.Size(indicatorWidth, indicatorHeight)
            )
        }
    }
}
