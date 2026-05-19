package com.wanderlog.android.domain.model

data class TripPhoto(
    val id: Long,
    val contentUri: String,
    val displayName: String,
    val mimeType: String?,
    val capturedAtMillis: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val hasLocation: Boolean
        get() = latitude != null && longitude != null
}
