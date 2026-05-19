package com.wanderlog.android.presentation.ai.ask

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wanderlog.android.domain.model.Attachment
import org.junit.Rule
import org.junit.Test

class AskTripContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun manyAttachments_startCollapsed_andKeepQuestionFieldVisible() {
        val attachments = List(40) { index ->
            Attachment(
                id = "attachment-$index",
                tripId = "trip-1",
                displayName = "Attachment $index",
                mimeType = "text/plain",
                localPath = "attachments/$index.txt",
                sizeBytes = 128L,
                createdAt = index.toLong()
            )
        }

        composeRule.setContent {
            MaterialTheme {
                AskTripContent(
                    state = AskTripUiState(
                        tripName = "Japan",
                        tripDestination = "Tokyo",
                        attachments = attachments,
                        isLoading = false
                    ),
                    modifier = Modifier.fillMaxSize(),
                    onQuestionChange = {},
                    onToggleAttachment = {},
                    onSendQuestion = {},
                    onClearError = {}
                )
            }
        }

        composeRule.onNodeWithText("Show attachments (40)").assertIsDisplayed()
        composeRule.onAllNodesWithText("Attachment 0").assertCountEquals(0)
        composeRule.onNodeWithText("Ask a question").assertIsDisplayed()
        composeRule.onNodeWithText("Ask About Trip").assertIsDisplayed()

        composeRule.onNodeWithText("Show attachments (40)").performClick()

        composeRule.onNodeWithText("Attachment 0").assertIsDisplayed()
        composeRule.onNodeWithText("Ask a question").assertIsDisplayed()
    }
}
