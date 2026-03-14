package com.f1widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.f1widget.data.CalendarItem
import com.f1widget.data.F1Repository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class F1Widget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = F1Repository()
        val nextRace = try {
            repository.getNextRace().first()
        } catch (e: Exception) {
            null
        }

        provideContent {
            GlanceTheme {
                F1WidgetContent(nextRace)
            }
        }
    }

    @Composable
    private fun F1WidgetContent(race: CalendarItem?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.glance.R.color.glance_colorBackground))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (race != null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NEXT RACE",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(androidx.glance.R.color.glance_colorOnBackground)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = race.roundTitle,
                        style = TextStyle(
                            fontSize = 18.sp,
                            color = ColorProvider(androidx.glance.R.color.glance_colorOnBackground)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = race.venueName,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(androidx.glance.R.color.glance_colorOnBackground)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))

                    // Display race time
                    Text(
                        text = "Race: ${formatDateTime(race.session5)}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(androidx.glance.R.color.glance_colorOnBackground)
                        )
                    )

                    // Display qualifying time
                    Text(
                        text = "Quali: ${formatDateTime(race.session4)}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(androidx.glance.R.color.glance_colorOnBackground)
                        )
                    )
                }
            } else {
                Text(
                    text = "Loading F1 data...",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(androidx.glance.R.color.glance_colorOnBackground)
                    )
                )
            }
        }
    }

    private fun formatDateTime(isoDateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("EEE, MMM d 'at' HH:mm", Locale.getDefault())
            val date = inputFormat.parse(isoDateTime)
            date?.let { outputFormat.format(it) } ?: isoDateTime
        } catch (e: Exception) {
            isoDateTime
        }
    }
}
