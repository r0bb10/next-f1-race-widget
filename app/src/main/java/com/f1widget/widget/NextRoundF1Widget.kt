package com.f1widget.widget

import android.annotation.SuppressLint
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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.f1widget.R
import com.f1widget.data.F1Repository
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NextRoundF1WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextRoundF1Widget()
}

class NextRoundF1Widget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = F1Repository()
        val nextRace = try {
            repository.getNextRace()
        } catch (e: Exception) {
            android.util.Log.e("NextRoundF1Widget", "Error fetching race", e)
            null
        }

        provideContent {
            GlanceTheme {
                WidgetContent(nextRace)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun WidgetContent(race: com.f1widget.data.CalendarItem?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.widget_background))
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (race != null) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        DateBox(race)
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        InfoBox(race)
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        CountdownBox(race)
                    }
                    Spacer(modifier = GlanceModifier.height(16.dp))
                    SessionsList(race)
                }
            } else {
                Text(
                    text = "Loading F1 schedule...",
                    style = TextStyle(fontSize = 14.sp, color = ColorProvider(R.color.widget_text_secondary))
                )
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun DateBox(race: com.f1widget.data.CalendarItem) {
        val (dayRange, month) = extractDateRange(race.session1, race.session5)
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(R.color.f1_white))
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = dayRange, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.f1_black)))
                Text(text = month, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.f1_black)))
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun InfoBox(race: com.f1widget.data.CalendarItem) {
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .background(ColorProvider(R.color.widget_header_background))
                .cornerRadius(8.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = "${extractRoundNumber(race.id)}•${extractLocation(race)} ${getCountryFlag(race.country)}",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.widget_text_primary))
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = race.roundTitle.uppercase(),
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(R.color.widget_text_secondary)),
                    maxLines = 2
                )
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun CountdownBox(race: com.f1widget.data.CalendarItem) {
        val nextSessionTime = findNextSession(race)
        val countdown = getCountdown(nextSessionTime)
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "NEXT SESSION", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.widget_text_secondary)))
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(horizontalAlignment = Alignment.End, verticalAlignment = Alignment.CenterVertically) {
                CountdownUnit(countdown.days.toString().padStart(2, '0'), "DAYS")
                Text(text = " | ", style = TextStyle(color = ColorProvider(R.color.widget_text_secondary)))
                CountdownUnit(countdown.hours.toString().padStart(2, '0'), "HRS")
                Text(text = " | ", style = TextStyle(color = ColorProvider(R.color.widget_text_secondary)))
                CountdownUnit(countdown.minutes.toString().padStart(2, '0'), "MIN")
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun CountdownUnit(value: String, label: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.widget_text_primary)))
            Text(text = label, style = TextStyle(fontSize = 8.sp, color = ColorProvider(R.color.widget_text_secondary)))
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun SessionsList(race: com.f1widget.data.CalendarItem) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            SessionRow("PRACTICE 1", race.session1)
            if (race.session2.isNotEmpty()) SessionRow(if (race.weekendType == "sprint") "SPRINT QUALIFYING" else "PRACTICE 2", race.session2)
            if (race.session3.isNotEmpty()) SessionRow(if (race.weekendType == "sprint") "SPRINT" else "PRACTICE 3", race.session3)
            Spacer(modifier = GlanceModifier.height(4.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(R.color.widget_separator))) {}
            Spacer(modifier = GlanceModifier.height(4.dp))
            SessionRow("QUALIFYING", race.session4)
            SessionRow("GRAND PRIX", race.session5, isRace = true)
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun SessionRow(label: String, time: String, isRace: Boolean = false) {
        if (time.isEmpty()) return
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.Start, verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.widget_text_primary)), modifier = GlanceModifier.defaultWeight())
            Spacer(modifier = GlanceModifier.width(8.dp))
            Box(modifier = GlanceModifier.background(ColorProvider(R.color.widget_header_background)).cornerRadius(12.dp).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(text = formatSessionTime(time), style = TextStyle(fontSize = 11.sp, color = ColorProvider(if (isRace) R.color.widget_text_primary else R.color.widget_text_secondary)))
            }
        }
    }

    private fun formatSessionTime(isoDateTime: String): String {
        return try {
            val date = parseIsoDate(isoDateTime)
            // Format: "FRIDAY 18:00" to match Racify design
            val outputFormat = SimpleDateFormat("EEEE HH:mm", Locale.ENGLISH).apply { timeZone = TimeZone.getDefault() }
            outputFormat.format(date).uppercase()
        } catch (_: Exception) { isoDateTime }
    }

    private fun extractDateRange(startIso: String, endIso: String): Pair<String, String> {
        return try {
            val startDate = parseIsoDate(startIso)
            val endDate = parseIsoDate(endIso)
            val dayFormat = SimpleDateFormat("dd", Locale.US)
            val monthFormat = SimpleDateFormat("MMM", Locale.US)
            Pair("${dayFormat.format(startDate)}-${dayFormat.format(endDate)}", monthFormat.format(endDate).uppercase())
        } catch (_: Exception) { Pair("??-??", "???") }
    }

    private fun extractRoundNumber(id: String): String {
        val match = Regex("\\d+").find(id)
        return match?.let { "R${it.value.toInt()}" } ?: "R?"
    }

    private fun extractLocation(race: com.f1widget.data.CalendarItem): String {
        // Use track name if available, otherwise use country
        return if (race.track.isNotEmpty() && race.track != race.country) {
            race.track
        } else {
            race.country
        }
    }

    private fun parseIsoDate(isoDate: String): Date {
        if (isoDate.isEmpty()) return Date(0)
        val cleanDate = isoDate.split("+")[0].split("Z")[0]
        val pattern = if (cleanDate.contains(".")) "yyyy-MM-dd'T'HH:mm:ss.SSS" else "yyyy-MM-dd'T'HH:mm:ss"
        val format = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return format.parse(cleanDate) ?: Date(0)
    }

    private fun findNextSession(race: com.f1widget.data.CalendarItem): String {
        val now = Date()
        return listOf(race.session1, race.session2, race.session3, race.session4, race.session5)
            .filter { it.isNotEmpty() }
            .map { it to parseIsoDate(it) }
            .filter { it.second.after(now) }
            .minByOrNull { it.second }?.first ?: race.session5
    }

    private fun getCountdown(sessionTime: String): Countdown {
        return try {
            val diff = parseIsoDate(sessionTime).time - Date().time
            if (diff <= 0) return Countdown(0, 0, 0)
            Countdown(TimeUnit.MILLISECONDS.toDays(diff).toInt(), (TimeUnit.MILLISECONDS.toHours(diff) % 24).toInt(), (TimeUnit.MILLISECONDS.toMinutes(diff) % 60).toInt())
        } catch (_: Exception) { Countdown(0, 0, 0) }
    }

    private fun getCountryFlag(country: String): String {
        val flagMap = mapOf("Bahrain" to "🇧🇭", "Saudi Arabia" to "🇸🇦", "Australia" to "🇦🇺", "Japan" to "🇯🇵", "China" to "🇨🇳", "United States" to "🇺🇸", "Italy" to "🇮🇹", "Monaco" to "🇲🇨", "Canada" to "🇨🇦", "Spain" to "🇪🇸", "Austria" to "🇦🇹", "United Kingdom" to "🇬🇧", "Hungary" to "🇭🇺", "Belgium" to "🇧🇪", "Netherlands" to "🇳🇱", "Azerbaijan" to "🇦🇿", "Singapore" to "🇸🇬", "Mexico" to "🇲🇽", "Brazil" to "🇧🇷", "USA" to "🇺🇸", "Las Vegas" to "🇺🇸", "Qatar" to "🇶🇦", "Abu Dhabi" to "🇦🇪", "UAE" to "🇦🇪")
        return flagMap[country] ?: ""
    }

    data class Countdown(val days: Int, val hours: Int, val minutes: Int)
}
