@file:OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
package com.f1widget.data

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

data class CalendarItem(
    val id: String = "",
    val roundTitle: String = "",
    val venueName: String = "",
    val country: String = "",
    val track: String = "",
    val session1: String = "",
    val session2: String = "",
    val session3: String = "",
    val session4: String = "",
    val session5: String = "",
    val weekendType: String? = null
)

class F1Repository {
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 15000; connectTimeoutMillis = 15000; socketTimeoutMillis = 15000 }
    }

    private val baseUrl = "https://api.openf1.org/v1"

    suspend fun getNextRace(): CalendarItem? = try {
        val now = Date()
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val sessions: List<OpenF1Session> = httpClient.get("$baseUrl/sessions?year=$year").body()

        val allRaces = sessions.filter { it.sessionType == "Race" }.sortedBy { parseDate(it.dateStart) }
        val nextRace = allRaces.firstOrNull { parseDate(it.dateStart).after(now) } ?: allRaces.lastOrNull() ?: return null

        // Count unique meetings (race weekends) to get correct round number
        val uniqueMeetings = allRaces.takeWhile { it.meetingKey <= nextRace.meetingKey }.distinctBy { it.meetingKey }
        val roundId = "round_${uniqueMeetings.size}"
        val weekend: List<OpenF1Session> = try { httpClient.get("$baseUrl/sessions?meeting_key=${nextRace.meetingKey}").body() } catch (_: Exception) { listOf(nextRace) }

        nextRace.toCalendarItem(weekend, roundId)
    } catch (e: Exception) {
        Log.e("F1Repo", "Error: ${e.message}"); null
    }

    private fun parseDate(isoDate: String): Date = try {
        val clean = isoDate.split("+")[0].split("Z")[0]
        val format = SimpleDateFormat(if (clean.contains(".")) "yyyy-MM-dd'T'HH:mm:ss.SSS" else "yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        format.apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(clean) ?: Date(0)
    } catch (_: Exception) { Date(0) }

    private fun OpenF1Session.toCalendarItem(all: List<OpenF1Session>, roundId: String): CalendarItem {
        val sorted = all.sortedBy { it.dateStart }
        return CalendarItem(
            id = roundId,
            roundTitle = "$countryName Grand Prix",
            venueName = circuitShortName,
            country = countryName,
            track = location,
            session1 = sorted.getOrNull(0)?.dateStart ?: "",
            session2 = sorted.getOrNull(1)?.dateStart ?: "",
            session3 = sorted.getOrNull(2)?.dateStart ?: "",
            session4 = sorted.getOrNull(3)?.dateStart ?: "",
            session5 = sorted.getOrNull(4)?.dateStart ?: "",
            weekendType = if (all.any { it.sessionName.contains("Sprint", true) && !it.sessionName.contains("Qualifying", true) }) "sprint" else null
        )
    }

    @Serializable
    data class OpenF1Session(
        @SerialName("session_type") val sessionType: String,
        @SerialName("session_name") val sessionName: String,
        @SerialName("date_start") val dateStart: String,
        @SerialName("meeting_key") val meetingKey: Int,
        @SerialName("circuit_short_name") val circuitShortName: String,
        @SerialName("country_name") val countryName: String,
        @SerialName("location") val location: String
    )
}
