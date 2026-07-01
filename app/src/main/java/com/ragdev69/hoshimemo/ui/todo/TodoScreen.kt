package com.ragdev69.hoshimemo.ui.todo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.R
import com.ragdev69.hoshimemo.data.model.TodoItem
import com.ragdev69.hoshimemo.ui.sound.LocalSoundConfig
import com.ragdev69.hoshimemo.ui.sound.SoundManager
import com.ragdev69.hoshimemo.ui.sound.SoundPrefs
import com.ragdev69.hoshimemo.ui.theme.HoshiColors
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.TodoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val colors           = LocalHoshiColors.current
    val context          = LocalContext.current
    val scope            = rememberCoroutineScope()
    val soundConfig      = LocalSoundConfig.current
    val soundMuted       by SoundPrefs.getMutedFlow(context).collectAsStateWithLifecycle(initialValue = soundConfig.muted)
    val todos            by viewModel.todos.collectAsStateWithLifecycle()
    val ungroupedPending by viewModel.ungroupedPending.collectAsStateWithLifecycle()
    val ungroupedDone    by viewModel.ungroupedCompleted.collectAsStateWithLifecycle()
    val ungroupedFailed  by viewModel.ungroupedFailed.collectAsStateWithLifecycle()
    val groups           by viewModel.groups.collectAsStateWithLifecycle()
    val groupTasksMap    by viewModel.groupTasksMap.collectAsStateWithLifecycle()
    val expiredDismissed by viewModel.expiredDialogDismissed.collectAsStateWithLifecycle()
    val justCompleted    by viewModel.justCompletedIds.collectAsStateWithLifecycle()
    val justFailed       by viewModel.justFailedIds.collectAsStateWithLifecycle()

    var showAddDialog    by remember { mutableStateOf(false) }
    var newTaskText      by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(0) }
    var timerH           by remember { mutableStateOf("") }
    var timerM           by remember { mutableStateOf("") }
    var timerS           by remember { mutableStateOf("") }
    var selectedGroupId  by remember { mutableStateOf("") }
    var selectedGroupName by remember { mutableStateOf("") }

    var showGroupDialog  by remember { mutableStateOf(false) }
    var newGroupName     by remember { mutableStateOf("") }
    var expandedGroupId  by remember { mutableStateOf<String?>(null) }

    // ── Multi-select (long-press to enter, tap to toggle, delete selected) ──
    var selectionMode    by remember { mutableStateOf(false) }
    var selectedIds       by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    fun toggleSelected(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) selectionMode = false
    }
    fun enterSelectionMode(id: String) {
        selectionMode = true
        selectedIds = setOf(id)
    }
    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = emptySet()
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            containerColor   = colors.surface,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Delete ${selectedIds.size} task${if (selectedIds.size > 1) "s" else ""}?", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text  = { Text("This can't be undone.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        selectedIds.forEach { viewModel.deleteTodo(it) }
                        showDeleteSelectedDialog = false
                        exitSelectionMode()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false; newGroupName = "" },
            containerColor   = colors.surface,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("New Group", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                OutlinedTextField(
                    value = newGroupName, onValueChange = { newGroupName = it },
                    placeholder = { Text("Group name", color = colors.textSecondary) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary, unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                        cursorColor = colors.primary, focusedContainerColor = colors.card, unfocusedContainerColor = colors.card
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.createGroup(newGroupName.trim())
                            newGroupName = ""
                            showGroupDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) { Text("Create", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false; newGroupName = "" }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false; newTaskText = ""
                timerH = ""; timerM = ""; timerS = ""
                selectedPriority = 0; selectedGroupId = ""; selectedGroupName = ""
            },
            containerColor = colors.surface,
            shape          = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, null, tint = colors.primary, modifier = Modifier.size(20.dp)) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (selectedGroupId.isNotEmpty()) "Add to $selectedGroupName" else stringResource(R.string.todo_new_task),
                        color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskText, onValueChange = { newTaskText = it },
                        placeholder = { Text(stringResource(R.string.todo_task_hint), color = colors.textSecondary) },
                        singleLine = true, shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary, unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                            cursorColor = colors.primary, focusedContainerColor = colors.card, unfocusedContainerColor = colors.card
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.todo_priority), color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(stringResource(R.string.todo_priority_normal), 0, colors.textSecondary),
                            Triple(stringResource(R.string.todo_priority_medium), 1, Color(0xFFFFAA00)),
                            Triple(stringResource(R.string.todo_priority_high),   2, Color(0xFFFF4444))
                        ).forEach { (label, p, chipColor) ->
                            val sel = selectedPriority == p
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(if (sel) chipColor.copy(alpha = 0.18f) else colors.card)
                                    .border(1.5.dp, if (sel) chipColor else colors.border, RoundedCornerShape(50.dp))
                                    .clickable { selectedPriority = p }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                                    .background(if (sel) chipColor else colors.border))
                                Text(label, color = if (sel) chipColor else colors.textSecondary,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Timer, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                        Text(
                            if (selectedGroupId.isNotEmpty()) "TIMER (recommended for groups)" else "TIMER (optional)",
                            color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf("h" to timerH, "m" to timerM, "s" to timerS).forEachIndexed { i, (lbl, v) ->
                            OutlinedTextField(
                                value = v,
                                onValueChange = { new ->
                                    val d = new.filter(Char::isDigit).take(2)
                                    when(i) { 0 -> timerH = d; 1 -> timerM = d; 2 -> timerS = d }
                                },
                                placeholder = { Text("0", color = colors.textSecondary) },
                                suffix = { Text(lbl, color = colors.textSecondary, fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary, unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                                    cursorColor = colors.primary, focusedContainerColor = colors.card, unfocusedContainerColor = colors.card
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Group picker only shown when opened from FAB (not pre-selected from a group's + button)
                    if (groups.isNotEmpty() && selectedGroupId.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("GROUP (optional)", color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val noneSelected = true
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(colors.primary.copy(alpha = 0.18f))
                                    .border(1.5.dp, colors.primary, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) { Text("None", color = colors.primary, fontSize = 13.sp) }

                            groups.forEach { g ->
                                val gId   = g["groupId"] ?: ""
                                val gName = g["groupName"] ?: ""
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(colors.card)
                                        .border(1.5.dp, colors.border, RoundedCornerShape(50.dp))
                                        .clickable { selectedGroupId = gId; selectedGroupName = gName }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) { Text(gName, color = colors.textSecondary, fontSize = 13.sp) }
                            }
                        }
                    } else if (selectedGroupId.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(colors.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.FolderOpen, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Will be added to: $selectedGroupName", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            val total = (timerH.toLongOrNull() ?: 0L) * 3600L +
                                    (timerM.toLongOrNull() ?: 0L) * 60L +
                                    (timerS.toLongOrNull() ?: 0L)
                            val order = todos.count { it.groupId == selectedGroupId }
                            viewModel.addTodo(newTaskText.trim(), selectedPriority, total, selectedGroupId, selectedGroupName, order)
                            newTaskText = ""; timerH = ""; timerM = ""; timerS = ""
                            selectedPriority = 0; selectedGroupId = ""; selectedGroupName = ""
                            showAddDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) { Text(stringResource(R.string.todo_add), fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false; newTaskText = ""
                    timerH = ""; timerM = ""; timerS = ""
                    selectedPriority = 0; selectedGroupId = ""; selectedGroupName = ""
                }) { Text(stringResource(R.string.todo_cancel), color = colors.textSecondary) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.surface, colors.background)))
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 16.dp)
            ) {
                if (selectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Filled.Close, "Cancel selection", tint = colors.text)
                        }
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colors.text),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                        IconButton(
                            onClick = {
                                selectedIds.forEach { viewModel.resetTodo(it) }
                                exitSelectionMode()
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Refresh, "Reset selected", tint = colors.primary)
                        }
                        IconButton(
                            onClick = { if (selectedIds.isNotEmpty()) showDeleteSelectedDialog = true },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Delete, "Delete selected", tint = Color(0xFFFF4444))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.todo_title),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = colors.text),
                            modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { scope.launch { SoundPrefs.setMuted(context, !soundMuted) } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (soundMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.todo_sound_toggle),
                                tint = if (soundMuted) colors.textSecondary.copy(alpha = 0.5f) else colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(colors.primary.copy(alpha = 0.13f))
                                .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                                .clickable { showGroupDialog = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Filled.CreateNewFolder, null, tint = colors.primary, modifier = Modifier.size(15.dp))
                                Text("New Group", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (ungroupedPending.isNotEmpty() || groups.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("← Fail", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFF4444).copy(alpha = 0.7f)))
                    Text("Complete →", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4CAF50).copy(alpha = 0.7f)))
                }
            }

            if (todos.isEmpty() && groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(100.dp).clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Text("✅", fontSize = 44.sp)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(stringResource(R.string.todo_empty_title), style = MaterialTheme.typography.titleLarge.copy(color = colors.text, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stringResource(R.string.todo_empty_subtitle), style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (groups.isNotEmpty()) {
                        item { TodoSectionHeader(label = "GROUPS", count = groups.size, color = colors.accent) }
                        items(groups, key = { it["groupId"] ?: "" }) { group ->
                            val gId      = group["groupId"] ?: ""
                            val gName    = group["groupName"] ?: ""
                            val expanded = expandedGroupId == gId
                            val groupTasks = groupTasksMap[gId] ?: emptyList()

                            GroupCard(
                                groupTasks = groupTasks,
                                groupName  = gName,
                                expanded   = expanded,
                                colors     = colors,
                                soundConfig = soundConfig,
                                onToggleExpand   = { expandedGroupId = if (expanded) null else gId },
                                onDeleteGroup    = { viewModel.deleteGroup(gId) },
                                onResetGroup     = { viewModel.resetGroup(gId) },
                                onAddTodoToGroup = { selectedGroupId = gId; selectedGroupName = gName; showAddDialog = true },
                                onComplete = { viewModel.completeTodo(it) },
                                onFail     = { viewModel.failTodo(it) },
                                onDelete   = { viewModel.deleteTodo(it) },
                                onReset    = { viewModel.resetTodo(it) },
                                onStartTimer = { viewModel.startTimer(it) },
                                justCompleted = justCompleted,
                                justFailed    = justFailed
                            )
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }

                    if (ungroupedPending.isNotEmpty()) {
                        item { TodoSectionHeader(stringResource(R.string.todo_section_pending), ungroupedPending.size, colors.textSecondary) }
                        items(ungroupedPending, key = { it.id }) { todo ->
                            AnimatedVisibility(visible = true, enter = fadeIn() + slideInHorizontally()) {
                                SwipeableTodoCard(
                                    todo = todo, colors = colors,
                                    onComplete = {
                                        SoundManager.playTodoComplete(soundConfig.secretTheme, soundConfig.muted)
                                        viewModel.completeTodo(todo.id)
                                    },
                                    onFail = { viewModel.failTodo(todo.id) },
                                    onDelete = { viewModel.deleteTodo(todo.id) },
                                    onReset = { viewModel.resetTodo(todo.id) },
                                    onStartTimer = {
                                        SoundManager.playTodoStart(soundConfig.secretTheme, soundConfig.muted)
                                        viewModel.startTimer(todo.id)
                                    },
                                    onMarkComplete = {
                                        SoundManager.playTodoComplete(soundConfig.secretTheme, soundConfig.muted)
                                        viewModel.toggleTodo(todo.id, true)
                                    },
                                    justCompleted = justCompleted, justFailed = justFailed,
                                    expiredDismissed = expiredDismissed,
                                    onExpiredDismissed = { viewModel.markExpiredDialogDismissed(todo.id) },
                                    showExpiredDialog = true,
                                    selectionMode = selectionMode,
                                    isSelected = todo.id in selectedIds,
                                    onLongPress = { enterSelectionMode(todo.id) },
                                    onTapSelect = { toggleSelected(todo.id) }
                                )
                            }
                        }
                    }

                    if (ungroupedDone.isNotEmpty()) {
                        item { Spacer(Modifier.height(4.dp)); TodoSectionHeader(stringResource(R.string.todo_section_completed), ungroupedDone.size, colors.primary) }
                        items(ungroupedDone, key = { it.id }) { todo ->
                            SwipeableTodoCard(
                                todo = todo, colors = colors,
                                onComplete = { viewModel.completeTodo(todo.id) },
                                onFail = { viewModel.failTodo(todo.id) },
                                onDelete = { viewModel.deleteTodo(todo.id) },
                                onReset = { viewModel.resetTodo(todo.id) },
                                onStartTimer = { viewModel.startTimer(todo.id) },
                                onMarkComplete = { viewModel.toggleTodo(todo.id, true) },
                                justCompleted = justCompleted, justFailed = justFailed,
                                expiredDismissed = expiredDismissed,
                                onExpiredDismissed = { viewModel.markExpiredDialogDismissed(todo.id) },
                                showExpiredDialog = false,
                                selectionMode = selectionMode,
                                isSelected = todo.id in selectedIds,
                                onLongPress = { enterSelectionMode(todo.id) },
                                onTapSelect = { toggleSelected(todo.id) }
                            )
                        }
                    }

                    if (ungroupedFailed.isNotEmpty()) {
                        item { Spacer(Modifier.height(4.dp)); TodoSectionHeader("FAILED", ungroupedFailed.size, Color(0xFFFF4444)) }
                        items(ungroupedFailed, key = { it.id }) { todo ->
                            SwipeableTodoCard(
                                todo = todo, colors = colors,
                                onComplete = { viewModel.completeTodo(todo.id) },
                                onFail = { viewModel.failTodo(todo.id) },
                                onDelete = { viewModel.deleteTodo(todo.id) },
                                onReset = { viewModel.resetTodo(todo.id) },
                                onStartTimer = { viewModel.startTimer(todo.id) },
                                onMarkComplete = { viewModel.toggleTodo(todo.id, true) },
                                justCompleted = justCompleted, justFailed = justFailed,
                                expiredDismissed = expiredDismissed,
                                onExpiredDismissed = { viewModel.markExpiredDialogDismissed(todo.id) },
                                showExpiredDialog = false,
                                selectionMode = selectionMode,
                                isSelected = todo.id in selectedIds,
                                onLongPress = { enterSelectionMode(todo.id) },
                                onTapSelect = { toggleSelected(todo.id) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { selectedGroupId = ""; selectedGroupName = ""; showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 24.dp),
            containerColor = colors.primary, contentColor = Color.White, shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) { Icon(Icons.Filled.Add, stringResource(R.string.todo_add), modifier = Modifier.size(26.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// GROUP CARD + TIMER ENGINE
// This is the core fix. Instead of each task in a group running its own
// independent timer and waiting on Firestore round-trips to know when to
// start the next one, this single composable owns the ENTIRE sequence
// locally:
//   - sortedTasks = pending tasks in orderInGroup order
//   - activeIndex tracks which one is "live" right now
//   - one LaunchedEffect runs a loop: count down activeIndex's timer ->
//     the instant it hits 0, immediately advance activeIndex (no delay,
//     no waiting for any network response) -> fire the Firestore write for
//     the finished task in the background (fire-and-forget, doesn't block)
// This means task 2 starts in the same frame task 1 hits zero, regardless
// of how slow or fast Firestore responds.
// ─────────────────────────────────────────────────────────────
@Composable
private fun GroupCard(
    groupTasks: List<TodoItem>,
    groupName: String,
    expanded: Boolean,
    colors: HoshiColors,
    soundConfig: com.ragdev69.hoshimemo.ui.sound.SoundConfig,
    onToggleExpand: () -> Unit,
    onDeleteGroup: () -> Unit,
    onResetGroup: () -> Unit,
    onAddTodoToGroup: () -> Unit,
    onComplete: (String) -> Unit,
    onFail: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReset: (String) -> Unit,
    onStartTimer: (String) -> Unit,
    justCompleted: Set<String>,
    justFailed: Set<String>
) {
    // ── FIX: fully local-first engine ──────────────────────────────────────
    // Root cause of "only task 1 runs / order wrong / last task never
    // triggers / progress doesn't update": the old engine waited on
    // Firestore round-trips (via the `groupTasks` parameter, which only
    // updates once a snapshot listener delivers a new list) to decide
    // both WHEN a task's timer had actually started and WHAT the next
    // task was. Firestore writes are fire-and-forget and can arrive late,
    // out of order, or be momentarily stale right after a local advance —
    // so the engine would stall on task 1, misjudge ordering, or never
    // see task 3 as "next" because the snapshot hadn't caught up yet.
    //
    // Fix: track completed/failed task IDs and the active task ID ENTIRELY
    // in local state, keyed only to the group's identity (its task ID set
    // doesn't change across completions, only flags do — so we seed once
    // and then own the state ourselves). Firestore writes still happen,
    // but purely as background persistence — they never gate sequencing,
    // never gate "what's next", and never gate the progress counter.
    val groupKey = remember(groupTasks.map { it.id }.sorted()) { groupTasks.map { it.id }.sorted() }

    // Local overrides — task IDs this engine has already advanced past,
    // independent of whether the Firestore write for them has landed yet.
    var localDoneIds by remember(groupKey) {
        mutableStateOf(groupTasks.filter { it.isCompleted }.map { it.id }.toSet())
    }
    var localFailedIds by remember(groupKey) {
        mutableStateOf(groupTasks.filter { it.isFailed }.map { it.id }.toSet())
    }

    // Merge: a task counts as done/failed if EITHER Firestore says so OR
    // our local engine has already moved past it.
    val mergedTasks = remember(groupTasks, localDoneIds, localFailedIds) {
        groupTasks.map { t ->
            when {
                t.id in localDoneIds && !t.isCompleted -> t.copy(isCompleted = true)
                t.id in localFailedIds && !t.isFailed   -> t.copy(isFailed = true)
                else -> t
            }
        }
    }

    val pendingSorted = mergedTasks.filter { !it.isCompleted && !it.isFailed }.sortedBy { it.orderInGroup }
    val done   = mergedTasks.count { it.isCompleted }
    val failed = mergedTasks.count { it.isFailed }

    // FIX: group was auto-starting itself whenever it rendered with a
    // pending task — including right after creating a brand-new group,
    // adding a fresh task to an idle group, or resetting a finished group.
    // None of those are "the user pressed start." We now track an explicit
    // `started` flag, seeded to false per group identity, that ONLY a
    // deliberate tap on the Start button flips to true. Nothing else
    // (recomposition, a new task arriving, a reset) is allowed to flip it.
    var started by remember(groupKey) { mutableStateOf(false) }

    // Which pending task is currently "active" (its timer is the one
    // actually running). Only meaningful once `started` is true.
    var activeId by remember(groupKey) { mutableStateOf<String?>(null) }

    val activeTask = mergedTasks.find { it.id == activeId }

    var localRemaining by remember(activeId) {
        mutableStateOf(mergedTasks.find { it.id == activeId }?.timerSeconds ?: 0L)
    }

    // The engine loop — entirely local. Only runs once `started` is true
    // AND there is an active task. Re-launches when activeId changes.
    LaunchedEffect(activeId, started) {
        if (!started) return@LaunchedEffect
        val id = activeId ?: return@LaunchedEffect
        val task = groupTasks.find { it.id == id } ?: return@LaunchedEffect
        if (task.timerSeconds <= 0L) {
            // No timer on this task — nothing to count down. Treat it as
            // immediately done so the queue doesn't stall waiting forever.
            localDoneIds = localDoneIds + id
            onComplete(id)
            val next = pendingSorted.firstOrNull { it.id != id }
            activeId = next?.id
            if (next == null) started = false
            return@LaunchedEffect
        }

        // Fire the "started" write in the background so other devices /
        // future sessions see it — but don't wait for it.
        onStartTimer(id)
        // Play start sound for every task
        SoundManager.playTodoStart(soundConfig.secretTheme, soundConfig.muted)

        var remaining = task.timerSeconds
        localRemaining = remaining
        while (remaining > 0L) {
            delay(1000L)
            remaining -= 1L
            localRemaining = remaining
        }

        // Timer hit zero — advance LOCALLY first (instant, no network
        // dependency), then fire the completion write in the background.
        localDoneIds = localDoneIds + id
        onComplete(id)

        val next = pendingSorted.firstOrNull { it.id != id }
        // Play complete sound only when the last task in the group finishes
        if (next == null) {
            SoundManager.playTodoComplete(soundConfig.secretTheme, soundConfig.muted)
        }
        activeId = next?.id
        if (next == null) started = false   // sequence finished
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FolderOpen, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(groupName, color = colors.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("$done/${mergedTasks.size} done · $failed failed", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Box(modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.border)) {
                    if (mergedTasks.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxHeight()
                            .fillMaxWidth(done.toFloat() / mergedTasks.size)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(listOf(colors.primary, colors.accent))))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                IconButton(onClick = onAddTodoToGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, "Add task", tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                if (!started && pendingSorted.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary.copy(alpha = 0.18f))
                            .clickable {
                                activeId = pendingSorted.firstOrNull()?.id
                                started = activeId != null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("START", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                IconButton(onClick = {
                    started = false
                    activeId = null
                    localDoneIds = emptySet()
                    localFailedIds = emptySet()
                    onResetGroup()
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, "Reset group", tint = colors.primary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDeleteGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    mergedTasks.sortedBy { it.orderInGroup }.forEach { todo ->
                        val isActiveTimer = todo.id == activeId
                        GroupTaskRow(
                            todo = todo, colors = colors,
                            displayRemaining = if (isActiveTimer) localRemaining else todo.timerSeconds,
                            isActive = isActiveTimer && !todo.isCompleted && !todo.isFailed,
                            onDelete = { onDelete(todo.id) },
                            onReset = {
                                localDoneIds = localDoneIds - todo.id
                                localFailedIds = localFailedIds - todo.id
                                onReset(todo.id)
                            },
                            justCompleted = justCompleted, justFailed = justFailed
                        )
                    }
                }
            }
        }
    }
}

// Simplified row used only inside a group — no swipe gestures, no manual
// complete/fail/start buttons. The group's timer engine drives everything
// automatically: the active task counts down on its own and the next one
// starts the instant the current one finishes. Reset/delete remain available.
@Composable
private fun GroupTaskRow(
    todo: TodoItem,
    colors: HoshiColors,
    displayRemaining: Long,
    isActive: Boolean,
    onDelete: () -> Unit,
    onReset: () -> Unit,
    justCompleted: Set<String>,
    justFailed: Set<String>
) {
    val isJustCompleted = todo.id in justCompleted
    val isJustFailed     = todo.id in justFailed
    val burstFraction by animateFloatAsState(if (isJustCompleted || isJustFailed) 1f else 0f, tween(400), label = "burst")
    val doneFraction   by animateFloatAsState(if (todo.isCompleted) 1f else 0f, tween(400), label = "done")
    val bg = when {
        isJustCompleted -> lerp(colors.card, Color(0xFF4CAF50).copy(alpha = 0.25f), burstFraction)
        isJustFailed    -> lerp(colors.card, Color(0xFFFF4444).copy(alpha = 0.25f), burstFraction)
        isActive        -> lerp(colors.card, colors.primary.copy(alpha = 0.10f), 1f)
        else            -> lerp(colors.card, colors.primary.copy(alpha = 0.07f), doneFraction)
    }
    val cardScale by animateFloatAsState(if (isJustCompleted || isJustFailed) 1.04f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "scale")

    val hasTimer = todo.timerSeconds > 0L

    Card(
        modifier  = Modifier.fillMaxWidth().scale(cardScale),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bg),
        border    = if (isActive && !todo.isCompleted && !todo.isFailed) BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(if (todo.isCompleted || todo.isFailed) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val priorityColor = when (todo.priority) { 2 -> Color(0xFFFF4444); 1 -> Color(0xFFFFAA00); else -> Color.Transparent }
            if (todo.priority > 0) {
                Box(modifier = Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(priorityColor))
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(
                    when {
                        todo.isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.18f)
                        todo.isFailed    -> Color(0xFFFF4444).copy(alpha = 0.18f)
                        isActive         -> colors.primary.copy(alpha = 0.18f)
                        else             -> colors.border.copy(alpha = 0.3f)
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    todo.isCompleted -> Icon(Icons.Filled.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    todo.isFailed    -> Icon(Icons.Filled.Close, null, tint = Color(0xFFFF4444), modifier = Modifier.size(16.dp))
                    isActive         -> Icon(Icons.Filled.PlayArrow, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    else             -> Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(colors.textSecondary.copy(alpha = 0.4f)))
                }
            }
            Spacer(modifier = Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = when { todo.isCompleted -> colors.textSecondary; todo.isFailed -> Color(0xFFFF4444).copy(alpha = 0.8f); else -> colors.text },
                        textDecoration = if (todo.isCompleted || todo.isFailed) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = if (todo.isCompleted || todo.isFailed) FontWeight.Normal else if (isActive) FontWeight.Bold else FontWeight.SemiBold
                    )
                )
                if (hasTimer) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val chipColor = when {
                        todo.isCompleted -> Color(0xFF4CAF50)
                        todo.isFailed    -> Color(0xFFFF4444)
                        !isActive        -> colors.textSecondary
                        displayRemaining <= 10L -> Color(0xFFFF4444)
                        displayRemaining <= 60L -> Color(0xFFFFAA00)
                        else             -> colors.primary
                    }
                    Text(
                        text = when {
                            todo.isCompleted -> "✓ ${formatTime(todo.timerSeconds)}"
                            todo.isFailed    -> "✗ ${formatTime(todo.timerSeconds)}"
                            isActive         -> "⏱ ${formatTime(displayRemaining)} · running"
                            else             -> "⏳ ${formatTime(todo.timerSeconds)} · waiting"
                        },
                        color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (todo.isCompleted || todo.isFailed) {
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, "Reset", tint = colors.primary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, "Delete", tint = colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Swipeable todo card — ungrouped tasks only
// ─────────────────────────────────────────────────────────────
@Composable
fun SwipeableTodoCard(
    todo: TodoItem, colors: HoshiColors,
    onComplete: () -> Unit, onFail: () -> Unit,
    onDelete: () -> Unit, onReset: () -> Unit,
    onStartTimer: () -> Unit, onMarkComplete: () -> Unit,
    justCompleted: Set<String>, justFailed: Set<String>,
    expiredDismissed: Set<String>, onExpiredDismissed: () -> Unit,
    showExpiredDialog: Boolean,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onTapSelect: () -> Unit = {}
) {
    var offsetX      by remember(todo.id) { mutableStateOf(0f) }
    var swiping      by remember(todo.id) { mutableStateOf(false) }
    val threshold     = 160f
    val animOffset   by animateFloatAsState(
        targetValue   = offsetX,
        animationSpec = if (swiping) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessMedium),
        label         = "swipe_offset"
    )

    val bgAlpha = (animOffset.absoluteValue / threshold).coerceIn(0f, 1f)
    val swipeBg = when {
        animOffset > 0f -> lerp(colors.card, Color(0xFF4CAF50), bgAlpha * 0.5f)
        animOffset < 0f -> lerp(colors.card, Color(0xFFFF4444), bgAlpha * 0.5f)
        else            -> colors.card
    }

    val isJustCompleted = todo.id in justCompleted
    val isJustFailed    = todo.id in justFailed

    val burstFraction by animateFloatAsState(if (isJustCompleted || isJustFailed) 1f else 0f, tween(400), label = "burst")
    val burstColor = when {
        isJustCompleted -> lerp(swipeBg, Color(0xFF4CAF50).copy(alpha = 0.25f), burstFraction)
        isJustFailed    -> lerp(swipeBg, Color(0xFFFF4444).copy(alpha = 0.25f), burstFraction)
        else            -> swipeBg
    }
    val cardScale by animateFloatAsState(if (isJustCompleted || isJustFailed) 1.04f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "scale")
    val doneFraction by animateFloatAsState(if (todo.isCompleted) 1f else 0f, tween(400), label = "done")
    val finalBg = when {
        isJustCompleted || isJustFailed -> burstColor
        else -> lerp(colors.card, colors.primary.copy(alpha = 0.07f), doneFraction)
    }

    var remainingSecs by remember(todo.id) { mutableStateOf(todo.remainingSeconds()) }
    val hasTimer     = todo.timerSeconds > 0L
    val timerRunning = todo.timerRunning()

    // FIX: no more "Did you complete X?" confirmation. Single tasks now
    // auto-complete once their timer hits zero, exactly like tasks inside
    // a group — no dialog, no extra tap. This also "catches up" tasks
    // whose timer ran out while you were on a different screen, so they
    // don't get stuck at 00:00 forever with no Reset button.
    //
    // Re-firing is prevented naturally by LaunchedEffect's own key
    // semantics: once onMarkComplete() succeeds and todo.isCompleted
    // flips to true, this effect's key tuple changes, so it relaunches —
    // sees timerRunning is now false — and returns immediately without
    // calling onMarkComplete() again. No extra "already fired" flag needed.
    LaunchedEffect(todo.id, todo.isCompleted, todo.isFailed, todo.timerStartedAt) {
        if (!timerRunning) return@LaunchedEffect
        while (true) {
            remainingSecs = todo.remainingSeconds()
            if (remainingSecs <= 0L) break
            delay(1_000L)
        }
        remainingSecs = 0L
        if (!todo.isCompleted && !todo.isFailed) {
            onMarkComplete()
        }
    }

    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        if (animOffset < 0f) {
            Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                Icon(Icons.Filled.Close, "Fail", tint = Color(0xFFFF4444).copy(alpha = bgAlpha), modifier = Modifier.size(28.dp))
            }
        }
        if (animOffset > 0f) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)) {
                Icon(Icons.Filled.Check, "Complete", tint = Color(0xFF4CAF50).copy(alpha = bgAlpha), modifier = Modifier.size(28.dp))
            }
        }

        Card(
            modifier  = Modifier.fillMaxWidth()
                .offset { IntOffset(animOffset.roundToInt(), 0) }
                .pointerInput(todo.id, todo.isCompleted, todo.isFailed, selectionMode) {
                    detectTapGestures(
                        onLongPress = { if (!selectionMode) onLongPress() },
                        onTap = { if (selectionMode) onTapSelect() }
                    )
                }
                .pointerInput(todo.id, todo.isCompleted, todo.isFailed, selectionMode) {
                    if (selectionMode || todo.isCompleted || todo.isFailed) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart  = { swiping = true },
                        onDragEnd    = {
                            swiping = false
                            when {
                                offsetX > threshold  -> { offsetX = 0f; onComplete() }
                                offsetX < -threshold -> { offsetX = 0f; onFail() }
                                else                  -> offsetX = 0f
                            }
                        },
                        onDragCancel = { swiping = false; offsetX = 0f }
                    ) { _, dragAmount -> offsetX = (offsetX + dragAmount).coerceIn(-threshold * 1.3f, threshold * 1.3f) }
                },
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = if (isSelected) colors.primary.copy(alpha = 0.16f) else finalBg),
            border    = if (isSelected) BorderStroke(1.5.dp, colors.primary) else null,
            elevation = CardDefaults.cardElevation(if (todo.isCompleted || todo.isFailed) 0.dp else 3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(CircleShape)
                            .background(if (isSelected) colors.primary else Color.Transparent)
                            .border(1.5.dp, if (isSelected) colors.primary else colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                val priorityColor = when (todo.priority) { 2 -> Color(0xFFFF4444); 1 -> Color(0xFFFFAA00); else -> Color.Transparent }
                if (todo.priority > 0) {
                    Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(priorityColor))
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(
                        when {
                            todo.isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.18f)
                            todo.isFailed    -> Color(0xFFFF4444).copy(alpha = 0.18f)
                            else             -> colors.border.copy(alpha = 0.3f)
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        todo.isCompleted -> Icon(Icons.Filled.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        todo.isFailed    -> Icon(Icons.Filled.Close, null, tint = Color(0xFFFF4444), modifier = Modifier.size(18.dp))
                        else             -> Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.textSecondary.copy(alpha = 0.4f)))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        todo.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = when { todo.isCompleted -> colors.textSecondary; todo.isFailed -> Color(0xFFFF4444).copy(alpha = 0.8f); else -> colors.text },
                            textDecoration = if (todo.isCompleted || todo.isFailed) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = if (todo.isCompleted || todo.isFailed) FontWeight.Normal else FontWeight.SemiBold
                        )
                    )
                    todo.createdAt?.let {
                        Text(SimpleDateFormat("MMM dd", Locale.getDefault()).format(it),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary.copy(alpha = 0.7f)))
                    }
                    if (hasTimer) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val expired   = todo.timerStartedAt > 0L && remainingSecs <= 0L && !todo.isCompleted && !todo.isFailed
                        val chipColor = when {
                            todo.isCompleted          -> Color(0xFF4CAF50)
                            todo.isFailed             -> Color(0xFFFF4444)
                            expired                   -> Color(0xFFFF4444)
                            todo.timerStartedAt == 0L -> colors.textSecondary
                            remainingSecs <= 60L      -> Color(0xFFFFAA00)
                            else                      -> colors.primary
                        }
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(chipColor.copy(alpha = 0.15f))
                                .border(1.dp, chipColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val timeStr = formatTime(
                                when {
                                    todo.isCompleted || todo.isFailed -> todo.timerSeconds
                                    todo.timerStartedAt == 0L         -> todo.timerSeconds
                                    expired                           -> 0L
                                    else                              -> remainingSecs
                                }
                            )
                            Text(
                                text = when {
                                    todo.isCompleted          -> "✓ $timeStr"
                                    todo.isFailed             -> "✗ $timeStr"
                                    expired                   -> "⏰ 00:00"
                                    todo.timerStartedAt == 0L -> "⏱ $timeStr"
                                    else                      -> "⏱ $timeStr"
                                },
                                color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                            )
                            if (todo.timerStartedAt == 0L && !todo.isCompleted && !todo.isFailed) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(colors.primary.copy(alpha = 0.2f))
                                        .clickable { onStartTimer() }.padding(horizontal = 5.dp, vertical = 1.dp)
                                ) { Text("START", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }

                if (todo.isCompleted || todo.isFailed) {
                    IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Refresh, "Reset", tint = colors.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", tint = colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TodoSectionHeader(label: String, count: Int, color: Color) {
    Row(modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp))
        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text("$count", style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold))
        }
    }
}

private fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}