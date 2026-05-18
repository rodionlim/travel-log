package com.wanderlog.android.domain.model

data class TripNote(
    val id: String,
    val tripId: String,
    val content: String,
    val isGlobal: Boolean = false
)
