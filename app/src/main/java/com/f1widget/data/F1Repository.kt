package com.f1widget.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

class F1Repository {
    private val database = FirebaseDatabase.getInstance()
    private val rootRef = database.reference

    fun getNextRace(): Flow<CalendarItem?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val response = snapshot.getValue(FirebaseResponse::class.java)
                val nextRace = response?.calendar?.let { findNextRace(it) }
                trySend(nextRace)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        rootRef.addValueEventListener(listener)
        awaitClose { rootRef.removeEventListener(listener) }
    }

    private fun findNextRace(calendar: List<CalendarItem>): CalendarItem? {
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return calendar
            .filter { item ->
                // Check if race (session5) is in the future
                try {
                    val raceDate = dateFormat.parse(item.session5)
                    raceDate?.after(now) == true
                } catch (e: Exception) {
                    false
                }
            }
            .minByOrNull { item ->
                try {
                    dateFormat.parse(item.session5)?.time ?: Long.MAX_VALUE
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
            }
    }
}
