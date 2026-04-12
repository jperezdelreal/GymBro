package com.gymbro.feature.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.model.WorkoutTemplateLibrary
import com.gymbro.core.ui.theme.AccentGreen

@Composable
fun TemplateLibraryRoute(
    viewModel: TemplateLibraryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToTemplateDetail: (WorkoutTemplateLibrary.CuratedTemplate) -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TemplateLibraryEffect.NavigateToTemplateDetail -> {
                    onNavigateToTemplateDetail(effect.template)
                }
            }
        }
    }

    TemplateLibraryScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplateLibraryScreen(
    state: TemplateLibraryState,
    onEvent: (TemplateLibraryEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.template_library_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Filter chips
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedFilter == null,
                        onClick = { onEvent(TemplateLibraryEvent.FilterChanged(null)) },
                        label = { Text(stringResource(R.string.template_filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGreen,
                        )
                    )
                    FilterChip(
                        selected = state.selectedFilter == WorkoutTemplateLibrary.TargetAudience.BEGINNER,
                        onClick = { onEvent(TemplateLibraryEvent.FilterChanged(WorkoutTemplateLibrary.TargetAudience.BEGINNER)) },
                        label = { Text(stringResource(R.string.template_filter_beginner)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGreen,
                        )
                    )
                    FilterChip(
                        selected = state.selectedFilter == WorkoutTemplateLibrary.TargetAudience.INTERMEDIATE,
                        onClick = { onEvent(TemplateLibraryEvent.FilterChanged(WorkoutTemplateLibrary.TargetAudience.INTERMEDIATE)) },
                        label = { Text(stringResource(R.string.template_filter_intermediate)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGreen,
                        )
                    )
                    FilterChip(
                        selected = state.selectedFilter == WorkoutTemplateLibrary.TargetAudience.ADVANCED,
                        onClick = { onEvent(TemplateLibraryEvent.FilterChanged(WorkoutTemplateLibrary.TargetAudience.ADVANCED)) },
                        label = { Text(stringResource(R.string.template_filter_advanced)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGreen,
                        )
                    )
                }
            }

            // Template cards
            items(state.filteredTemplates) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onEvent(TemplateLibraryEvent.TemplateClicked(template)) }
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplateLibrary.CuratedTemplate,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    label = stringResource(R.string.programs_days_per_week, template.daysPerWeek),
                )
                InfoChip(
                    label = when (template.targetAudience) {
                        WorkoutTemplateLibrary.TargetAudience.BEGINNER -> 
                            stringResource(R.string.template_filter_beginner)
                        WorkoutTemplateLibrary.TargetAudience.INTERMEDIATE -> 
                            stringResource(R.string.template_filter_intermediate)
                        WorkoutTemplateLibrary.TargetAudience.ADVANCED -> 
                            stringResource(R.string.template_filter_advanced)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun InfoChip(label: String) {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .then(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AccentGreen
        )
    }
}
