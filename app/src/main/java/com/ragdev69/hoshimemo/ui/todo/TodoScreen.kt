package com.ragdev69.hoshimemo.ui.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
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

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val colors              = LocalHoshiColors.current
    val todos               by viewModel.todos.collectAsStateWithLifecycle()
    val pending             by viewModel.pendingTodos.collectAsStateWithLifecycle()
    val completed           by viewModel.completedTodos.collectAsStateWithLifecycle()
    val expiredDismissed    by viewModel.expiredDialogDismissed.collectAsStateWithLifecycle()

    var showAddDialog    by remember { mutableStateOf(false) }
    var newTaskText      by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(0) }
    // Timer input — separate hours / minutes / seconds fields
    var timerHours   by remember { mutableStateOf("") }
    var timerMinutes by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableStateOf("") }

    // ── Add-task dialog ────────────────────────────────────────
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newTaskText = ""
                timerHours = ""; timerMinutes = ""; timerSeconds = ""
                selectedPriority = 0
            },
            containerColor = colors.surface,
            shape          = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null,
                            tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.todo_new_task),
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    // Task name
                    OutlinedTextField(
                        value         = newTaskText,
                        onValueChange = { newTaskText = it },
                        placeholder   = { Text(stringResource(R.string.todo_task_hint), color = colors.textSecondary) },
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Priority chips
                    Text(
                        stringResource(R.string.todo_priority),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(stringResource(R.string.todo_priority_normal), 0, colors.textSecondary),
                            Triple(stringResource(R.string.todo_priority_medium), 1, Color(0xFFFFAA00)),
                            Triple(stringResource(R.string.todo_priority_high),   2, Color(0xFFFF4444))
                        ).forEach { (label, p, chipColor) ->
                            val sel = selectedPriority == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) chipColor.copy(alpha = 0.18f) else colors.card)
                                    .border(1.dp, if (sel) chipColor else colors.border, RoundedCornerShape(10.dp))
                                    .clickable { selectedPriority = p }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    label,
                                    color      = if (sel) chipColor else colors.textSecondary,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize   = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timer — H / M / S
                    Text(
                        "TIMER (optional)",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        TimerField(
                            value     = timerHours,
                            onValueChange = { if (it.length <= 2) timerHours = it.filter(Char::isDigit) },
                            label     = "h",
                            colors    = colors,
                            modifier  = Modifier.weight(1f)
                        )
                        TimerField(
                            value     = timerMinutes,
                            onValueChange = { if (it.length <= 2) timerMinutes = it.filter(Char::isDigit) },
                            label     = "m",
                            colors    = colors,
                            modifier  = Modifier.weight(1f)
                        )
                        TimerField(
                            value     = timerSeconds,
                            onValueChange = { if (it.length <= 2) timerSeconds = it.filter(Char::isDigit) },
                            label     = "s",
                            colors    = colors,
                            modifier  = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            val totalSeconds =
                                (timerHours.toLongOrNull()   ?: 0L) * 3600L +
                                        (timerMinutes.toLongOrNull() ?: 0L) * 60L   +
                                        (timerSeconds.toLongOrNull() ?: 0L)
                            viewModel.addTodo(newTaskText.trim(), selectedPriority, totalSeconds)
                            newTaskText = ""
                            timerHours = ""; timerMinutes = ""; timerSeconds = ""
                            selectedPriority = 0
                            showAddDialog = false
                        }
                    },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(stringResource(R.string.todo_add), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newTaskText = ""
                    timerHours = ""; timerMinutes = ""; timerSeconds = ""
                    selectedPriority = 0
                }) {
                    Text(stringResource(R.string.todo_cancel), color = colors.textSecondary)
                }
            }
        )
    }

    // ── Main layout ────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colors.surface, colors.background)))
                    .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 16.dp)
            ) {
                Column {
                    Text(
                        stringResource(R.string.todo_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = colors.text
                        )
                    )
                    if (todos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = completed.size.toFloat() / todos.size
                        val animatedProgress by animateFloatAsState(
                            targetValue  = progress,
                            animationSpec = tween(600, easing = EaseOutCubic),
                            label        = "progress"
                        )
                        Row(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment   = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.todo_progress, completed.size, todos.size),
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color      = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.border)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Brush.horizontalGradient(listOf(colors.primary, colors.accent)))
                            )
                        }
                    }
                }
            }

            if (todos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Text("✅", fontSize = 44.sp) }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.todo_empty_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color      = colors.text,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.todo_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pending.isNotEmpty()) {
                        item {
                            TodoSectionHeader(
                                label = stringResource(R.string.todo_section_pending),
                                count = pending.size,
                                color = colors.textSecondary
                            )
                        }
                        items(pending, key = { it.id }) { todo ->
                            AnimatedVisibility(visible = true, enter = fadeIn() + slideInHorizontally()) {
                                TodoCard(
                                    todo                     = todo,
                                    colors                   = colors,
                                    onToggle                 = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                    onDelete                 = { viewModel.deleteTodo(todo.id) },
                                    onMarkComplete           = { viewModel.toggleTodo(todo.id, true) },
                                    expiredDialogDismissed   = expiredDismissed,
                                    onExpiredDialogDismissed = { viewModel.markExpiredDialogDismissed(todo.id) }
                                )
                            }
                        }
                    }
                    if (completed.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            TodoSectionHeader(
                                label = stringResource(R.string.todo_section_completed),
                                count = completed.size,
                                color = colors.primary
                            )
                        }
                        items(completed, key = { it.id }) { todo ->
                            TodoCard(
                                todo                     = todo,
                                colors                   = colors,
                                onToggle                 = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                onDelete                 = { viewModel.deleteTodo(todo.id) },
                                onMarkComplete           = { viewModel.toggleTodo(todo.id, true) },
                                expiredDialogDismissed   = expiredDismissed,
                                onExpiredDialogDismissed = { viewModel.markExpiredDialogDismissed(todo.id) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick        = { showAddDialog = true },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            containerColor = colors.primary,
            contentColor   = Color.White,
            shape          = CircleShape,
            elevation      = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.todo_add),
                modifier = Modifier.size(26.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: HoshiColors,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text("0", color = colors.textSecondary) },
        suffix        = { Text(label, color = colors.textSecondary, fontSize = 12.sp) },
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape  = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = colors.primary,
            unfocusedBorderColor    = colors.border,
            focusedTextColor        = colors.text,
            unfocusedTextColor      = colors.text,
            cursorColor             = colors.primary,
            focusedContainerColor   = colors.card,
            unfocusedContainerColor = colors.card
        ),
        modifier = modifier
    )
}

@Composable
private fun TodoSectionHeader(label: String, count: Int, color: Color) {
    Row(
        modifier            = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color       = color,
                fontWeight  = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = color,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/** Format seconds as H:MM:SS (hours omitted when 0). */
private fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else              "%02d:%02d".format(m, s)
}

// ─────────────────────────────────────────────────────────────────────────────
// TodoCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodoCard(
    todo: TodoItem,
    colors: HoshiColors,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMarkComplete: () -> Unit,
    expiredDialogDismissed: Set<String>,
    onExpiredDialogDismissed: () -> Unit
) {
    val sdf           = SimpleDateFormat("MMM dd", Locale.getDefault())
    val priorityColor = when (todo.priority) {
        2    -> Color(0xFFFF4444)
        1    -> Color(0xFFFFAA00)
        else -> Color.Transparent
    }

    // ── Optimistic local checkbox state ───────────────────────
    // FIX: Checkbox(checked = todo.isCompleted) waits for Firestore to confirm
    // the write (~300-500ms) before showing the tick. The animation never fires
    // because the card moves out of the pending list before the burst can play.
    // Fix: flip localCompleted instantly on tap; sync it back when Firestore
    // responds (todo.isCompleted changes) so they stay in agreement.
    var localCompleted by remember(todo.id) { mutableStateOf(todo.isCompleted) }
    LaunchedEffect(todo.isCompleted) { localCompleted = todo.isCompleted }

    // ── Completion animation state ─────────────────────────────
    // previousCompleted intentionally starts as false — NOT todo.isCompleted.
    // If it started as todo.isCompleted, then when isCompleted flips true the
    // LaunchedEffect sees previousCompleted==true already and skips the burst.
    var previousCompleted by remember(todo.id) { mutableStateOf(false) }
    var justCompleted     by remember(todo.id) { mutableStateOf(false) }

    // Key on BOTH todo.id and localCompleted so the effect reruns on every flip
    LaunchedEffect(todo.id, localCompleted) {
        if (localCompleted && !previousCompleted) {
            justCompleted = true
            delay(600)
            justCompleted = false
        }
        previousCompleted = localCompleted
    }

    val burstFraction by animateFloatAsState(
        targetValue   = if (justCompleted) 1f else 0f,
        animationSpec = tween(400),
        label         = "burst"
    )
    val doneFraction by animateFloatAsState(
        targetValue   = if (localCompleted) 1f else 0f,
        animationSpec = tween(400),
        label         = "done"
    )
    val cardBg     = when {
        justCompleted -> lerp(colors.card, colors.primary.copy(alpha = 0.18f), burstFraction)
        else          -> lerp(colors.card, colors.primary.copy(alpha = 0.07f), doneFraction)
    }
    val cardScale  by animateFloatAsState(
        targetValue   = if (justCompleted) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label         = "card_scale"
    )
    val checkScale by animateFloatAsState(
        targetValue   = if (justCompleted) 1.35f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label         = "check_scale"
    )

    // ── Timer countdown — persists across navigation ──────────
    // remainingSeconds() is calculated from absolute timerStartedAt so it
    // never resets when you navigate away and come back.
    var remainingSecs by remember(todo.id) { mutableStateOf(todo.remainingSeconds()) }
    val hasTimer      = todo.timerSeconds > 0L && todo.timerStartedAt > 0L

    // Tick every second while the timer is running
    LaunchedEffect(todo.id, todo.isCompleted) {
        if (!hasTimer || todo.isCompleted) return@LaunchedEffect
        while (true) {
            remainingSecs = todo.remainingSeconds()
            if (remainingSecs <= 0L) break
            delay(1_000L)
        }
        // Re-read after loop ends to make sure it shows 0
        remainingSecs = 0L
    }

    // ── Timer-expired completion dialog ───────────────────────
    // showExpiredDialog is derived purely from ViewModel state — no remember needed.
    // Dialog shows if: timer expired AND not completed AND not already dismissed.
    val showExpiredDialog = hasTimer &&
            remainingSecs <= 0L &&
            !todo.isCompleted &&
            todo.id !in expiredDialogDismissed

    if (showExpiredDialog) {
        AlertDialog(
            onDismissRequest = { onExpiredDialogDismissed() },
            containerColor   = colors.surface,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "⏰  Time's up!",
                    color      = colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            },
            text = {
                Text(
                    "Did you complete \"${todo.title}\"?",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { onExpiredDialogDismissed(); onMarkComplete() },
                    shape   = RoundedCornerShape(12.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yes, done!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onExpiredDialogDismissed() },
                    shape   = RoundedCornerShape(12.dp),
                    border  = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null,
                        tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Not yet", color = colors.textSecondary)
                }
            }
        )
    }

    // ── Card UI ────────────────────────────────────────────────
    Card(
        modifier  = Modifier.fillMaxWidth().scale(cardScale),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (localCompleted) 0.dp else 3.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority bar
            if (todo.priority > 0) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(priorityColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Checkbox — uses localCompleted for instant visual feedback.
            // onToggle() fires the Firestore update in the background.
            Box(modifier = Modifier.scale(checkScale)) {
                Checkbox(
                    checked         = localCompleted,
                    onCheckedChange = {
                        localCompleted = !localCompleted   // instant tick/untick
                        onToggle()                         // async Firestore update
                    },
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = colors.primary,
                        uncheckedColor = colors.textSecondary,
                        checkmarkColor = Color.White
                    )
                )
            }

            // Title + date + timer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text  = todo.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        // Use localCompleted so strikethrough + dimming happen instantly
                        color          = if (localCompleted) colors.textSecondary else colors.text,
                        textDecoration = if (localCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight     = if (localCompleted) FontWeight.Normal else FontWeight.SemiBold
                    )
                )
                todo.createdAt?.let {
                    Text(
                        text  = sdf.format(it),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary.copy(alpha = 0.7f)
                        )
                    )
                }
                // Timer chip
                if (hasTimer) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val timerExpired = remainingSecs <= 0L && !todo.isCompleted
                    val chipColor    = when {
                        todo.isCompleted -> colors.primary
                        timerExpired     -> Color(0xFFFF4444)
                        remainingSecs <= 60L -> Color(0xFFFFAA00)
                        else             -> colors.primary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor.copy(alpha = 0.15f))
                            .border(1.dp, chipColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text  = when {
                                todo.isCompleted -> "✓ " + formatTime(todo.timerSeconds)
                                timerExpired     -> "⏰ 00:00"
                                else             -> "⏱ " + formatTime(remainingSecs)
                            },
                            color      = chipColor,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Done badge — uses localCompleted so it appears instantly on tap
            AnimatedVisibility(
                visible = localCompleted,
                enter   = scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) + fadeIn(tween(200)),
                exit    = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint     = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint     = colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}