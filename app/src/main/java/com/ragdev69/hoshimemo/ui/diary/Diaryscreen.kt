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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ragdev69.hoshimemo.ui.sound.LocalSoundConfig
import com.ragdev69.hoshimemo.ui.sound.SoundManager
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
                        readOnly  = !isEditable,
                        dateLabel = when {
                            selectedDate == today -> "Today"
                            else -> selectedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
                        }
                    )
                    DiaryTab.CODE  -> CodeUnlockPanel(
                        onCodeEntered = { code ->
                            if (code == "181412") {
                                onCodeUnlocked(code)
                            }
                        },
                        onAdUnlock = { onCodeUnlocked("AD_UNLOCK") }
                    )
                }
            }
        }
    }
}

// ── Diary page — decorated paper UI + lightweight rich-text formatting ────────
// No emojis (they didn't render reliably as a text-insertion target inside
// BasicTextField on every device/keyboard, which is what made them feel
// "broken" — tapping one should insert a character at the cursor, but emoji
// glyphs are multi-codepoint and some IME/cursor combinations swallowed the
// insertion). Replaced with real formatting: bold, underline, bullet points,
// numbered lists, text color, and highlight. Formatting is stored as simple
// inline markers directly in the plain text that's already saved to Room —
// no schema change — and rendered as the actual styled result via a shared
// parser used both for the live editor (VisualTransformation) and read-only
// rendering (AnnotatedString), so the two never drift out of sync with
// each other.
//
// Marker set:
//   **bold**
//   __underline__
//   [c:#RRGGBB]colored text[/c]
//   [h:#RRGGBB]highlighted text[/h]
//   "• "  and "1. " line prefixes for bullet / numbered lists

private data class MarkerSpan(
    val openLen: Int,
    val closeMarker: String,
    val style: SpanStyle
)

// Tries to match a marker opening at position `i`. Returns the marker info
// and the position right after the opening tag, or null if none matches.
private fun matchMarkerOpen(text: String, i: Int): Pair<MarkerSpan, Int>? {
    return when {
        text.startsWith("**", i) -> MarkerSpan(2, "**", SpanStyle(fontWeight = FontWeight.Bold)) to i + 2
        text.startsWith("__", i) -> MarkerSpan(2, "__", SpanStyle(textDecoration = TextDecoration.Underline)) to i + 2
        text.startsWith("[c:", i) -> {
            val close = text.indexOf("]", i + 3)
            if (close == -1) return null
            val hex = text.substring(i + 3, close)
            val color = parseHexColorOrNull(hex) ?: return null
            MarkerSpan(close + 1 - i, "[/c]", SpanStyle(color = color)) to close + 1
        }
        text.startsWith("[h:", i) -> {
            val close = text.indexOf("]", i + 3)
            if (close == -1) return null
            val hex = text.substring(i + 3, close)
            val color = parseHexColorOrNull(hex) ?: return null
            MarkerSpan(close + 1 - i, "[/h]", SpanStyle(background = color)) to close + 1
        }
        else -> null
    }
}

private fun parseHexColorOrNull(hex: String): Color? {
    return try {
        val clean = hex.removePrefix("#")
        if (clean.length != 6) return null
        Color(("FF$clean").toLong(16))
    } catch (e: Exception) { null }
}

// Shared parser. `dimMarkers` controls whether opening/closing tags are kept
// in the output (small + faint, for the LIVE editor — so cursor offsets stay
// 1:1 with the raw text) or stripped entirely (for read-only rendering,
// where there's no cursor to keep in sync, so the clean styled result reads
// better without literal "**" characters cluttering it).
private fun parseDiaryMarkup(text: String, dimMarkers: Boolean, mutedColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val match = matchMarkerOpen(text, i)
        if (match == null) {
            append(text[i]); i++
            continue
        }
        val (marker, contentStart) = match
        val end = text.indexOf(marker.closeMarker, contentStart)
        if (end == -1) {
            // No matching close tag found — treat the opener as literal text
            // rather than silently eating it, so a stray "**" never just vanishes.
            append(text[i]); i++
            continue
        }
        if (dimMarkers) {
            withStyle(SpanStyle(color = mutedColor, fontSize = 10.sp)) { append(text.substring(i, contentStart)) }
            withStyle(marker.style) { append(text.substring(contentStart, end)) }
            withStyle(SpanStyle(color = mutedColor, fontSize = 10.sp)) { append(marker.closeMarker) }
        } else {
            withStyle(marker.style) { append(text.substring(contentStart, end)) }
        }
        i = end + marker.closeMarker.length
    }
}

private fun markdownToAnnotated(text: String): AnnotatedString =
    parseDiaryMarkup(text, dimMarkers = false, mutedColor = Color.Transparent)

// Renders markers as actual bold/underline/color/highlight while keeping the
// cursor mapped to the RAW text underneath (so editing still works
// character-for-character). Markers stay visible-but-dim rather than
// vanishing completely, since fully hiding them would desync the visible
// cursor position from the real text offset and break typing/deletion near
// a marker — this only works because dimming never changes the character
// count, so OffsetMapping.Identity stays valid.
private class DiaryFormattingTransform(private val mutedColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val out = parseDiaryMarkup(text.text, dimMarkers = true, mutedColor = mutedColor)
        return TransformedText(out, OffsetMapping.Identity)
    }
}

@Composable
private fun DiaryEditor(
    value:    String,
    onChange: (String) -> Unit,
    readOnly: Boolean = false,
    dateLabel: String = ""
) {
    val colors      = LocalHoshiColors.current
    val soundConfig = LocalSoundConfig.current

    // Debounce timestamps for write and delete sounds
    var lastWriteSoundMs by remember { mutableLongStateOf(0L) }
    var lastDelSoundMs   by remember { mutableLongStateOf(0L) }

    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    fun applyChange(newValue: TextFieldValue) {
        val now = System.currentTimeMillis()
        when {
            newValue.text.length > fieldValue.text.length -> {
                if (!readOnly && now - lastWriteSoundMs > 400L) {
                    lastWriteSoundMs = now
                    SoundManager.playWrite(soundConfig.secretTheme, soundConfig.muted)
                }
            }
            newValue.text.length < fieldValue.text.length -> {
                if (!readOnly && now - lastDelSoundMs > 400L) {
                    lastDelSoundMs = now
                    SoundManager.playDel(soundConfig.secretTheme, soundConfig.muted)
                }
            }
        }
        fieldValue = newValue
        onChange(newValue.text)
    }

    // Wrap the current selection in `marker` (or insert an empty pair with
    // the cursor placed in between, if nothing is selected).
    fun wrapSelection(marker: String) {
        if (readOnly) return
        val sel = fieldValue.selection
        val text = fieldValue.text
        if (sel.collapsed) {
            val insertion = "$marker$marker"
            val newText = text.substring(0, sel.start) + insertion + text.substring(sel.start)
            applyChange(TextFieldValue(newText, TextRange(sel.start + marker.length)))
        } else {
            val start = minOf(sel.start, sel.end)
            val end   = maxOf(sel.start, sel.end)
            val selected = text.substring(start, end)
            val newText = text.substring(0, start) + marker + selected + marker + text.substring(end)
            applyChange(TextFieldValue(newText, TextRange(end + marker.length * 2)))
        }
    }

    fun insertLinePrefix(prefix: String) {
        if (readOnly) return
        val text = fieldValue.text
        val cursor = fieldValue.selection.start
        val needsNewline = text.isNotEmpty() && cursor > 0 && text[cursor - 1] != '\n'
        val insertion = if (needsNewline) "\n$prefix" else prefix
        val newText = text.substring(0, cursor) + insertion + text.substring(cursor)
        applyChange(TextFieldValue(newText, TextRange(cursor + insertion.length)))
    }

    fun insertNumberedLine() {
        if (readOnly) return
        // Look back through prior lines to find the next number in sequence.
        val linesBefore = fieldValue.text.substring(0, fieldValue.selection.start).lines()
        val lastNumbered = linesBefore.lastOrNull { it.trimStart().firstOrNull()?.isDigit() == true && it.contains(". ") }
        val nextNum = lastNumbered
            ?.trimStart()
            ?.substringBefore(". ")
            ?.toIntOrNull()
            ?.plus(1) ?: 1
        insertLinePrefix("$nextNum. ")
    }

    // Wraps the current selection (or inserts an empty pair at the cursor)
    // in a [c:#HEX]...[/c] or [h:#HEX]...[/h] marker.
    fun wrapWithTag(openTag: String, closeTag: String) {
        if (readOnly) return
        val sel = fieldValue.selection
        val text = fieldValue.text
        if (sel.collapsed) {
            val insertion = "$openTag$closeTag"
            val newText = text.substring(0, sel.start) + insertion + text.substring(sel.start)
            applyChange(TextFieldValue(newText, TextRange(sel.start + openTag.length)))
        } else {
            val start = minOf(sel.start, sel.end)
            val end   = maxOf(sel.start, sel.end)
            val selected = text.substring(start, end)
            val newText = text.substring(0, start) + openTag + selected + closeTag + text.substring(end)
            applyChange(TextFieldValue(newText, TextRange(end + openTag.length + closeTag.length)))
        }
    }

    var showColorPicker     by remember { mutableStateOf(false) }
    var showHighlightPicker by remember { mutableStateOf(false) }

    // A small, theme-friendly palette — works on both light and dark pages.
    val swatches = listOf(
        "#E84855", "#FF9F1C", "#F5C518", "#2EC4B6", "#3A86FF", "#8338EC", "#FF6FB5", "#5C5C66"
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Formatting toolbar ──────────────────────────────────────────
        if (!readOnly) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.navBar)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FormatToolButton(label = "B", description = "Bold", bold = true, colors = colors) { wrapSelection("**") }
                FormatToolButton(label = "U", description = "Underline", underline = true, colors = colors) { wrapSelection("__") }
                FormatToolButton(label = "•", description = "Bullet point", colors = colors) { insertLinePrefix("• ") }
                FormatToolButton(label = "1.", description = "Numbered list", colors = colors) { insertNumberedLine() }
                FormatToolButton(
                    label = "A", description = "Text color", colors = colors,
                    tint = colors.accent
                ) {
                    showHighlightPicker = false
                    showColorPicker = !showColorPicker
                }
                FormatToolButton(
                    label = "A", description = "Highlight", colors = colors,
                    highlightSwatch = colors.accent.copy(alpha = 0.35f)
                ) {
                    showColorPicker = false
                    showHighlightPicker = !showHighlightPicker
                }
            }

            AnimatedVisibility(visible = showColorPicker || showHighlightPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.navBar)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    swatches.forEach { hex ->
                        val swatchColor = parseHexColorOrNull(hex) ?: Color.Gray
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .clickable {
                                    if (showColorPicker) wrapWithTag("[c:$hex]", "[/c]")
                                    else wrapWithTag("[h:$hex]", "[/h]")
                                    showColorPicker = false
                                    showHighlightPicker = false
                                }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ── Paper card ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Decorative stars scattered around the page corners — purely
            // visual, themed off the active palette so it fits Dark,
            // Lightning, Sakura, etc. without looking out of place.
            DiaryStarDecorations(colors = colors)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.card.copy(alpha = if (colors.isDark) 1f else 0.6f),
                                colors.surface
                            )
                        )
                    )
                    .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Washi-tape style date tag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .rotate(-2f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.accent.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = dateLabel.ifEmpty { "Today" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Notebook-rule writing area
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        NotebookRules(colors = colors)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                                // Intercept all taps when read-only so they don't
                                // fall through to notes/cards behind this screen.
                                .then(
                                    if (readOnly) Modifier.clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                        onClick = {}
                                    ) else Modifier
                                )
                        ) {
                            if (fieldValue.text.isEmpty() && !readOnly) {
                                Text(
                                    text  = "Write your thoughts for today…",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color      = colors.textSecondary.copy(alpha = 0.5f),
                                        lineHeight = 28.sp
                                    )
                                )
                            }
                            if (fieldValue.text.isEmpty() && readOnly) {
                                Text(
                                    text  = "No entry for this day.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color      = colors.textSecondary.copy(alpha = 0.4f),
                                        lineHeight = 28.sp
                                    )
                                )
                            }
                            if (readOnly) {
                                // Read-only days never need an editable cursor — just
                                // render the formatted result as plain styled text.
                                Text(
                                    text  = markdownToAnnotated(fieldValue.text),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color      = colors.text,
                                        lineHeight = 28.sp,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                            } else {
                                BasicTextField(
                                    value         = fieldValue,
                                    onValueChange = { applyChange(it) },
                                    readOnly      = false,
                                    modifier      = Modifier.fillMaxSize(),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color      = colors.text,
                                        lineHeight = 28.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    visualTransformation = DiaryFormattingTransform(colors.textSecondary.copy(alpha = 0.35f)),
                                    cursorBrush = SolidColor(colors.primary),
                                    decorationBox = { innerTextField -> innerTextField() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatToolButton(
    label: String,
    description: String,
    bold: Boolean = false,
    underline: Boolean = false,
    tint: Color? = null,
    highlightSwatch: Color? = null,
    colors: com.ragdev69.hoshimemo.ui.theme.HoshiColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (highlightSwatch != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(highlightSwatch)
                    .padding(horizontal = 3.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = colors.text,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = tint ?: colors.textSecondary,
                    fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
                    textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None
                )
            )
        }
    }
}

// Decorative ✦ / ✧ stars scattered near the corners of the diary page —
// purely visual flourish, no interaction. Sizes/positions/opacities are
// fixed (not random) so the layout doesn't jump between recompositions.
@Composable
private fun DiaryStarDecorations(colors: com.ragdev69.hoshimemo.ui.theme.HoshiColors) {
    Box(modifier = Modifier.fillMaxSize()) {
        StarGlyph("✦", colors.accent, 16.sp, Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-10).dp))
        StarGlyph("✧", colors.primary, 11.sp, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-2).dp))
        StarGlyph("✦", colors.primary, 10.sp, Modifier.align(Alignment.BottomStart).offset(x = 2.dp, y = 8.dp))
        StarGlyph("✧", colors.accent, 14.sp, Modifier.align(Alignment.BottomEnd).offset(x = (-2).dp, y = 10.dp))
    }
}

@Composable
private fun StarGlyph(glyph: String, color: Color, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier) {
    Text(
        text = glyph,
        color = color.copy(alpha = 0.55f),
        fontSize = size,
        modifier = modifier
    )
}

// Faint horizontal rule lines behind the text, like notebook paper.
@Composable
private fun NotebookRules(colors: com.ragdev69.hoshimemo.ui.theme.HoshiColors) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineSpacing = 28.dp.toPx()
        val lineColor = colors.border.copy(alpha = if (colors.isDark) 0.25f else 0.4f)
        var y = 50f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end   = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
            y += lineSpacing
        }
    }
}

// ── Code unlock panel ─────────────────────────────────────────────────────────
// This is NOT a source-code editor. It's a passcode entry that, when a
// recognised code is entered, calls onCodeEntered so the caller can navigate.
// Also offers a "Watch Ad to Unlock" path: a rewarded ad that, once watched
// to completion, calls onAdUnlock — this unlock is SESSION-ONLY (not
// persisted), so it resets the next time the app is closed/restarted.
@Composable
private fun CodeUnlockPanel(
    onCodeEntered: (String) -> Unit,
    onAdUnlock: () -> Unit
) {
    val colors  = LocalHoshiColors.current
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    var code   by remember { mutableStateOf("") }
    var shake  by remember { mutableStateOf(false) }

    // ── Rewarded ad state ───────────────────────────────────────────────────
    var rewardedAd   by remember { mutableStateOf<RewardedAd?>(null) }
    var isAdLoading  by remember { mutableStateOf(false) }
    var adLoadFailed by remember { mutableStateOf(false) }

    val rewardedAdUnitId = if (com.ragdev69.hoshimemo.BuildConfig.DEBUG)
        "ca-app-pub-3940256099942544/5224354917"   // Google's official rewarded test unit ID
    else
        "ca-app-pub-7539583464292210/8595709262"   // Diary Secret Vault Unlock — live rewarded ad unit

    fun loadRewardedAd() {
        if (isAdLoading || rewardedAd != null) return
        isAdLoading = true
        adLoadFailed = false
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd  = ad
                    isAdLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd   = null
                    isAdLoading  = false
                    adLoadFailed = true
                }
            }
        )
    }

    // Preload as soon as this panel appears so the ad is ready to show
    // the instant the user taps the button.
    LaunchedEffect(Unit) { loadRewardedAd() }

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
            .verticalScroll(rememberScrollState())
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
                                                        // auto-submit once all 6 digits are entered
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

        // ── Divider + "Watch Ad to Unlock" alternative ─────────────────────
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
            Text(
                "OR",
                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
        }
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(colors.primary.copy(alpha = 0.12f))
                .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                .clickable(enabled = !isAdLoading) {
                    val ad = rewardedAd
                    if (ad != null && activity != null) {
                        ad.show(activity) { _ ->
                            // Reward earned — unlock for this session only.
                            // Consume the ad instance and preload the next one
                            // so it's ready if the user backs out and re-enters.
                            rewardedAd = null
                            onAdUnlock()
                            loadRewardedAd()
                        }
                    } else {
                        // Not ready yet — try loading again
                        loadRewardedAd()
                    }
                }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary
                    )
                } else {
                    Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                Text(
                    if (isAdLoading) "Loading ad…" else "Watch Ad to Unlock",
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        if (adLoadFailed) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Couldn't load ad — check connection and try again",
                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Unlocks until you close the app",
            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary.copy(alpha = 0.6f))
        )
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