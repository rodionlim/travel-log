package com.wanderlog.android.domain.usecase.ai

import android.content.Context
import android.net.Uri
import com.wanderlog.android.core.util.toDataUri
import com.wanderlog.android.core.util.FileUtils
import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.data.remote.openai.dto.ImagePart
import com.wanderlog.android.data.remote.openai.dto.ImageUrlDto
import com.wanderlog.android.data.remote.openai.dto.TextPart
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.repository.AiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ParseFileUseCase @Inject constructor(
    private val repo: AiRepository,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(
        uri: Uri,
        hint: DocumentHint? = null,
        rasterizePdfAsImages: Boolean = false
    ): ParsedBooking = invoke(listOf(uri), hint, rasterizePdfAsImages)

    suspend operator fun invoke(
        uris: List<Uri>,
        hint: DocumentHint? = null,
        rasterizePdfAsImages: Boolean = false
    ): ParsedBooking = withContext(Dispatchers.IO) {
        val parts = uris.flatMapIndexed { index, uri ->
            val mimeType = FileUtils.getMimeType(context, uri) ?: "application/octet-stream"
            val displayName = FileUtils.getDisplayName(context, uri) ?: "file-${index + 1}"
            buildList {
                add(
                    TextPart(
                        text = "Uploaded file ${index + 1}: $displayName ($mimeType). Parse this together with the other uploaded files as part of the same booking request."
                    )
                )
                addAll(contentPartsForUri(uri, mimeType, rasterizePdfAsImages))
            }
        }
        repo.parseFile(parts, hint)
    }

    private suspend fun contentPartsForUri(
        uri: Uri,
        mimeType: String,
        rasterizePdfAsImages: Boolean
    ): List<ContentPartDto> {
        return when {
            mimeType.startsWith("image/") -> {
                val bytes = FileUtils.readBytes(context, uri)
                val compressed = FileUtils.compressImageToJpeg(bytes)
                listOf(ImagePart(imageUrl = ImageUrlDto(url = compressed.toDataUri("image/jpeg"))))
            }

            mimeType == "application/pdf" && rasterizePdfAsImages -> {
                FileUtils.rasterizePdf(context, uri).map { pageBytes ->
                    ImagePart(imageUrl = ImageUrlDto(url = pageBytes.toDataUri("image/jpeg")))
                }
            }

            mimeType == "application/pdf" -> {
                val text = FileUtils.readPdfText(context, uri)
                listOf(TextPart(text = text))
            }

            else -> {
                val text = FileUtils.readText(context, uri)
                listOf(TextPart(text = text))
            }
        }
    }
}
