package com.wanderlog.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun OpenAiApiKeyHelpDialog(
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenOpenAi: (() -> Unit)? = null,
    introductoryText: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                buildString {
                    introductoryText?.trim()?.takeIf { it.isNotBlank() }?.let {
                        append(it)
                        append("\n\n")
                    }
                    append("1. Sign in to your OpenAI account and create an API key from the API keys page.\n\n")
                    append("2. Copy that key and paste it into the OpenAI API Key field in Wanderlog Settings.\n\n")
                    append(
                        "Tip: if you enable data sharing in OpenAI under Settings > Data controls > Sharing, OpenAI can grant free daily tokens. That is typically enough to cover this app's normal API usage, as long as you do not upload large images or rasterize PDFs heavily."
                    )
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onOpenOpenAi?.let { openOpenAi ->
                    TextButton(onClick = openOpenAi) {
                        Text("Open OpenAI")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        }
    )
}
