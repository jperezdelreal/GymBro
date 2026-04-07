package com.gymbro.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gymbro.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint

class StatsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                StatsContent()
            }
        }
    }

    @Composable
    private fun StatsContent() {
        val workoutsThisWeek = currentState(WORKOUTS_WEEK_KEY) ?: 0
        val totalVolume = currentState(TOTAL_VOLUME_KEY) ?: 0.0
        val streakCount = currentState(STREAK_KEY) ?: 0

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
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
                            putExtra("destination", "history")
                        }
                    )
                )
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header
                Text(
                    text = "Weekly Stats",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(androidx.compose.ui.graphics.Color.White)
                    )
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Stats Grid
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    StatItem(
                        label = "Workouts",
                        value = "$workoutsThisWeek",
                        accentColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF00FF87)),
                        modifier = GlanceModifier.defaultWeight()
                    )

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    StatItem(
                        label = "Volume (kg)",
                        value = "${totalVolume.toInt()}",
                        accentColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF00D4FF)),
                        modifier = GlanceModifier.defaultWeight()
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Streak
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(
                        label = "Streak",
                        value = "$streakCount days",
                        accentColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFB800)),
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun StatItem(
        label: String,
        value: String,
        accentColor: ColorProvider,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Column(
            modifier = modifier
                .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E))
                .cornerRadius(12.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9E9E9E))
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = value,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            )
        }
    }

    companion object {
        val WORKOUTS_WEEK_KEY = intPreferencesKey("workouts_this_week")
        val TOTAL_VOLUME_KEY = doublePreferencesKey("total_volume")
        val STREAK_KEY = intPreferencesKey("streak_count")
    }
}

@AndroidEntryPoint
class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}
