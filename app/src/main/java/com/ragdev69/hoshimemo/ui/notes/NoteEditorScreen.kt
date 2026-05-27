package com.ragdev69.hoshimemo.ui.notes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.NotesViewModel

@Composable
fun NoteEditorScreen(
    noteId: String = "",
    noteTitle: String = "",
    noteContent: String = "",
    onBack: () -> Unit,
    viewModel: NotesViewModel = viewModel()
) {
    val colors = LocalHoshiColors.current

    var title   by remember { mutableStateOf(noteTitle) }
    var content by remember { mutableStateOf(noteContent) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val wordCount = remember(content) {
        content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    }
    val charCount = content.length

    val titleAlpha by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(400, delayMillis = 100),
        label         = "title_fade"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(400, delayMillis = 200),
        label         = "content_fade"
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = colors.surface,
            title = {
                Text("Move to Trash?", color = colors.text, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This note will be moved to trash.", color = colors.textSecondary)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteId.isNotEmpty()) viewModel.moveToTrash(noteId)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Move", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 50.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text  = "$wordCount words · $charCount chars",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                if (noteId.isNotEmpty()) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = colors.accent
                        )
                    }
                }
            }

            // ── Divider ──────────────────────────────────────
            // Fixed: Divider is deprecated in Material3, use HorizontalDivider
            HorizontalDivider(
                color     = colors.border,
                thickness = 0.5.dp,
                modifier  = Modifier.padding(horizontal = 20.dp)
            )

            // ── Title Input ──────────────────────────────────
            TextField(
                value         = title,
                onValueChange = { title = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .alpha(titleAlpha),
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color      = colors.textSecondary.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color      = colors.text,
                    fontWeight = FontWeight.Bold
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = colors.primary
                ),
                singleLine = true
            )

            // ── Content Input ────────────────────────────────
            TextField(
                value         = content,
                onValueChange = { content = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .alpha(contentAlpha),
                placeholder = {
                    Text(
                        "Start writing your thoughts...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textSecondary.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color      = colors.text,
                    lineHeight = 28.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = colors.primary
                )
            )
        }

        // ── Save FAB ──────────────────────────────────────────
        AnimatedVisibility(
            visible  = !isLoading,
            enter    = scaleIn() + fadeIn(),
            exit     = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        viewModel.saveNote(
                            Note(id = noteId, title = title, content = content)
                        ) { onBack() }
                    }
                },
                containerColor = colors.primary,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Save")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 32.dp),
                color = colors.primary
            )
        }
    }
}