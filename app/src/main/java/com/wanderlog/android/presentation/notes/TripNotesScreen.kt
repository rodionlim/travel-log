package com.wanderlog.android.presentation.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.wanderlog.android.core.ui.component.ConfirmDialog
import com.wanderlog.android.core.ui.component.WanderTopBar
import com.wanderlog.android.domain.model.TripNote

@Composable
fun TripNotesScreen(
    onBack: () -> Unit,
    viewModel: TripNotesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingNote by remember { mutableStateOf<TripNote?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<TripNote?>(null) }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            WanderTopBar(
                title = state.title,
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (state.canAdd) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add note")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NotesHeader(
                    isGlobalMode = state.isGlobalMode,
                    tripName = state.tripName
                )
            }

            if (state.isLoading) {
                item {
                    Text("Loading notes...")
                }
            } else if (state.notes.isEmpty()) {
                item {
                    EmptyNotesState(isGlobalMode = state.isGlobalMode)
                }
            } else {
                items(state.notes, key = { it.note.id }) { item ->
                    TripNoteCard(
                        item = item,
                        isGlobalMode = state.isGlobalMode,
                        onEdit = { editingNote = item.note },
                        onDelete = { noteToDelete = item.note }
                    )
                }
            }
        }
    }

    if (showAddDialog && state.tripId != null) {
        TripNoteEditDialog(
            title = "Add note",
            initialContent = "",
            initialIsGlobal = false,
            onDismiss = { showAddDialog = false },
            onSave = { content, isGlobal ->
                viewModel.addNote(content, isGlobal)
                showAddDialog = false
            }
        )
    }

    editingNote?.let { note ->
        TripNoteEditDialog(
            title = "Edit note",
            initialContent = note.content,
            initialIsGlobal = note.isGlobal,
            onDismiss = { editingNote = null },
            onSave = { content, isGlobal ->
                viewModel.updateNote(note, content, isGlobal)
                editingNote = null
            }
        )
    }

    noteToDelete?.let { note ->
        ConfirmDialog(
            title = "Delete note",
            message = "Delete this note?",
            onConfirm = {
                viewModel.deleteNote(note)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }
}

@Composable
private fun NotesHeader(
    isGlobalMode: Boolean,
    tripName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isGlobalMode) {
            Text(
                text = "Global notes tagged from a trip appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = if (tripName.isBlank()) "Add notes for this trip and mark the ones that should also appear on the home screen." else "Add notes for $tripName and mark the ones that should also appear on the home screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyNotesState(isGlobalMode: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isGlobalMode) "No global notes yet." else "No notes for this trip yet.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (isGlobalMode) "Open a trip, add a note, and enable home screen visibility to see it here." else "Add reminders like IDP, baggage rules, or check-in constraints here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TripNoteCard(
    item: TripNoteListItem,
    isGlobalMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isGlobalMode) {
                Text(
                    text = item.tripName ?: "Unknown trip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (item.note.isGlobal) {
                Text(
                    text = "Visible on home screen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = item.note.content,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit note")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete note")
                }
            }
        }
    }
}

@Composable
private fun TripNoteEditDialog(
    title: String,
    initialContent: String,
    initialIsGlobal: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    var content by remember(initialContent) { mutableStateOf(initialContent) }
    var isGlobal by remember(initialIsGlobal) { mutableStateOf(initialIsGlobal) }
    val isValid = content.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isGlobal,
                        onCheckedChange = { isGlobal = it }
                    )
                    Text("Show on home screen")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(content.trim(), isGlobal) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
