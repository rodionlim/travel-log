package com.wanderlog.android.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TravellerProfile(
    val name: String,
    val age: Int? = null
) {
    val displayName: String
        get() = age?.let { "$name ($it)" } ?: name
}
