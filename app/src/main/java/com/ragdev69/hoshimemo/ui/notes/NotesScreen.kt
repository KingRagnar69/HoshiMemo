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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.ui.navigation.Screen
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
    val notes       by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val isGrid      by viewModel.isGridLayout.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortBy      by viewModel.sortBy.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "Hoshi Memo ⭐",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = colors.text
                        )
                    )
                    Text(
                        text  = "${notes.size} notes",
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
                            "updatedAt" to "Date Modified",
                            "createdAt" to "Date Created",
                            "title"     to "Name A-Z"
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

            // ── Search Bar ───────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("🔍  Search notes...", color = colors.textSecondary) },
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
                            "No notes yet",
                            style = MaterialTheme.typography.titleMedium.copy(color = colors.text)
                        )
                        Text(
                            "Tap + to create your first note",
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
                            // Fixed: can't mix horizontal/vertical with bottom
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
                                NoteCard(
                                    note    = note,
                                    onClick = {
                                        navController.navigate(
                                            Screen.NoteEditor.createRoute(
                                                note.id, note.title, note.content
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            // Fixed: can't mix horizontal/vertical with bottom
                            contentPadding = PaddingValues(
                                start  = 12.dp,
                                end    = 12.dp,
                                top    = 4.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(
                                    note       = note,
                                    isListMode = true,
                                    onClick    = {
                                        navController.navigate(
                                            Screen.NoteEditor.createRoute(
                                                note.id, note.title, note.content
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────
        FloatingActionButton(
            onClick        = { navController.navigate(Screen.NoteEditor.createRoute()) },
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

@Composable
fun NoteCard(
    note: Note,
    isListMode: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalHoshiColors.current
    val sdf    = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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

    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .then(if (!isListMode) Modifier.height(160.dp) else Modifier),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                if (note.isPinned) {
                    Text("📌", fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                }

                Text(
                    text     = note.title.ifEmpty { "Untitled" },
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