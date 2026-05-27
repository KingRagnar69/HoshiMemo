package com.ragdev69.hoshimemo.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrashScreen(viewModel: NotesViewModel = viewModel()) {
    val colors  = LocalHoshiColors.current
    val trashed by viewModel.trashedNotes.collectAsStateWithLifecycle()
    var showEmptyDialog by remember { mutableStateOf(false) }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            containerColor   = colors.surface,
            title = { Text("Empty Trash?", color = colors.text, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "All ${trashed.size} notes will be permanently deleted. This cannot be undone.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyDialog = false
                }) { Text("Empty", color = colors.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Trash 🗑️",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold, color = colors.text
                ),
                modifier = Modifier.weight(1f)
            )
            if (trashed.isNotEmpty()) {
                TextButton(onClick = { showEmptyDialog = true }) {
                    Text(
                        "Empty Trash", color = colors.accent,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (trashed.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗑️", fontSize = 60.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Trash is empty",
                        style = MaterialTheme.typography.titleMedium.copy(color = colors.text)
                    )
                    Text(
                        "Deleted notes appear here",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textSecondary
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                // Fixed: can't mix horizontal/vertical with bottom — use start/top/end/bottom
                contentPadding = PaddingValues(
                    start  = 12.dp,
                    end    = 12.dp,
                    top    = 4.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashed, key = { it.id }) { note ->
                    TrashNoteCard(
                        note      = note,
                        onRestore = { viewModel.restoreNote(note.id) },
                        onDelete  = { viewModel.deletePermanently(note.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrashNoteCard(note: Note, onRestore: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalHoshiColors.current
    val sdf    = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = colors.surface,
            title = {
                Text("Delete Forever?", color = colors.text, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "\"${note.title.ifEmpty { "Untitled" }}\" will be permanently removed.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold, color = colors.text
                    )
                )
                if (note.content.isNotEmpty()) {
                    Text(
                        text     = note.content,
                        style    = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary
                        ),
                        maxLines = 2
                    )
                }
                note.updatedAt?.let {
                    Text(
                        text     = "Deleted ${sdf.format(it)}",
                        style    = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Filled.Restore,
                    contentDescription = "Restore",
                    tint = colors.primary
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = "Delete Forever",
                    tint = colors.accent
                )
            }
        }
    }
}