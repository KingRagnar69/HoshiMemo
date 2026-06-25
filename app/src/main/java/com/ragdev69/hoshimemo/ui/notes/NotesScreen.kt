package com.ragdev69.hoshimemo.ui.notes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ragdev69.hoshimemo.R
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.ui.navigation.Screen
import com.ragdev69.hoshimemo.ui.sound.LocalSoundConfig
import com.ragdev69.hoshimemo.ui.sound.SoundManager
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavHostController,
    viewModel: NotesViewModel = viewModel()
) {
    val colors      = LocalHoshiColors.current
    val soundConfig = LocalSoundConfig.current
    val notes       by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val isGrid      by viewModel.isGridLayout.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortBy      by viewModel.sortBy.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    // ── Selection state ───────────────────────────────────────
    // selectedIds: set of note IDs currently checked.
    // isSelectionMode: true when at least one note is selected (long-pressed).
    var selectedIds     by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode  = selectedIds.isNotEmpty()

    // Exit selection mode when notes list changes (e.g. after trash)
    LaunchedEffect(notes) {
        if (isSelectionMode) {
            // Keep only IDs that still exist in the list
            selectedIds = selectedIds.intersect(notes.map { it.id }.toSet())
        }
    }

    // Read strings at composable scope — safe, not inside lambdas
    val strTitle         = stringResource(R.string.notes_title)
    val strCount         = stringResource(R.string.notes_count, notes.size)
    val strSortModified  = stringResource(R.string.notes_sort_modified)
    val strSortCreated   = stringResource(R.string.notes_sort_created)
    val strSortName      = stringResource(R.string.notes_sort_name)
    val strSearchHint    = stringResource(R.string.notes_search_hint)
    val strEmptyTitle    = stringResource(R.string.notes_empty_title)
    val strEmptySubtitle = stringResource(R.string.notes_empty_subtitle)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    // ── Selection mode header ─────────────────
                    // Close button — clears selection
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cancel selection",
                            tint = colors.text
                        )
                    }
                    Text(
                        text  = "${selectedIds.size} selected",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = colors.text
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    // Select-all toggle
                    IconButton(onClick = {
                        selectedIds = if (selectedIds.size == notes.size) {
                            emptySet()
                        } else {
                            notes.map { it.id }.toSet()
                        }
                    }) {
                        Icon(
                            imageVector = if (selectedIds.size == notes.size)
                                Icons.Filled.Deselect else Icons.Filled.SelectAll,
                            contentDescription = "Select all",
                            tint = colors.primary
                        )
                    }
                    // Trash selected
                    IconButton(onClick = {
                        viewModel.moveMultipleToTrash(selectedIds.toList())
                        selectedIds = emptySet()
                    }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Move selected to trash",
                            tint = colors.accent
                        )
                    }
                } else {
                    // ── Normal header ─────────────────────────
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = strTitle,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color      = colors.text
                            )
                        )
                        Text(
                            text  = strCount,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary
                            )
                        )
                    }

                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Filled.Sort,
                                contentDescription = "Sort",
                                tint = colors.textSecondary
                            )
                        }
                        DropdownMenu(
                            expanded         = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier         = Modifier.background(colors.surface)
                        ) {
                            listOf(
                                "updatedAt" to strSortModified,
                                "createdAt" to strSortCreated,
                                "title"     to strSortName
                            ).forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            color = if (sortBy == key) colors.primary else colors.text
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortBy(key)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Layout toggle
                    IconButton(onClick = { viewModel.setGridLayout(!isGrid) }) {
                        Icon(
                            imageVector        = if (isGrid) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle layout",
                            tint               = colors.primary
                        )
                    }
                }
            }

            // ── Search Bar (hidden during selection mode) ─────
            if (!isSelectionMode) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(strSearchHint, color = colors.textSecondary) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = colors.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(16.dp),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = colors.primary,
                        unfocusedBorderColor    = colors.border,
                        focusedTextColor        = colors.text,
                        unfocusedTextColor      = colors.text,
                        cursorColor             = colors.primary,
                        focusedContainerColor   = colors.surface,
                        unfocusedContainerColor = colors.surface
                    )
                )
            }

            // ── Notes Grid / List ────────────────────────────
            if (notes.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐", fontSize = 60.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            strEmptyTitle,
                            style = MaterialTheme.typography.titleMedium.copy(color = colors.text)
                        )
                        Text(
                            strEmptySubtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            } else {
                AnimatedContent(
                    targetState  = isGrid,
                    transitionSpec = {
                        fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                    },
                    label = "layout_switch"
                ) { grid ->
                    if (grid) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start  = 12.dp,
                                end    = 12.dp,
                                top    = 4.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement   = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                val isSelected = note.id in selectedIds
                                NoteCard(
                                    note        = note,
                                    isSelected  = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick     = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected)
                                                selectedIds - note.id
                                            else
                                                selectedIds + note.id
                                        } else {
                                            SoundManager.playNav(soundConfig.secretTheme, soundConfig.muted)
                                            navController.navigate(
                                                Screen.NoteEditor.createRoute(
                                                    note.id, note.title, note.content
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        // In selection mode: tap toggles selection
                                        selectedIds = selectedIds + note.id
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start  = 12.dp,
                                end    = 12.dp,
                                top    = 4.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                val isSelected = note.id in selectedIds
                                NoteCard(
                                    note            = note,
                                    isListMode      = true,
                                    isSelected      = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick         = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected)
                                                selectedIds - note.id
                                            else
                                                selectedIds + note.id
                                        } else {
                                            SoundManager.playNav(soundConfig.secretTheme, soundConfig.muted)
                                            navController.navigate(
                                                Screen.NoteEditor.createRoute(
                                                    note.id, note.title, note.content
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        selectedIds = selectedIds + note.id
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── FAB (hidden during selection mode) ────────────────
        if (!isSelectionMode) {
            FloatingActionButton(
                onClick        = {
                    SoundManager.playNav(soundConfig.secretTheme, soundConfig.muted)
                    navController.navigate(Screen.NoteEditor.createRoute())
                },
                modifier       = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
                    .shadow(12.dp, CircleShape),
                containerColor = colors.primary,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Note")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    isListMode: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val colors   = LocalHoshiColors.current
    val sdf      = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val untitled = stringResource(R.string.notes_untitled)

    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.0f,
        targetValue   = 0.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Selected cards get a colored border highlight
    val cardBorder = if (isSelected)
        BorderStroke(2.dp, colors.primary)
    else
        null

    // FIX: removed Card(onClick=onClick) — having both Card's own onClick AND
    // combinedClickable on the modifier caused double-firing on tap and prevented
    // long press from being detected. Now combinedClickable is the single handler.
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .then(if (!isListMode) Modifier.height(160.dp) else Modifier)
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            ),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected)
                colors.primary.copy(alpha = 0.15f)
            else
                colors.card
        ),
        border    = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glow overlay (only when not selected)
            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    colors.primary.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Pin indicator / selection checkbox row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isPinned && !isSelectionMode) {
                        Text("📌", fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Checkbox — visible in selection mode
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = if (isSelected) colors.primary else colors.border,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint     = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text     = note.title.ifEmpty { untitled },
                    style    = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = colors.text
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text     = note.content,
                    style    = MaterialTheme.typography.bodySmall.copy(
                        color      = colors.textSecondary,
                        lineHeight = 18.sp
                    ),
                    maxLines = if (isListMode) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text     = note.updatedAt?.let { sdf.format(it) } ?: "",
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary
                    ),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}