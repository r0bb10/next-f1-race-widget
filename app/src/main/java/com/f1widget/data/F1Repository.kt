package com.f1widget.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class F1Repository {

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val baseUrl = "https://api.openf1.org/v1"

    suspend fun getNextRace(): CalendarItem? {
        return try {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val sessions: List<OpenF1Session> = httpClient
                .get("$baseUrl/sessions?year=$year")
                .body()

            // Find next race weekend
            val now = Date()
            val nextRaceSession = sessions
                .filter { it.sessionType == "Race" && parseDate(it.dateStart).after(now) }
                .minByOrNull { parseDate(it.dateStart) }
                ?: return null

            // Get all sessions for that meeting
            val weekendSessions: List<OpenF1Session> = httpClient
                .get("$baseUrl/sessions?meeting_key=${nextRaceSession.meetingKey}")
                .body()

            // Sort by date and convert to CalendarItem
            nextRaceSession.toCalendarItem(weekendSessions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getNextRaceFlow(): Flow<CalendarItem?> = flow {
        emit(getNextRace())
    }

    private fun parseDate(isoDate: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(isoDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun OpenF1Session.toCalendarItem(allSessions: List<OpenF1Session>): CalendarItem {
        val sorted = allSessions.sortedBy { it.dateStart }

        // Determine weekend type
        val isSprint = allSessions.any {
            it.sessionName.contains("Sprint", ignoreCase = true) &&
            !it.sessionName.contains("Qualifying", ignoreCase = true)
        }

        // Extract round number from session key or meeting key
        val roundNum = "round_${String.format("%02d", meetingKey % 100)}"

        return CalendarItem(
            id = roundNum,
            roundTitle = "$countryName Grand Prix",
            venueName = circuitShortName,
            country = countryName,
            track = location,
            session1 = sorted.getOrNull(0)?.dateStart ?: "",
            session2 = sorted.getOrNull(1)?.dateStart ?: "",
            session3 = sorted.getOrNull(2)?.dateStart ?: "",
            session4 = sorted.getOrNull(3)?.dateStart ?: "",
            session5 = sorted.getOrNull(4)?.dateStart ?: "",
            weekendType = if (isSprint) "sprint" else null
        )
    }

    @Serializable
    data class OpenF1Session(
        @SerialName("session_key") val sessionKey: Int,
        @SerialName("session_type") val sessionType: String,
        @SerialName("session_name") val sessionName: String,
        @SerialName("date_start") val dateStart: String,
        @SerialName("date_end") val dateEnd: String,
        @SerialName("meeting_key") val meetingKey: Int,
        @SerialName("circuit_short_name") val circuitShortName: String,
        @SerialName("country_name") val countryName: String,
        @SerialName("country_code") val countryCode: String,
        @SerialName("location") val location: String,
        @SerialName("year") val year: Int
    )
}
