package com.wanderlog.android.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.app.RecoverableSecurityException
import android.media.ExifInterface
import android.database.Cursor
import androidx.core.net.toUri
import android.provider.MediaStore
import com.wanderlog.android.domain.repository.LocalPhotoDeleteResult
import com.wanderlog.android.domain.model.TripPhoto
import com.wanderlog.android.domain.repository.LocalPhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import javax.inject.Inject

class LocalPhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalPhotoRepository {

    override suspend fun getPhotosForDateRange(startDate: LocalDate, endDate: LocalDate): List<TripPhoto> =
        withContext(Dispatchers.IO) {
            val window = LocalPhotoQueryWindow.from(startDate, endDate)
            val photos = mutableListOf<TripPhoto>()

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PHOTO_PROJECTION,
                LOCAL_PHOTO_SELECTION,
                arrayOf(
                    window.startMillis.toString(),
                    window.endMillisExclusive.toString(),
                    window.startSeconds.toString(),
                    window.endSecondsExclusive.toString()
                ),
                "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val capturedAtMillis = resolveCapturedAtMillis(
                        dateTakenMillis = cursor.getLongOrNull(dateTakenIndex),
                        dateAddedSeconds = cursor.getLongOrNull(dateAddedIndex)
                    ) ?: continue
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val coordinates = readCoordinates(contentUri)

                    photos += TripPhoto(
                        id = id,
                        contentUri = contentUri.toString(),
                        displayName = cursor.getString(displayNameIndex).orEmpty().ifBlank { "Photo" },
                        mimeType = cursor.getString(mimeTypeIndex),
                        capturedAtMillis = capturedAtMillis,
                        latitude = coordinates?.first,
                        longitude = coordinates?.second
                    )
                }
            }

            photos.sortedByDescending(TripPhoto::capturedAtMillis)
        }

    override suspend fun deletePhotos(photos: List<TripPhoto>): LocalPhotoDeleteResult =
        withContext(Dispatchers.IO) {
            if (photos.isEmpty()) {
                return@withContext LocalPhotoDeleteResult.Deleted(emptyList())
            }

            val resolver = context.contentResolver

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    LocalPhotoDeleteResult.RequiresUserConfirmation(
                        intentSender = MediaStore.createDeleteRequest(
                            resolver,
                            photos.map { it.contentUri.toUri() }
                        ).intentSender,
                        pendingPhotos = photos,
                        retryAfterConfirmation = false
                    )
                } else {
                    val deletedPhotos = mutableListOf<TripPhoto>()

                    photos.forEachIndexed { index, photo ->
                        try {
                            val deletedCount = resolver.delete(photo.contentUri.toUri(), null, null)
                            if (deletedCount > 0) {
                                deletedPhotos += photo
                            }
                        } catch (error: SecurityException) {
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && error is RecoverableSecurityException) {
                                return@withContext LocalPhotoDeleteResult.RequiresUserConfirmation(
                                    intentSender = error.userAction.actionIntent.intentSender,
                                    pendingPhotos = photos.drop(index),
                                    deletedPhotos = deletedPhotos,
                                    retryAfterConfirmation = true
                                )
                            }
                            throw error
                        }
                    }

                    LocalPhotoDeleteResult.Deleted(deletedPhotos)
                }
            } catch (error: SecurityException) {
                throw error
            }
        }

    private fun readCoordinates(uri: Uri): Pair<Double, Double>? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val latLong = FloatArray(2)
            if (ExifInterface(inputStream).getLatLong(latLong)) {
                (latLong[0].toDouble() to latLong[1].toDouble()).takeIf(::isMeaningfulCoordinate)
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun isMeaningfulCoordinate(coordinates: Pair<Double, Double>): Boolean {
    val (latitude, longitude) = coordinates
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        return false
    }

    return !(abs(latitude) < 0.00001 && abs(longitude) < 0.00001)
}

private val PHOTO_PROJECTION = arrayOf(
    MediaStore.Images.Media._ID,
    MediaStore.Images.Media.DISPLAY_NAME,
    MediaStore.Images.Media.MIME_TYPE,
    MediaStore.Images.Media.DATE_TAKEN,
    MediaStore.Images.Media.DATE_ADDED
)

private val LOCAL_PHOTO_SELECTION = """
    (
        ${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?
    ) OR (
        (${MediaStore.Images.Media.DATE_TAKEN} IS NULL OR ${MediaStore.Images.Media.DATE_TAKEN} = 0)
        AND ${MediaStore.Images.Media.DATE_ADDED} >= ?
        AND ${MediaStore.Images.Media.DATE_ADDED} < ?
    )
""".trimIndent()

private data class LocalPhotoQueryWindow(
    val startMillis: Long,
    val endMillisExclusive: Long,
    val startSeconds: Long,
    val endSecondsExclusive: Long
) {
    companion object {
        fun from(startDate: LocalDate, endDate: LocalDate): LocalPhotoQueryWindow {
            val zoneId = ZoneId.systemDefault()
            val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillisExclusive = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            return LocalPhotoQueryWindow(
                startMillis = startMillis,
                endMillisExclusive = endMillisExclusive,
                startSeconds = startMillis / 1000,
                endSecondsExclusive = endMillisExclusive / 1000
            )
        }
    }
}

internal fun resolveCapturedAtMillis(dateTakenMillis: Long?, dateAddedSeconds: Long?): Long? {
    if (dateTakenMillis != null && dateTakenMillis > 0) {
        return dateTakenMillis
    }
    if (dateAddedSeconds != null && dateAddedSeconds > 0) {
        return dateAddedSeconds * 1000
    }
    return null
}

private fun Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)
