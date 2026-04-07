package com.gymbro.feature.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.gymbro.core.error.UiError
import kotlinx.coroutines.flow.Flow

@Composable
fun ObserveErrors(
    errorFlow: Flow<UiError>,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(errorFlow) {
        errorFlow.collect { error ->
            val result = snackbarHostState.showSnackbar(
                message = error.message,
                actionLabel = if (error.retryable) "Retry" else null,
                duration = if (error.retryable) SnackbarDuration.Long else SnackbarDuration.Short
            )
            
            if (result == SnackbarResult.ActionPerformed) {
                error.action?.invoke()
            }
        }
    }
}
