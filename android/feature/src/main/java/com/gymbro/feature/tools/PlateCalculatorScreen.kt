package com.gymbro.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.core.R
import com.gymbro.core.ui.theme.AccentAmberEnd
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.Background
import com.gymbro.core.ui.theme.GlassBorder
import com.gymbro.core.ui.theme.GlassOverlay
import com.gymbro.feature.common.GlassmorphicCard
import kotlin.math.abs

@Composable
fun PlateCalculatorRoute(
    onNavigateBack: () -> Unit = {},
) {
    PlateCalculatorScreen(
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PlateCalculatorScreen(
    onNavigateBack: () -> Unit,
) {
    var targetWeight by remember { mutableStateOf("") }
    var barWeight by remember { mutableStateOf("20") }
    var useKg by remember { mutableStateOf(true) }

    val plates by remember {
        derivedStateOf {
            calculatePlates(
                targetWeight = targetWeight.toDoubleOrNull() ?: 0.0,
                barWeight = barWeight.toDoubleOrNull() ?: 20.0,
                useKg = useKg
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tools_plate_calculator),
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
                        text = stringResource(R.string.tools_plate_target_weight),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetWeight,
                        onValueChange = { targetWeight = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("100${if (useKg) "kg" else "lbs"}")
                        },
                        suffix = {
                            Text(if (useKg) "kg" else "lbs")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreenStart,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.tools_plate_bar_weight),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = barWeight,
                        onValueChange = { barWeight = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("20kg / 45lbs")
                        },
                        suffix = {
                            Text(if (useKg) "kg" else "lbs")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            if (plates.isNotEmpty()) {
                GlassmorphicCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tools_plate_per_side),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreenStart,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Plate Representation
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            plates.forEach { plate ->
                                PlateVisual(
                                    weight = plate,
                                    unit = if (useKg) "kg" else "lbs",
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text Summary
                        Text(
                            text = plates.joinToString(" + ") { "$it${if (useKg) "kg" else "lbs"}" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                        )
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
                        colors = listOf(AccentGreenStart, AccentGreenEnd)
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
                color = if (isSelected) AccentGreenEnd else GlassBorder,
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
private fun PlateVisual(
    weight: Double,
    unit: String,
) {
    val color = getPlateColor(weight, unit == "kg")
    
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 60.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0.8f), color)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = color,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$weight\n$unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}

private fun getPlateColor(weight: Double, isKg: Boolean): Color {
    return if (isKg) {
        when (weight) {
            25.0 -> Color(0xFFE74C3C) // Red
            20.0 -> Color(0xFF3498DB) // Blue
            15.0 -> Color(0xFFF39C12) // Orange
            10.0 -> Color(0xFF2ECC71) // Green
            5.0 -> Color(0xFFFFFFFF) // White
            2.5 -> Color(0xFF95A5A6) // Gray
            1.25 -> Color(0xFF34495E) // Dark Gray
            else -> AccentGreenStart
        }
    } else {
        when (weight) {
            45.0 -> Color(0xFFE74C3C) // Red
            35.0 -> Color(0xFF3498DB) // Blue
            25.0 -> Color(0xFF2ECC71) // Green
            10.0 -> Color(0xFFF39C12) // Orange
            5.0 -> Color(0xFFFFFFFF) // White
            2.5 -> Color(0xFF95A5A6) // Gray
            else -> AccentGreenStart
        }
    }
}

private fun calculatePlates(
    targetWeight: Double,
    barWeight: Double,
    useKg: Boolean,
): List<Double> {
    if (targetWeight <= barWeight) return emptyList()

    val availablePlates = if (useKg) {
        listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    } else {
        listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    }

    val weightPerSide = (targetWeight - barWeight) / 2.0
    val plates = mutableListOf<Double>()
    var remaining = weightPerSide

    for (plate in availablePlates) {
        while (remaining >= plate - 0.01) { // Small tolerance for floating point
            plates.add(plate)
            remaining -= plate
        }
    }

    return plates
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
