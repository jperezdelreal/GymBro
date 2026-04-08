package com.gymbro.feature.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.gymbro.core.model.MuscleGroup

object MuscleGroupIcons {
    
    val Chest: ImageVector
        get() = ImageVector.Builder(
            name = "Chest",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Pectoral silhouette - simplified chest outline
                moveTo(12f, 4f)
                // Left pec
                curveTo(9f, 4f, 7f, 6f, 6f, 8f)
                curveTo(5f, 10f, 4.5f, 12f, 5f, 14f)
                lineTo(12f, 12f)
                // Right pec
                lineTo(19f, 14f)
                curveTo(19.5f, 12f, 19f, 10f, 18f, 8f)
                curveTo(17f, 6f, 15f, 4f, 12f, 4f)
                close()
            }
        }.build()

    val Back: ImageVector
        get() = ImageVector.Builder(
            name = "Back",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Upper back / lat spread
                moveTo(12f, 5f)
                // Left lat
                curveTo(8f, 6f, 5f, 8f, 4f, 12f)
                curveTo(3.5f, 14f, 4f, 16f, 6f, 18f)
                lineTo(12f, 19f)
                // Right lat
                lineTo(18f, 18f)
                curveTo(20f, 16f, 20.5f, 14f, 20f, 12f)
                curveTo(19f, 8f, 16f, 6f, 12f, 5f)
                close()
                // Spine detail
                moveTo(12f, 8f)
                lineTo(12f, 19f)
            }
        }.build()

    val Quadriceps: ImageVector
        get() = ImageVector.Builder(
            name = "Quadriceps",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Front thigh silhouette
                moveTo(10f, 4f)
                curveTo(9f, 4f, 8f, 5f, 8f, 6f)
                lineTo(8f, 18f)
                curveTo(8f, 19f, 9f, 20f, 10f, 20f)
                lineTo(14f, 20f)
                curveTo(15f, 20f, 16f, 19f, 16f, 18f)
                lineTo(16f, 6f)
                curveTo(16f, 5f, 15f, 4f, 14f, 4f)
                close()
                // Muscle definition lines
                moveTo(10.5f, 8f)
                lineTo(10.5f, 16f)
                moveTo(12f, 6f)
                lineTo(12f, 18f)
                moveTo(13.5f, 8f)
                lineTo(13.5f, 16f)
            }
        }.build()

    val Hamstrings: ImageVector
        get() = ImageVector.Builder(
            name = "Hamstrings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Back of thigh
                moveTo(9f, 4f)
                curveTo(8f, 5f, 7f, 6f, 7f, 8f)
                lineTo(8f, 18f)
                curveTo(8f, 19f, 9f, 20f, 10f, 20f)
                lineTo(14f, 20f)
                curveTo(15f, 20f, 16f, 19f, 16f, 18f)
                lineTo(17f, 8f)
                curveTo(17f, 6f, 16f, 5f, 15f, 4f)
                close()
                // Hamstring curves
                moveTo(9.5f, 10f)
                curveTo(10f, 12f, 10f, 14f, 9.5f, 16f)
                moveTo(14.5f, 10f)
                curveTo(14f, 12f, 14f, 14f, 14.5f, 16f)
            }
        }.build()

    val Shoulders: ImageVector
        get() = ImageVector.Builder(
            name = "Shoulders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Left deltoid cap
                moveTo(4f, 10f)
                curveTo(4f, 7f, 6f, 5f, 8f, 5f)
                lineTo(10f, 5f)
                lineTo(11f, 9f)
                curveTo(11f, 11f, 10f, 13f, 8f, 14f)
                curveTo(6f, 13f, 4f, 12f, 4f, 10f)
                close()
                
                // Right deltoid cap
                moveTo(20f, 10f)
                curveTo(20f, 7f, 18f, 5f, 16f, 5f)
                lineTo(14f, 5f)
                lineTo(13f, 9f)
                curveTo(13f, 11f, 14f, 13f, 16f, 14f)
                curveTo(18f, 13f, 20f, 12f, 20f, 10f)
                close()
                
                // Center connection (neck area)
                moveTo(10f, 5f)
                lineTo(14f, 5f)
            }
        }.build()

    val Biceps: ImageVector
        get() = ImageVector.Builder(
            name = "Biceps",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Flexed arm silhouette
                moveTo(18f, 12f)
                curveTo(18f, 8f, 15f, 5f, 12f, 5f)
                curveTo(9f, 5f, 6f, 8f, 6f, 12f)
                lineTo(6f, 14f)
                curveTo(6f, 16f, 7f, 18f, 9f, 19f)
                curveTo(10f, 19.5f, 11f, 20f, 12f, 20f)
                curveTo(13f, 20f, 14f, 19.5f, 15f, 19f)
                curveTo(17f, 18f, 18f, 16f, 18f, 14f)
                close()
                // Bicep peak
                moveTo(12f, 8f)
                curveTo(14f, 8f, 15.5f, 9.5f, 15.5f, 11.5f)
                curveTo(15.5f, 13.5f, 14f, 15f, 12f, 15f)
                curveTo(10f, 15f, 8.5f, 13.5f, 8.5f, 11.5f)
                curveTo(8.5f, 9.5f, 10f, 8f, 12f, 8f)
                close()
            }
        }.build()

    val Triceps: ImageVector
        get() = ImageVector.Builder(
            name = "Triceps",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Extended arm back view
                moveTo(10f, 5f)
                lineTo(14f, 5f)
                curveTo(15f, 5f, 16f, 6f, 16f, 7f)
                lineTo(16f, 17f)
                curveTo(16f, 18f, 15f, 19f, 14f, 19f)
                lineTo(10f, 19f)
                curveTo(9f, 19f, 8f, 18f, 8f, 17f)
                lineTo(8f, 7f)
                curveTo(8f, 6f, 9f, 5f, 10f, 5f)
                close()
                // Three heads of tricep
                moveTo(10f, 8f)
                lineTo(10f, 16f)
                moveTo(12f, 6f)
                lineTo(12f, 18f)
                moveTo(14f, 8f)
                lineTo(14f, 16f)
            }
        }.build()

    val Core: ImageVector
        get() = ImageVector.Builder(
            name = "Core",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Torso outline
                moveTo(9f, 4f)
                lineTo(15f, 4f)
                curveTo(16f, 4f, 17f, 5f, 17f, 6f)
                lineTo(17f, 18f)
                curveTo(17f, 19f, 16f, 20f, 15f, 20f)
                lineTo(9f, 20f)
                curveTo(8f, 20f, 7f, 19f, 7f, 18f)
                lineTo(7f, 6f)
                curveTo(7f, 5f, 8f, 4f, 9f, 4f)
                close()
                // Ab lines (six-pack)
                moveTo(9f, 8f)
                lineTo(15f, 8f)
                moveTo(9f, 12f)
                lineTo(15f, 12f)
                moveTo(9f, 16f)
                lineTo(15f, 16f)
                moveTo(12f, 4f)
                lineTo(12f, 20f)
            }
        }.build()

    val Glutes: ImageVector
        get() = ImageVector.Builder(
            name = "Glutes",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Hip/glute area
                moveTo(6f, 8f)
                curveTo(6f, 6f, 8f, 4f, 12f, 4f)
                curveTo(16f, 4f, 18f, 6f, 18f, 8f)
                curveTo(18f, 10f, 17f, 12f, 15f, 14f)
                curveTo(14f, 15f, 13f, 16f, 12f, 16f)
                curveTo(11f, 16f, 10f, 15f, 9f, 14f)
                curveTo(7f, 12f, 6f, 10f, 6f, 8f)
                close()
                // Definition line
                moveTo(12f, 4f)
                lineTo(12f, 16f)
            }
        }.build()

    val Calves: ImageVector
        get() = ImageVector.Builder(
            name = "Calves",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Lower leg shape
                moveTo(10f, 4f)
                lineTo(14f, 4f)
                lineTo(15f, 10f)
                curveTo(15.5f, 12f, 15f, 14f, 14f, 16f)
                lineTo(14f, 20f)
                lineTo(10f, 20f)
                lineTo(10f, 16f)
                curveTo(9f, 14f, 8.5f, 12f, 9f, 10f)
                close()
                // Calf muscle bulge
                moveTo(9.5f, 10f)
                curveTo(10f, 11f, 10.5f, 12f, 10.5f, 13f)
                moveTo(14.5f, 10f)
                curveTo(14f, 11f, 13.5f, 12f, 13.5f, 13f)
            }
        }.build()

    val Forearms: ImageVector
        get() = ImageVector.Builder(
            name = "Forearms",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Forearm shape
                moveTo(11f, 4f)
                lineTo(13f, 4f)
                curveTo(14f, 4f, 15f, 5f, 15f, 6f)
                lineTo(15f, 14f)
                curveTo(15f, 15f, 14.5f, 16f, 14f, 17f)
                lineTo(14f, 20f)
                lineTo(10f, 20f)
                lineTo(10f, 17f)
                curveTo(9.5f, 16f, 9f, 15f, 9f, 14f)
                lineTo(9f, 6f)
                curveTo(9f, 5f, 10f, 4f, 11f, 4f)
                close()
                // Muscle striations
                moveTo(10.5f, 8f)
                lineTo(13.5f, 10f)
                moveTo(10.5f, 11f)
                lineTo(13.5f, 13f)
            }
        }.build()

    val FullBody: ImageVector
        get() = ImageVector.Builder(
            name = "FullBody",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Full person silhouette
                // Head
                moveTo(12f, 3f)
                curveTo(13f, 3f, 14f, 4f, 14f, 5f)
                curveTo(14f, 6f, 13f, 7f, 12f, 7f)
                curveTo(11f, 7f, 10f, 6f, 10f, 5f)
                curveTo(10f, 4f, 11f, 3f, 12f, 3f)
                close()
                
                // Torso
                moveTo(12f, 7f)
                lineTo(12f, 14f)
                
                // Arms
                moveTo(12f, 9f)
                lineTo(8f, 12f)
                moveTo(12f, 9f)
                lineTo(16f, 12f)
                
                // Legs
                moveTo(12f, 14f)
                lineTo(10f, 21f)
                moveTo(12f, 14f)
                lineTo(14f, 21f)
            }
        }.build()
}

fun MuscleGroup.icon(): ImageVector = when (this) {
    MuscleGroup.CHEST -> MuscleGroupIcons.Chest
    MuscleGroup.BACK -> MuscleGroupIcons.Back
    MuscleGroup.QUADRICEPS -> MuscleGroupIcons.Quadriceps
    MuscleGroup.HAMSTRINGS -> MuscleGroupIcons.Hamstrings
    MuscleGroup.SHOULDERS -> MuscleGroupIcons.Shoulders
    MuscleGroup.BICEPS -> MuscleGroupIcons.Biceps
    MuscleGroup.TRICEPS -> MuscleGroupIcons.Triceps
    MuscleGroup.CORE -> MuscleGroupIcons.Core
    MuscleGroup.GLUTES -> MuscleGroupIcons.Glutes
    MuscleGroup.CALVES -> MuscleGroupIcons.Calves
    MuscleGroup.FOREARMS -> MuscleGroupIcons.Forearms
    MuscleGroup.FULL_BODY -> MuscleGroupIcons.FullBody
}
