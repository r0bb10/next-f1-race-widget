package com.f1widget.data

data class CalendarItem(
    val id: String = "",
    val roundTitle: String = "",
    val venueName: String = "",
    val country: String = "",
    val track: String = "",
    val session1: String = "", // Practice 1 or FP1
    val session2: String = "", // Practice 2, FP2, or Sprint Qualifying
    val session3: String = "", // Practice 3, FP3, or Sprint
    val session4: String = "", // Qualifying
    val session5: String = "", // Race
    val weekendType: String? = null
)
