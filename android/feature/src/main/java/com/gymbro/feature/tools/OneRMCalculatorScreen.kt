package com.gymbro.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.core.R
import com.gymbro.core.ui.theme.AccentAmberEnd
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentCyanEnd
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.Background
import com.gymbro.core.ui.theme.GlassBorder
import com.gymbro.core.ui.theme.GlassOverlay
import com.gymbro.feature.common.GlassmorphicCard
import kotlin.math.roundToInt

@Composable
fun OneRMCalculatorRoute(
    onNavigateBack: () -> Unit = {},
) {
    OneRMCalculatorScreen(
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OneRMCalculatorScreen(
    onNavigateBack: () -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var useKg by remember { mutableStateOf(true) }

    val oneRM by remember {
        derivedStateOf {
            calculateOneRM(
                weight = weight.toDoubleOrNull() ?: 0.0,
                reps = reps.toIntOrNull() ?: 0
            )
        }
    }

    val percentages = listOf(100, 95, 90, 85, 80, 75, 70, 65, 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tools_one_rm_calculator),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = Color.White,
                ),
            )
        },
        containerColor = Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Unit Toggle
            GlassmorphicCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    UnitButton(
                        text = "KG",
                        isSelected = useKg,
                        onClick = { useKg = true },
                    )
                    UnitButton(
                        text = "LBS",
                        isSelected = !useKg,
                        onClick = { useKg = false },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Section
            GlassmorphicCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tools_one_rm_weight_lifted),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("100${if (useKg) "kg" else "lbs"}")
                        },
                        suffix = {
                            Text(if (useKg) "kg" else "lbs")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyanStart,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.tools_one_rm_reps_performed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("5")
                        },
                        suffix = {
                            Text(stringResource(R.string.tools_one_rm_reps))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentAmberStart,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results Section
            if (oneRM > 0) {
                // 1RM Result
                GlassmorphicCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.tools_one_rm_estimated),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${String.format("%.1f", oneRM)}${if (useKg) "kg" else "lbs"}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreenStart,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Percentage Chart
                GlassmorphicCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tools_one_rm_percentage_chart),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        percentages.forEach { percentage ->
                            PercentageRow(
                                percentage = percentage,
                                weight = oneRM * percentage / 100.0,
                                unit = if (useKg) "kg" else "lbs",
                            )
                            if (percentage != percentages.last()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UnitButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(48.dp)
            .background(
                brush = if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(AccentCyanStart, AccentCyanEnd)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(GlassOverlay, GlassOverlay)
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentCyanEnd else GlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
            .then(
                if (!isSelected) {
                    Modifier.clickableWithoutRipple(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else Color.White,
        )
    }
}

@Composable
private fun PercentageRow(
    percentage: Int,
    weight: Double,
    unit: String,
) {
    val color = getPercentageColor(percentage)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = "${String.format("%.1f", weight)} $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

private fun getPercentageColor(percentage: Int): Color {
    return when {
        percentage >= 95 -> Color(0xFFE74C3C) // Red - max intensity
        percentage >= 85 -> Color(0xFFF39C12) // Orange - high intensity
        percentage >= 75 -> Color(0xFFF1C40F) // Yellow - moderate-high
        percentage >= 65 -> AccentCyanStart // Cyan - moderate
        else -> Color(0xFF95A5A6) // Gray - light work
    }
}

private fun calculateOneRM(weight: Double, reps: Int): Double {
    if (weight <= 0 || reps <= 0) return 0.0
    if (reps == 1) return weight
    if (reps >= 37) return 0.0

    // Brzycki formula: 1RM = weight × (36 / (37 - reps))
    return weight * (36.0 / (37 - reps))
}

@Composable
private fun Modifier.clickableWithoutRipple(
    onClick: () -> Unit,
): Modifier {
    return clickable(
        onClick = onClick,
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    )
}
