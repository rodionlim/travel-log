package com.wanderlog.android.domain.repository

import android.content.IntentSender
import com.wanderlog.android.domain.model.TripPhoto
import java.time.LocalDate

sealed interface LocalPhotoDeleteResult {
    data class Deleted(
        val deletedPhotos: List<TripPhoto>
    ) : LocalPhotoDeleteResult

    data class RequiresUserConfirmation(
        val intentSender: IntentSender,
        val pendingPhotos: List<TripPhoto>,
        val deletedPhotos: List<TripPhoto> = emptyList(),
        val retryAfterConfirmation: Boolean
    ) : LocalPhotoDeleteResult
}

interface LocalPhotoRepository {
    suspend fun getPhotosForDateRange(startDate: LocalDate, endDate: LocalDate): List<TripPhoto>
    suspend fun deletePhotos(photos: List<TripPhoto>): LocalPhotoDeleteResult
}
