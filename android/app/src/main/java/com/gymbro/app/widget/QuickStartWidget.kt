package com.gymbro.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gymbro.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint

class QuickStartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickStartContent()
            }
        }
    }

    @Composable
    private fun QuickStartContent() {
        val daysSinceLastWorkout = currentState(DAYS_SINCE_KEY) ?: 0

        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(androidx.compose.ui.graphics.Color(0xFF141414))
                    .cornerRadius(16.dp)
                    .padding(16.dp)
                    .clickable(
                        onClick = actionStartActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                null,
                                null,
                                MainActivity::class.java
                            ).apply {
                                putExtra("destination", "active_workout")
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Title
                    Text(
                        text = "Start Workout",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(androidx.compose.ui.graphics.Color.White)
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Days since last workout
                    if (daysSinceLastWorkout > 0) {
                        Text(
                            text = "$daysSinceLastWorkout ${if (daysSinceLastWorkout == 1) "day" else "days"} since last workout",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9E9E9E))
                            )
                        )
                    }
                }
            }

            // Accent bar at bottom
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(androidx.compose.ui.graphics.Color(0xFF00FF87))
            ) {}
        }
    }

    companion object {
        val DAYS_SINCE_KEY = intPreferencesKey("days_since_last_workout")
    }
}

@AndroidEntryPoint
class QuickStartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickStartWidget()
}
