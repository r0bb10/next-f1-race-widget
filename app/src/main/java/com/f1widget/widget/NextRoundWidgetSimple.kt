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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.f1widget.R
import com.f1widget.data.CalendarItem
import com.f1widget.data.F1Repository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NextRoundWidgetSimple : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = F1Repository()
        val nextRace = try {
            repository.getNextRace()
        } catch (e: Exception) {
            null
        }

        provideContent {
            GlanceTheme {
                NextRoundContent(nextRace)
            }
        }
    }

    @Composable
    private fun NextRoundContent(race: CalendarItem?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.widget_background))
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (race != null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Top row: Date box + Header box + Countdown
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Date box (left)
                        DateBox(race.session1)

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Header box with race info (center, expanding)
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(ColorProvider(R.color.widget_header_background))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                // Round number + location + flag
                                Text(
                                    text = "${extractRoundNumber(race.id)}•${extractLocation(race.roundTitle)} ${getCountryFlag(race.country)}",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(R.color.widget_text_primary)
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                // Full race name
                                Text(
                                    text = race.roundTitle.uppercase(),
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = ColorProvider(R.color.widget_text_secondary)
                                    ),
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Countdown timer (right)
                        CountdownBox(race.session1)
                    }

                    Spacer(modifier = GlanceModifier.height(16.dp))

                    // Sessions list
                    SessionsList(race)
                }
            } else {
                Text(
                    text = "Loading F1 schedule...",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(R.color.widget_text_secondary)
                    )
                )
            }
        }
    }

    @Composable
    private fun DateBox(sessionTime: String) {
        val (dayRange, month) = extractDateRange(sessionTime)
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(R.color.f1_white))
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayRange,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.f1_black)
                    )
                )
                Text(
                    text = month,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.f1_black)
                    )
                )
            }
        }
    }

    @Composable
    private fun CountdownBox(sessionTime: String) {
        val countdown = getCountdown(sessionTime)
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "NEXT SESSION",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_text_secondary)
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownUnit(countdown.days.toString().padStart(2, '0'), "DAYS")
                Text(text = " | ", style = TextStyle(color = ColorProvider(R.color.widget_text_secondary)))
                CountdownUnit(countdown.hours.toString().padStart(2, '0'), "HRS")
                Text(text = " | ", style = TextStyle(color = ColorProvider(R.color.widget_text_secondary)))
                CountdownUnit(countdown.minutes.toString().padStart(2, '0'), "MIN")
            }
        }
    }

    @Composable
    private fun CountdownUnit(value: String, label: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_text_primary)
                )
            )
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 8.sp,
                    color = ColorProvider(R.color.widget_text_secondary)
                )
            )
        }
    }

    @Composable
    private fun SessionsList(race: CalendarItem) {
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            // Session 1 (Practice 1 or FP1)
            SessionRow("PRACTICE 1", race.session1)

            // Session 2 (Practice 2 or Sprint Qualifying)
            if (race.session2.isNotEmpty()) {
                SessionRow(
                    if (race.weekendType == "sprint") "SPRINT QUALIFYING" else "PRACTICE 2",
                    race.session2
                )
            }

            // Session 3 (Practice 3 or Sprint)
            if (race.session3.isNotEmpty()) {
                SessionRow(
                    if (race.weekendType == "sprint") "SPRINT" else "PRACTICE 3",
                    race.session3
                )
            }

            // Red separator before qualifying
            Spacer(modifier = GlanceModifier.height(4.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(R.color.widget_separator))
            ) {}
            Spacer(modifier = GlanceModifier.height(4.dp))

            // Session 4 (Qualifying)
            SessionRow("QUALIFYING", race.session4)

            // Session 5 (Race)
            SessionRow("GRAND PRIX", race.session5, isRace = true)
        }
    }

    @Composable
    private fun SessionRow(label: String, time: String, isRace: Boolean = false) {
        if (time.isEmpty()) return

        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Session name
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = if (isRace) FontWeight.Bold else FontWeight.Bold,
                    color = ColorProvider(R.color.widget_text_primary)
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Time pill
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(R.color.widget_header_background))
                    .cornerRadius(12.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatSessionTime(time),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(
                            if (isRace) R.color.widget_text_primary
                            else R.color.widget_text_secondary
                        )
                    )
                )
            }
        }
    }

    private fun formatSessionTime(isoDateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("EEE HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            val date = inputFormat.parse(isoDateTime)
            date?.let { outputFormat.format(it).uppercase() } ?: isoDateTime
        } catch (e: Exception) {
            isoDateTime
        }
    }

    private fun extractDateRange(isoDateTime: String): Pair<String, String> {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(isoDateTime)
            val dayFormat = SimpleDateFormat("dd", Locale.US)
            val monthFormat = SimpleDateFormat("MMM", Locale.US)
            val day = date?.let { dayFormat.format(it) } ?: "??"
            val month = date?.let { monthFormat.format(it).uppercase() } ?: "???"
            // For now just show single day, could be enhanced to show range
            Pair(day, month)
        } catch (e: Exception) {
            Pair("??", "???")
        }
    }

    private fun extractRoundNumber(id: String): String {
        // Extract round number from ID (e.g., "round_01" -> "R1")
        val match = Regex("\\d+").find(id)
        return match?.let { "R${it.value.toInt()}" } ?: "R?"
    }

    private fun extractLocation(roundTitle: String): String {
        // Extract location from title (e.g., "Monaco Grand Prix" -> "Monaco")
        return roundTitle.replace(" Grand Prix", "")
            .replace(" GP", "")
            .trim()
            .split(" ")
            .last() // Take last word (usually the location)
    }

    private fun getCountdown(sessionTime: String): Countdown {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val sessionDate = format.parse(sessionTime)
            val now = Date()

            val diff = sessionDate.time - now.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

            Countdown(days.toInt().coerceAtLeast(0), hours.toInt().coerceAtLeast(0), minutes.toInt().coerceAtLeast(0))
        } catch (e: Exception) {
            Countdown(0, 0, 0)
        }
    }

    private fun getCountryFlag(country: String): String {
        val flagMap = mapOf(
            "Bahrain" to "🇧🇭",
            "Saudi Arabia" to "🇸🇦",
            "Australia" to "🇦🇺",
            "Japan" to "🇯🇵",
            "China" to "🇨🇳",
            "United States" to "🇺🇸",
            "Italy" to "🇮🇹",
            "Monaco" to "🇲🇨",
            "Canada" to "🇨🇦",
            "Spain" to "🇪🇸",
            "Austria" to "🇦🇹",
            "United Kingdom" to "🇬🇧",
            "Hungary" to "🇭🇺",
            "Belgium" to "🇧🇪",
            "Netherlands" to "🇳🇱",
            "Azerbaijan" to "🇦🇿",
            "Singapore" to "🇸🇬",
            "Mexico" to "🇲🇽",
            "Brazil" to "🇧🇷",
            "USA" to "🇺🇸",
            "Las Vegas" to "🇺🇸",
            "Qatar" to "🇶🇦",
            "Abu Dhabi" to "🇦🇪",
            "UAE" to "🇦🇪"
        )
        return flagMap[country] ?: ""
    }

    data class Countdown(val days: Int, val hours: Int, val minutes: Int)
}
