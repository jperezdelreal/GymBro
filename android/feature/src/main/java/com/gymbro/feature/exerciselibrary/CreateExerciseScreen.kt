package com.gymbro.feature.exerciselibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.ObserveErrors

private val AccentGreen = Color(0xFF00FF87)

@Composable
fun CreateExerciseRoute(
    viewModel: CreateExerciseViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(
        errorFlow = viewModel.errorEvents,
        snackbarHostState = snackbarHostState
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateExerciseEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    CreateExerciseScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateExerciseScreen(
    state: CreateExerciseState,
    onEvent: (CreateExerciseEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Exercise",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CreateExerciseEvent.CancelClicked) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (state.isLoading) {
            FullScreenLoading(message = "Creating exercise...")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Exercise Name
                    Column {
                        Text(
                            text = "Exercise Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.exerciseName,
                            onValueChange = { onEvent(CreateExerciseEvent.NameChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "e.g. Dumbbell Chest Press",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            isError = state.nameError != null,
                            supportingText = state.nameError?.let { { Text(it) } },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = AccentGreen,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                errorBorderColor = Color(0xFFCF6679),
                            ),
                        )
                    }

                    // Muscle Group
                    Column {
                        Text(
                            text = "Muscle Group",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MuscleGroup.entries.forEach { group ->
                                val isSelected = state.selectedMuscleGroup == group
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onEvent(CreateExerciseEvent.MuscleGroupSelected(group))
                                    },
                                    label = { Text(group.displayName, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = AccentGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = AccentGreen,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.surfaceVariant,
                                        selectedBorderColor = AccentGreen.copy(alpha = 0.5f),
                                        enabled = true,
                                        selected = isSelected,
                                    ),
                                )
                            }
                        }
                    }

                    // Category
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExerciseCategory.entries.forEach { category ->
                                val isSelected = state.selectedCategory == category
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onEvent(CreateExerciseEvent.CategorySelected(category))
                                    },
                                    label = { Text(category.displayName, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = AccentGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = AccentGreen,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.surfaceVariant,
                                        selectedBorderColor = AccentGreen.copy(alpha = 0.5f),
                                        enabled = true,
                                        selected = isSelected,
                                    ),
                                )
                            }
                        }
                    }

                    // Equipment
                    Column {
                        Text(
                            text = "Equipment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Equipment.entries.forEach { equipment ->
                                val isSelected = state.selectedEquipment == equipment
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onEvent(CreateExerciseEvent.EquipmentSelected(equipment))
                                    },
                                    label = {
                                        Text(
                                            equipment.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontSize = 13.sp,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = AccentGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = AccentGreen,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.surfaceVariant,
                                        selectedBorderColor = AccentGreen.copy(alpha = 0.5f),
                                        enabled = true,
                                        selected = isSelected,
                                    ),
                                )
                            }
                        }
                    }

                    // Description (Optional)
                    Column {
                        Text(
                            text = "Description (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { onEvent(CreateExerciseEvent.DescriptionChanged(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = {
                                Text(
                                    "Add notes about form, cues, or variations...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = AccentGreen,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { onEvent(CreateExerciseEvent.CancelClicked) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onEvent(CreateExerciseEvent.SaveClicked) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = Color.Black,
                        ),
                        enabled = state.exerciseName.isNotBlank(),
                    ) {
                        Text("Create Exercise", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
