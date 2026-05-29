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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.data.model.TodoItem
import com.ragdev69.hoshimemo.ui.theme.HoshiColors
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val colors    = LocalHoshiColors.current
    val todos     by viewModel.todos.collectAsStateWithLifecycle()
    val pending   by viewModel.pendingTodos.collectAsStateWithLifecycle()
    val completed by viewModel.completedTodos.collectAsStateWithLifecycle()

    var showAddDialog    by remember { mutableStateOf(false) }
    var newTaskText      by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(0) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newTaskText = "" },
            containerColor   = colors.surface,
            shape            = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint     = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "New Task",
                        color      = colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value         = newTaskText,
                        onValueChange = { newTaskText = it },
                        placeholder   = {
                            Text("What needs to be done?", color = colors.textSecondary)
                        },
                        singleLine = true,
                        shape      = RoundedCornerShape(14.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
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
                    Text(
                        "PRIORITY",
                        color         = colors.textSecondary,
                        style         = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Normal", 0, colors.textSecondary),
                            Triple("Medium", 1, Color(0xFFFFAA00)),
                            Triple("High",   2, Color(0xFFFF4444))
                        ).forEach { (label, p, chipColor) ->
                            val sel = selectedPriority == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (sel) chipColor.copy(alpha = 0.18f) else colors.card
                                    )
                                    .border(
                                        1.dp,
                                        if (sel) chipColor else colors.border,
                                        RoundedCornerShape(10.dp)
                                    )
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            viewModel.addTodo(newTaskText.trim(), selectedPriority)
                            newTaskText      = ""
                            selectedPriority = 0
                            showAddDialog    = false
                        }
                    },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Add Task", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newTaskText = "" }) {
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

            // ── Header ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(colors.surface, colors.background))
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 16.dp)
            ) {
                Column {
                    Text(
                        "My Tasks",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = colors.text
                        )
                    )
                    if (todos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = completed.size.toFloat() / todos.size
                        val animatedProgress by animateFloatAsState(
                            targetValue   = progress,
                            animationSpec = tween(600, easing = EaseOutCubic),
                            label         = "progress"
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "${completed.size} of ${todos.size} done",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = colors.textSecondary
                                )
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
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(colors.primary, colors.accent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // ── Empty State ───────────────────────────────────
            if (todos.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✅", fontSize = 44.sp)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "All clear!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color      = colors.text,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Tap + to add your first task",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 8.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pending.isNotEmpty()) {
                        item {
                            TodoSectionHeader(
                                label  = "PENDING",
                                count  = pending.size,
                                color  = colors.textSecondary,
                                colors = colors
                            )
                        }
                        items(pending, key = { it.id }) { todo ->
                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn() + slideInHorizontally()
                            ) {
                                TodoCard(
                                    todo     = todo,
                                    colors   = colors,
                                    onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                    onDelete = { viewModel.deleteTodo(todo.id) }
                                )
                            }
                        }
                    }
                    if (completed.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            TodoSectionHeader(
                                label  = "COMPLETED",
                                count  = completed.size,
                                color  = colors.primary,
                                colors = colors
                            )
                        }
                        items(completed, key = { it.id }) { todo ->
                            TodoCard(
                                todo     = todo,
                                colors   = colors,
                                onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                onDelete = { viewModel.deleteTodo(todo.id) }
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
            Icon(Icons.Filled.Add, contentDescription = "Add Task", modifier = Modifier.size(26.dp))
        }
    }
}

// Renamed to TodoSectionHeader to avoid any conflict
@Composable
private fun TodoSectionHeader(
    label: String,
    count: Int,
    color: Color,
    colors: HoshiColors
) {
    Row(
        modifier              = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color         = color,
                fontWeight    = FontWeight.Bold,
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

// Renamed to TodoCard to avoid any conflict
@Composable
private fun TodoCard(
    todo: TodoItem,
    colors: HoshiColors,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())

    val priorityColor = when (todo.priority) {
        2    -> Color(0xFFFF4444)
        1    -> Color(0xFFFFAA00)
        else -> Color.Transparent
    }

    var previousCompleted by remember(todo.id) { mutableStateOf(todo.isCompleted) }
    var justCompleted     by remember(todo.id) { mutableStateOf(false) }

    LaunchedEffect(todo.isCompleted) {
        if (todo.isCompleted && !previousCompleted) {
            justCompleted = true
            kotlinx.coroutines.delay(600)
            justCompleted = false
        }
        previousCompleted = todo.isCompleted
    }

    // Use lerp instead of animateColorAsState to avoid the import issue
    val burstFraction by animateFloatAsState(
        targetValue   = if (justCompleted) 1f else 0f,
        animationSpec = tween(400),
        label         = "burst"
    )
    val doneFraction by animateFloatAsState(
        targetValue   = if (todo.isCompleted) 1f else 0f,
        animationSpec = tween(400),
        label         = "done"
    )

    val cardBg = when {
        justCompleted -> lerp(colors.card, colors.primary.copy(alpha = 0.18f), burstFraction)
        else          -> lerp(colors.card, colors.primary.copy(alpha = 0.07f), doneFraction)
    }

    val cardScale by animateFloatAsState(
        targetValue   = if (justCompleted) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "card_scale"
    )

    val checkScale by animateFloatAsState(
        targetValue   = if (justCompleted) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .scale(cardScale),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (todo.isCompleted) 0.dp else 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Box(modifier = Modifier.scale(checkScale)) {
                Checkbox(
                    checked         = todo.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = colors.primary,
                        uncheckedColor = colors.textSecondary,
                        checkmarkColor = Color.White
                    )
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text  = todo.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color          = if (todo.isCompleted) colors.textSecondary else colors.text,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough
                        else TextDecoration.None,
                        fontWeight     = if (todo.isCompleted) FontWeight.Normal
                        else FontWeight.SemiBold
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
            }

            AnimatedVisibility(
                visible = todo.isCompleted,
                enter   = scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                        + fadeIn(tween(200)),
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

            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
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