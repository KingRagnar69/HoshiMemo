package com.ragdev69.hoshimemo.ui.diary

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Tab ───────────────────────────────────────────────────────────────────────
private enum class DiaryTab { DIARY, CODE }

// ── Entry point ───────────────────────────────────────────────────────────────
@Composable
fun DiaryScreen(
    onDismiss: () -> Unit,
    onCodeUnlocked: (String) -> Unit = {},
    vm: DiaryViewModel = viewModel()
) {
    val colors       = LocalHoshiColors.current
    val selectedDate by vm.selectedDate.collectAsState()
    val currentEntry by vm.currentEntry.collectAsState()
    val entryDates   by vm.entryDates.collectAsState()
    val earliestDate by vm.earliestDate.collectAsState()

    val today = LocalDate.now()

    // A date is editable only if it's today
    val isEditable = selectedDate == today

    var showCalendar by remember { mutableStateOf(false) }
    var activeTab    by remember { mutableStateOf(DiaryTab.DIARY) }

    var diaryDraft by remember(currentEntry, selectedDate) {
        mutableStateOf(currentEntry?.diaryText ?: "")
    }
    var codeDraft by remember(currentEntry, selectedDate) {
        mutableStateOf(currentEntry?.codeText ?: "")
    }

    val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()   // ← pushes content below system status bar
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))   // breathing room below status bar

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = colors.text
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            selectedDate == today -> "Today"
                            selectedDate > today  -> selectedDate.format(
                                DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
                            ) + "  ·  Future"
                            else -> selectedDate.format(
                                DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
                            )
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = colors.text
                        )
                    )
                    Text(
                        text  = selectedDate.format(monthFmt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary
                        )
                    )
                }

                // Calendar toggle
                IconButton(
                    onClick  = { showCalendar = !showCalendar },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (showCalendar) colors.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = "Calendar",
                        tint = if (showCalendar) colors.primary else colors.textSecondary
                    )
                }

                // Save button — only shown when today (editable)
                AnimatedVisibility(visible = isEditable) {
                    IconButton(
                        onClick  = { vm.saveEntry(diaryDraft, codeDraft) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save",
                            tint = colors.primary
                        )
                    }
                }
            }

            // ── Calendar ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showCalendar,
                enter   = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit    = shrinkVertically(tween(300)) + fadeOut(tween(300))
            ) {
                DiaryCalendar(
                    selectedDate   = selectedDate,
                    entryDates     = entryDates,
                    earliestDate   = earliestDate ?: selectedDate,
                    onDateSelected = { date ->
                        vm.selectDate(date)
                        showCalendar = false
                    }
                )
            }

            // ── Tabs ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.navBar)
                    .padding(4.dp)
            ) {
                DiaryTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) colors.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { activeTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (tab) {
                                    DiaryTab.DIARY -> Icons.Filled.AutoStories
                                    DiaryTab.CODE  -> Icons.Filled.Key
                                },
                                contentDescription = null,
                                tint     = if (selected) colors.primary else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (tab) {
                                    DiaryTab.DIARY -> "Diary"
                                    DiaryTab.CODE  -> "Code"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color      = if (selected) colors.primary else colors.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = colors.textSecondary.copy(alpha = 0.1f)
            )

            // Read-only banner for past / future dates
            if (!isEditable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (selectedDate > today) Icons.Filled.Visibility
                        else Icons.Filled.Lock,
                        contentDescription = null,
                        tint     = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (selectedDate > today)
                            "Future date — view only"
                        else
                            "Past entries cannot be edited",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color      = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // ── Editor ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (activeTab) {
                    DiaryTab.DIARY -> DiaryEditor(
                        value     = diaryDraft,
                        onChange  = { if (isEditable) diaryDraft = it },
                        readOnly  = !isEditable
                    )
                    DiaryTab.CODE  -> CodeUnlockPanel(
                        onCodeEntered = { code ->
                            if (code == "010101") {
                                onCodeUnlocked(code)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── Diary text editor ─────────────────────────────────────────────────────────
@Composable
private fun DiaryEditor(
    value:    String,
    onChange: (String) -> Unit,
    readOnly: Boolean = false
) {
    val colors = LocalHoshiColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (value.isEmpty() && !readOnly) {
            Text(
                text  = "Write your thoughts for today...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textSecondary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(4.dp)
            )
        }
        if (value.isEmpty() && readOnly) {
            Text(
                text  = "No entry for this day.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textSecondary.copy(alpha = 0.4f)
                ),
                modifier = Modifier.padding(4.dp)
            )
        }
        BasicTextField(
            value         = value,
            onValueChange = onChange,
            readOnly      = readOnly,
            modifier      = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color      = colors.text,
                lineHeight = 28.sp
            ),
            decorationBox = { innerTextField -> innerTextField() }
        )
    }
}

// ── Code unlock panel ─────────────────────────────────────────────────────────
// This is NOT a source-code editor. It's a passcode entry that, when a
// recognised code is entered, calls onCodeEntered so the caller can navigate.
@Composable
private fun CodeUnlockPanel(
    onCodeEntered: (String) -> Unit
) {
    val colors = LocalHoshiColors.current
    var code   by remember { mutableStateOf("") }
    var shake  by remember { mutableStateOf(false) }

    val shakeOffset by animateFloatAsState(
        targetValue = if (shake) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 400
            0f  at 0
            -8f at 50
            8f  at 100
            -6f at 150
            6f  at 200
            -4f at 250
            4f  at 300
            0f  at 400
        },
        finishedListener = { shake = false },
        label = "shake"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Key,
                contentDescription = null,
                tint     = colors.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text  = "Enter Code",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color      = colors.text
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text  = "Enter a secret code to continue",
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.textSecondary
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Code dots display
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.offset(x = shakeOffset.dp)
        ) {
            repeat(6) { index ->
                val filled = index < code.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) colors.primary
                            else colors.textSecondary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Numpad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(colors.navBar)
                                    .clickable {
                                        when (key) {
                                            "⌫"  -> { if (code.isNotEmpty()) code = code.dropLast(1) }
                                            else -> {
                                                if (code.length < 6) {
                                                    code += key
                                                    if (code.length == 6) {
                                                        // auto-submit at 3 digits per original spec ("010")
                                                        onCodeEntered(code)
                                                        code = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = key,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color      = colors.text,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Calendar ──────────────────────────────────────────────────────────────────
@Composable
private fun DiaryCalendar(
    selectedDate:   LocalDate,
    entryDates:     Set<String>,
    earliestDate:   LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val colors  = LocalHoshiColors.current
    val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today         = LocalDate.now()
    val earliestMonth = YearMonth.from(earliestDate)
    // Allow viewing up to year 2126
    val maxMonth      = YearMonth.of(2126, 12)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.navBar)
            .padding(12.dp)
    ) {
        // Month nav row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = { if (currentMonth > earliestMonth) currentMonth = currentMonth.minusMonths(1) },
                enabled  = currentMonth > earliestMonth
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = if (currentMonth > earliestMonth) colors.text else colors.textSecondary.copy(alpha = 0.3f)
                )
            }

            Text(
                text  = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = colors.text
                )
            )

            IconButton(
                onClick  = { if (currentMonth < maxMonth) currentMonth = currentMonth.plusMonths(1) },
                enabled  = currentMonth < maxMonth
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (currentMonth < maxMonth) colors.text else colors.textSecondary.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day-of-week headers
        val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text      = day,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style     = MaterialTheme.typography.labelSmall.copy(
                        color      = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Day grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val startOffset     = firstDayOfMonth.dayOfWeek.value % 7   // Sun=0
        val daysInMonth     = currentMonth.lengthOfMonth()
        val totalCells      = startOffset + daysInMonth
        val rows            = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNum    = cellIndex - startOffset + 1

                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        val date       = currentMonth.atDay(dayNum)
                        val isSelected = date == selectedDate
                        val isToday    = date == today
                        val isFuture   = date > today
                        val hasEntry   = entryDates.contains(date.format(dateFmt))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> colors.primary
                                        isToday    -> colors.primary.copy(alpha = 0.2f)
                                        else       -> Color.Transparent
                                    }
                                )
                                // All dates clickable — future is view-only (handled in DiaryScreen)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text  = dayNum.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = when {
                                            isSelected -> Color.White
                                            isFuture   -> colors.textSecondary.copy(alpha = 0.4f)
                                            isToday    -> colors.primary
                                            else       -> colors.text
                                        },
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                if (hasEntry && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}