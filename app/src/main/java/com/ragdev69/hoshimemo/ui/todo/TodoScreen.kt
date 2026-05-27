package com.ragdev69.hoshimemo.ui.todo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.data.model.TodoItem
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
            title = {
                Text("New Task ✅", color = colors.text, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value         = newTaskText,
                        onValueChange = { newTaskText = it },
                        label         = {
                            Text("What needs to be done?", color = colors.textSecondary)
                        },
                        singleLine = true,
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor     = colors.text,
                            unfocusedTextColor   = colors.text,
                            cursorColor          = colors.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Priority", color = colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Normal" to 0, "Medium" to 1, "High" to 2).forEach { (label, p) ->
                            FilterChip(
                                selected = selectedPriority == p,
                                onClick  = { selectedPriority = p },
                                label    = { Text(label) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor     = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTaskText.isNotBlank()) {
                        viewModel.addTodo(newTaskText.trim(), selectedPriority)
                        newTaskText      = ""
                        selectedPriority = 0
                        showAddDialog    = false
                    }
                }) {
                    Text("Add", color = colors.primary, fontWeight = FontWeight.Bold)
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

            Column(
                modifier = Modifier.padding(
                    start = 20.dp, end = 20.dp, top = 52.dp, bottom = 12.dp
                )
            ) {
                Text(
                    "To-Do ✅",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold, color = colors.text
                    )
                )
                if (todos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = completed.size.toFloat() / todos.size
                    Column {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${pending.size} remaining",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = colors.textSecondary
                                )
                            )
                            Text(
                                "${completed.size} done",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = colors.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress   = { progress },
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color      = colors.primary,
                            trackColor = colors.border
                        )
                    }
                }
            }

            if (todos.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 60.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No tasks yet!",
                            style = MaterialTheme.typography.titleMedium.copy(color = colors.text)
                        )
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
                        start = 12.dp, end = 12.dp, top = 4.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (pending.isNotEmpty()) {
                        item {
                            Text(
                                "PENDING",
                                style    = MaterialTheme.typography.labelSmall.copy(
                                    color         = colors.textSecondary,
                                    letterSpacing = 1.5.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp)
                            )
                        }
                        items(pending, key = { it.id }) { todo ->
                            TodoItemCard(
                                todo     = todo,
                                colors   = colors,
                                onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                onDelete = { viewModel.deleteTodo(todo.id) }
                            )
                        }
                    }

                    if (completed.isNotEmpty()) {
                        item {
                            Text(
                                "COMPLETED",
                                style    = MaterialTheme.typography.labelSmall.copy(
                                    color         = colors.textSecondary,
                                    letterSpacing = 1.5.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 12.dp)
                            )
                        }
                        items(completed, key = { it.id }) { todo ->
                            TodoItemCard(
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
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = colors.primary,
            contentColor   = Color.White,
            shape          = CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun TodoItemCard(
    todo: TodoItem,
    colors: com.ragdev69.hoshimemo.ui.theme.HoshiColors,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf           = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val priorityColor = when (todo.priority) {
        2    -> Color(0xFFFF4444)
        1    -> Color(0xFFFFAA00)
        else -> colors.textSecondary
    }

    // ── Completion burst animation ────────────────────────────
    var triggerBurst by remember(todo.id) { mutableStateOf(false) }
    var wasCompleted by remember(todo.id) { mutableStateOf(todo.isCompleted) }

    LaunchedEffect(todo.isCompleted) {
        if (todo.isCompleted && !wasCompleted) {
            triggerBurst = true
            kotlinx.coroutines.delay(500)
            triggerBurst = false
        }
        wasCompleted = todo.isCompleted
    }

    val burstScale by animateFloatAsState(
        targetValue   = if (triggerBurst) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "burst_scale"
    )

    val cardBgColor by animateColorAsState(
        targetValue   = if (todo.isCompleted)
            colors.primary.copy(alpha = 0.10f)
        else
            colors.card,
        animationSpec = tween(400),
        label         = "card_bg"
    )

    val checkScale by animateFloatAsState(
        targetValue   = if (triggerBurst) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .scale(burstScale),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated checkbox
            Box(
                modifier = Modifier.scale(checkScale),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked         = todo.isCompleted,
                    onCheckedChange = {
                        onToggle()
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor   = colors.primary,
                        uncheckedColor = colors.textSecondary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                // Animated strikethrough text
                val textAlpha by animateFloatAsState(
                    targetValue   = if (todo.isCompleted) 0.55f else 1f,
                    animationSpec = tween(300),
                    label         = "text_alpha"
                )
                Text(
                    text  = todo.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color          = if (todo.isCompleted)
                            colors.textSecondary
                        else
                            colors.text,
                        textDecoration = if (todo.isCompleted)
                            TextDecoration.LineThrough
                        else
                            TextDecoration.None,
                        fontWeight     = FontWeight.Medium
                    ),
                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(200)
                    )
                )
                todo.createdAt?.let {
                    Text(
                        text  = sdf.format(it),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary
                        )
                    )
                }
            }

            // Completion badge animation
            AnimatedVisibility(
                visible = todo.isCompleted,
                enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit    = scaleOut() + fadeOut()
            ) {
                Text(
                    "✓",
                    color    = colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            if (todo.priority > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint     = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}