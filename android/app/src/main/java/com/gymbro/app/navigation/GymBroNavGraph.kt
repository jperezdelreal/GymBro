package com.gymbro.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun GymBroNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "workout",
    ) {
        composable("workout") {
            PlaceholderScreen(title = "Workout")
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
