package com.f1widget.data

data class FirebaseResponse(
    val calendar: List<CalendarItem>? = null,
    val lastUpdateTimestamp: Double? = null
)
