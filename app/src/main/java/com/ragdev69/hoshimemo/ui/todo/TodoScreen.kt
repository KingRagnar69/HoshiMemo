package com.ragdev69.hoshimemo.ui.todo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import com.ragdev69.hoshimemo.ui.theme.HoshiColors
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.TodoViewModel
import kotlinx.coroutines.delay
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
    val todos            by viewModel.todos.collectAsStateWithLifecycle()
    val ungroupedPending by viewModel.ungroupedPending.collectAsStateWithLifecycle()
    val ungroupedDone    by viewModel.ungroupedCompleted.collectAsStateWithLifecycle()
    val ungroupedFailed  by viewModel.ungroupedFailed.collectAsStateWithLifecycle()
    val completedTodos   by viewModel.completedTodos.collectAsStateWithLifecycle()
    val groups           by viewModel.groups.collectAsStateWithLifecycle()
    val groupTasksMap    by viewModel.groupTasksMap.collectAsStateWithLifecycle()
    val expiredDismissed by viewModel.expiredDialogDismissed.collectAsStateWithLifecycle()
    val justCompleted    by viewModel.justCompletedIds.collectAsStateWithLifecycle()
    val justFailed       by viewModel.justFailedIds.collectAsStateWithLifecycle()

    // Add-task dialog state
    var showAddDialog    by remember { mutableStateOf(false) }
    var newTaskText      by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(0) }
    var timerH           by remember { mutableStateOf("") }
    var timerM           by remember { mutableStateOf("") }
    var timerS           by remember { mutableStateOf("") }
    var selectedGroupId  by remember { mutableStateOf("") }
    var selectedGroupName by remember { mutableStateOf("") }

    // Create-group dialog state
    var showGroupDialog  by remember { mutableStateOf(false) }
    var newGroupName     by remember { mutableStateOf("") }

    // Expanded group in list
    var expandedGroupId  by remember { mutableStateOf<String?>(null) }

    // ── Create group dialog ────────────────────────────────────
    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false; newGroupName = "" },
            containerColor   = colors.surface,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("New Group", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                OutlinedTextField(
                    value         = newGroupName,
                    onValueChange = { newGroupName = it },
                    placeholder   = { Text("Group name", color = colors.textSecondary) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = colors.primary,
                        unfocusedBorderColor    = colors.border,
                        focusedTextColor        = colors.text,
                        unfocusedTextColor      = colors.text,
                        cursorColor             = colors.primary,
                        focusedContainerColor   = colors.card,
                        unfocusedContainerColor = colors.card
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
                    shape  = RoundedCornerShape(12.dp),
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

    // ── Add task dialog ────────────────────────────────────────
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
                    Text(stringResource(R.string.todo_new_task), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    // Task name
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

                    // Priority
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

                    // Timer
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Timer, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                        Text("TIMER (optional)", color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
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
                    Spacer(modifier = Modifier.height(16.dp))

                    // Group picker (only shown if groups exist)
                    if (groups.isNotEmpty()) {
                        Text("GROUP (optional)", color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        // None chip + group chips
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // None
                            val noneSelected = selectedGroupId.isEmpty()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(if (noneSelected) colors.primary.copy(alpha = 0.18f) else colors.card)
                                    .border(1.5.dp, if (noneSelected) colors.primary else colors.border, RoundedCornerShape(50.dp))
                                    .clickable { selectedGroupId = ""; selectedGroupName = "" }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) { Text("None", color = if (noneSelected) colors.primary else colors.textSecondary, fontSize = 13.sp) }

                            groups.forEach { g ->
                                val gId   = g["groupId"] ?: ""
                                val gName = g["groupName"] ?: ""
                                val sel   = selectedGroupId == gId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (sel) colors.primary.copy(alpha = 0.18f) else colors.card)
                                        .border(1.5.dp, if (sel) colors.primary else colors.border, RoundedCornerShape(50.dp))
                                        .clickable { selectedGroupId = gId; selectedGroupName = gName }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) { Text(gName, color = if (sel) colors.primary else colors.textSecondary, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                            }
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

    // ── Main layout ────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header + progress
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.surface, colors.background)))
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.todo_title),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold, color = colors.text),
                            modifier = Modifier.weight(1f))
                        // Create group button — styled pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(colors.primary.copy(alpha = 0.13f))
                                .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                                .clickable { showGroupDialog = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Filled.CreateNewFolder, null,
                                    tint = colors.primary, modifier = Modifier.size(15.dp))
                                Text("New Group", color = colors.primary,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    val done  = completedTodos.size
                    val total = todos.filter { !it.isFailed }.size
                    val progress = if (total > 0) done.toFloat() / total else 0f
                    val animProg by animateFloatAsState(progress, tween(600, easing = EaseOutCubic), label = "prog")
                    // Only show progress bar once at least one task is completed
                    if (done > 0 && total > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(stringResource(R.string.todo_progress, done, total),
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary))
                            Text("${(animProg * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(color = colors.primary, fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.border)) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animProg)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(colors.primary, colors.accent))))
                        }
                    }
                }
            }

            // Swipe hint
            if (ungroupedPending.isNotEmpty() || groups.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("← Fail", style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFF4444).copy(alpha = 0.7f)))
                    Text("Complete →", style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF4CAF50).copy(alpha = 0.7f)))
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
                        Text(stringResource(R.string.todo_empty_title),
                            style = MaterialTheme.typography.titleLarge.copy(color = colors.text, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stringResource(R.string.todo_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Groups section ─────────────────────────
                    if (groups.isNotEmpty()) {
                        item {
                            TodoSectionHeader(label = "GROUPS", count = groups.size, color = colors.accent)
                        }
                        items(groups, key = { it["groupId"] ?: "" }) { group ->
                            val gId      = group["groupId"] ?: ""
                            val gName    = group["groupName"] ?: ""
                            val expanded = expandedGroupId == gId
                            // Read from the single map — no StateFlow created per item
                            val groupTasks = groupTasksMap[gId] ?: emptyList()

                            GroupCard(
                                groupName = gName,
                                tasks     = groupTasks,
                                expanded  = expanded,
                                colors    = colors,
                                onToggleExpand = { expandedGroupId = if (expanded) null else gId },
                                onDeleteGroup  = { viewModel.deleteGroup(gId) },
                                onResetGroup   = { viewModel.resetGroup(gId) },
                                onAddTodoToGroup = {
                                    selectedGroupId   = gId
                                    selectedGroupName = gName
                                    showAddDialog     = true
                                },
                                onStartGroup = { viewModel.startGroupTimer(gId) },
                                onComplete    = { viewModel.completeTodo(it) },
                                onFail        = { viewModel.failTodo(it) },
                                onDelete      = { viewModel.deleteTodo(it) },
                                onReset       = { viewModel.resetTodo(it) },
                                onStartTimer  = { viewModel.startTimer(it) },
                                onMarkComplete = { viewModel.toggleTodo(it, true) },
                                justCompleted  = justCompleted,
                                justFailed     = justFailed,
                                expiredDismissed    = expiredDismissed,
                                onExpiredDismissed  = { viewModel.markExpiredDialogDismissed(it) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }

                    // ── Ungrouped pending ──────────────────────
                    if (ungroupedPending.isNotEmpty()) {
                        item { TodoSectionHeader(stringResource(R.string.todo_section_pending), ungroupedPending.size, colors.textSecondary) }
                        items(ungroupedPending, key = { it.id }) { todo ->
                            AnimatedVisibility(visible = true, enter = fadeIn() + slideInHorizontally()) {
                                SwipeableTodoCard(
                                    todo             = todo,
                                    colors           = colors,
                                    onComplete       = { viewModel.completeTodo(todo.id) },
                                    onFail           = { viewModel.failTodo(todo.id) },
                                    onDelete         = { viewModel.deleteTodo(todo.id) },
                                    onReset          = { viewModel.resetTodo(todo.id) },
                                    onStartTimer     = { viewModel.startTimer(todo.id) },
                                    onMarkComplete   = { viewModel.toggleTodo(todo.id, true) },
                                    justCompleted    = justCompleted,
                                    justFailed       = justFailed,
                                    expiredDismissed = expiredDismissed,
                                    onExpiredDismissed = { viewModel.markExpiredDialogDismissed(todo.id) }
                                )
                            }
                        }
                    }

                    // ── Ungrouped completed ────────────────────
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
                                onExpiredDismissed = { viewModel.markExpiredDialogDismissed(todo.id) }
                            )
                        }
                    }

                    // ── Ungrouped failed ───────────────────────
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
                                onExpiredDismissed = { viewModel.markExpiredDialogDismissed(todo.id) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                // Always clear group selection when opening from FAB
                selectedGroupId   = ""
                selectedGroupName = ""
                showAddDialog     = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 24.dp),
            containerColor = colors.primary, contentColor = Color.White, shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) { Icon(Icons.Filled.Add, stringResource(R.string.todo_add), modifier = Modifier.size(26.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// Group card
// ─────────────────────────────────────────────────────────────
@Composable
private fun GroupCard(
    groupName: String, tasks: List<TodoItem>, expanded: Boolean,
    colors: HoshiColors,
    onToggleExpand: () -> Unit,
    onDeleteGroup: () -> Unit,
    onResetGroup: () -> Unit,
    onAddTodoToGroup: () -> Unit,
    onStartGroup: () -> Unit,  // starts the first pending task's timer
    onComplete: (String) -> Unit, onFail: (String) -> Unit,
    onDelete: (String) -> Unit, onReset: (String) -> Unit,
    onStartTimer: (String) -> Unit, onMarkComplete: (String) -> Unit,
    justCompleted: Set<String>, justFailed: Set<String>,
    expiredDismissed: Set<String>, onExpiredDismissed: (String) -> Unit
) {
    val pending   = tasks.filter { !it.isCompleted && !it.isFailed }
    val done      = tasks.filter { it.isCompleted }
    val failed    = tasks.filter { it.isFailed }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group header row
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
                    Text(groupName, color = colors.text, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall)
                    Text("${done.size}/${tasks.size} done · ${failed.size} failed",
                        color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                // Progress mini bar
                Box(modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.border)) {
                    if (tasks.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxHeight()
                            .fillMaxWidth(done.size.toFloat() / tasks.size)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(listOf(colors.primary, colors.accent))))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = colors.textSecondary, modifier = Modifier.size(20.dp)
                )
                // Add task to group
                IconButton(onClick = onAddTodoToGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, "Add task", tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                // START — starts the first pending task's timer
                val firstPending = tasks.filter { !it.isCompleted && !it.isFailed }
                    .minByOrNull { it.orderInGroup }
                val canStart = firstPending != null &&
                        firstPending.timerSeconds > 0L &&
                        firstPending.timerStartedAt == 0L
                if (canStart) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary.copy(alpha = 0.18f))
                            .clickable { onStartGroup() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("START", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                // Reset all tasks in group
                IconButton(onClick = onResetGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, "Reset group", tint = colors.primary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
                // Delete group
                IconButton(onClick = onDeleteGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            // Expanded task list
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { todo ->
                        val othersDone = tasks
                            .filter { it.id != todo.id }
                            .all { it.isCompleted || it.isFailed }
                        SwipeableTodoCard(
                            todo = todo, colors = colors,
                            onComplete = { onComplete(todo.id) }, onFail = { onFail(todo.id) },
                            onDelete = { onDelete(todo.id) }, onReset = { onReset(todo.id) },
                            onStartTimer = { onStartTimer(todo.id) },
                            onMarkComplete = { onMarkComplete(todo.id) },
                            justCompleted = justCompleted, justFailed = justFailed,
                            expiredDismissed = expiredDismissed,
                            onExpiredDismissed = { onExpiredDismissed(todo.id) },
                            allGroupTasksDone = othersDone,
                            isInsideGroup = true
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Swipeable todo card
// ─────────────────────────────────────────────────────────────
@Composable
fun SwipeableTodoCard(
    todo: TodoItem, colors: HoshiColors,
    onComplete: () -> Unit, onFail: () -> Unit,
    onDelete: () -> Unit, onReset: () -> Unit,
    onStartTimer: () -> Unit, onMarkComplete: () -> Unit,
    justCompleted: Set<String>, justFailed: Set<String>,
    expiredDismissed: Set<String>, onExpiredDismissed: () -> Unit,
    allGroupTasksDone: Boolean = true,
    isInsideGroup: Boolean = false  // hides START button; group header has its own
) {
    // Swipe state
    var offsetX      by remember(todo.id) { mutableStateOf(0f) }
    var swiping      by remember(todo.id) { mutableStateOf(false) }
    val threshold     = 160f   // px before action triggers
    val animOffset   by animateFloatAsState(
        targetValue   = offsetX,
        animationSpec = if (swiping) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessMedium),
        label         = "swipe_offset"
    )

    // Swipe background color
    val bgAlpha = (animOffset.absoluteValue / threshold).coerceIn(0f, 1f)
    val swipeBg = when {
        animOffset > 0f -> lerp(colors.card, Color(0xFF4CAF50), bgAlpha * 0.5f)  // green = complete
        animOffset < 0f -> lerp(colors.card, Color(0xFFFF4444), bgAlpha * 0.5f)  // red = fail
        else            -> colors.card
    }

    // Animation from ViewModel
    val isJustCompleted = todo.id in justCompleted
    val isJustFailed    = todo.id in justFailed

    val burstFraction by animateFloatAsState(
        targetValue = if (isJustCompleted || isJustFailed) 1f else 0f,
        animationSpec = tween(400), label = "burst"
    )
    val burstColor = when {
        isJustCompleted -> lerp(swipeBg, Color(0xFF4CAF50).copy(alpha = 0.25f), burstFraction)
        isJustFailed    -> lerp(swipeBg, Color(0xFFFF4444).copy(alpha = 0.25f), burstFraction)
        else            -> swipeBg
    }
    val cardScale by animateFloatAsState(
        targetValue = if (isJustCompleted || isJustFailed) 1.04f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "scale"
    )
    val doneFraction by animateFloatAsState(
        targetValue = if (todo.isCompleted) 1f else 0f,
        animationSpec = tween(400), label = "done"
    )
    val finalBg = when {
        isJustCompleted || isJustFailed -> burstColor
        else -> lerp(colors.card, colors.primary.copy(alpha = 0.07f), doneFraction)
    }

    // Timer state
    var remainingSecs by remember(todo.id) { mutableStateOf(todo.remainingSeconds()) }
    val hasTimer       = todo.timerSeconds > 0L
    val timerRunning   = todo.timerRunning()

    LaunchedEffect(todo.id, todo.isCompleted, todo.isFailed, todo.timerStartedAt) {
        if (!timerRunning) return@LaunchedEffect
        while (true) {
            remainingSecs = todo.remainingSeconds()
            if (remainingSecs <= 0L) break
            delay(1_000L)
        }
        remainingSecs = 0L
    }

    val showExpiredDialog = hasTimer && todo.timerStartedAt > 0L &&
            remainingSecs <= 0L && !todo.isCompleted && !todo.isFailed &&
            todo.id !in expiredDismissed &&
            allGroupTasksDone  // for grouped tasks, only show when all others are done too

    if (showExpiredDialog) {
        AlertDialog(
            onDismissRequest = { onExpiredDismissed() },
            containerColor   = colors.surface,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("⏰  Time's up!", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text  = { Text("Did you complete \"${todo.title}\"?", color = colors.textSecondary) },
            confirmButton = {
                Button(onClick = { onExpiredDismissed(); onMarkComplete() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yes, done!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onExpiredDismissed() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.border)) {
                    Icon(Icons.Filled.Close, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Not yet", color = colors.textSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
        // Swipe hint icons behind the card
    ) {
        // Behind — fail icon (left side)
        if (animOffset < 0f) {
            Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                Icon(Icons.Filled.Close, "Fail", tint = Color(0xFFFF4444).copy(alpha = bgAlpha),
                    modifier = Modifier.size(28.dp))
            }
        }
        // Behind — complete icon (right side)
        if (animOffset > 0f) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)) {
                Icon(Icons.Filled.Check, "Complete", tint = Color(0xFF4CAF50).copy(alpha = bgAlpha),
                    modifier = Modifier.size(28.dp))
            }
        }

        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animOffset.roundToInt(), 0) }
                .pointerInput(todo.id, todo.isCompleted, todo.isFailed) {
                    if (todo.isCompleted || todo.isFailed) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart  = { swiping = true },
                        onDragEnd    = {
                            swiping = false
                            when {
                                offsetX > threshold  -> { offsetX = 0f; onComplete() }
                                offsetX < -threshold -> { offsetX = 0f; onFail() }
                                else                 -> offsetX = 0f
                            }
                        },
                        onDragCancel = { swiping = false; offsetX = 0f }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-threshold * 1.3f, threshold * 1.3f)
                    }
                },
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = finalBg),
            elevation = CardDefaults.cardElevation(if (todo.isCompleted || todo.isFailed) 0.dp else 3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 6.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority bar
                val priorityColor = when (todo.priority) {
                    2 -> Color(0xFFFF4444); 1 -> Color(0xFFFFAA00); else -> Color.Transparent
                }
                if (todo.priority > 0) {
                    Box(modifier = Modifier.width(4.dp).height(40.dp)
                        .clip(RoundedCornerShape(2.dp)).background(priorityColor))
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Status badge (✓ / ✗ / pending dot)
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

                // Title + date + timer
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        todo.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = when {
                                todo.isCompleted -> colors.textSecondary
                                todo.isFailed    -> Color(0xFFFF4444).copy(alpha = 0.8f)
                                else             -> colors.text
                            },
                            textDecoration = if (todo.isCompleted || todo.isFailed)
                                TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = if (todo.isCompleted || todo.isFailed)
                                FontWeight.Normal else FontWeight.SemiBold
                        )
                    )
                    todo.createdAt?.let {
                        Text(SimpleDateFormat("MMM dd", Locale.getDefault()).format(it),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary.copy(alpha = 0.7f)))
                    }
                    // Timer chip
                    if (hasTimer) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val expired   = todo.timerStartedAt > 0L && remainingSecs <= 0L && !todo.isCompleted && !todo.isFailed
                        val chipColor = when {
                            todo.isCompleted          -> Color(0xFF4CAF50)
                            todo.isFailed             -> Color(0xFFFF4444)
                            expired                   -> Color(0xFFFF4444)
                            todo.timerStartedAt == 0L -> colors.textSecondary  // not started
                            remainingSecs <= 60L      -> Color(0xFFFFAA00)
                            else                      -> colors.primary
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipColor.copy(alpha = 0.15f))
                                .border(1.dp, chipColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            // Manual start button — only for ungrouped tasks with timer not started
                            if (todo.timerStartedAt == 0L && !todo.isCompleted && !todo.isFailed && !isInsideGroup) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.primary.copy(alpha = 0.2f))
                                        .clickable { onStartTimer() }
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("START", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Reset (only for completed or failed) + Delete
                if (todo.isCompleted || todo.isFailed) {
                    IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Refresh, "Reset", tint = colors.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", tint = colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────
@Composable
private fun TodoSectionHeader(label: String, count: Int, color: Color) {
    Row(modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(
            color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp))
        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)) {
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