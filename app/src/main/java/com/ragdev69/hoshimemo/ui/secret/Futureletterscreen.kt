package com.ragdev69.hoshimemo.ui.secret

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureLetterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors  = LocalHoshiColors.current
    val scope   = rememberCoroutineScope()

    val letters by FutureLetterManager.getLettersFlow(context)
        .collectAsState(initial = emptyList())

    var showCompose by remember { mutableStateOf(false) }
    var viewingLetter by remember { mutableStateOf<FutureLetter?>(null) }

    val nowMs = System.currentTimeMillis()

    // Ask for notification permission so the "your letter unlocked" alert
    // can actually be shown. Only needed on Android 13+.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op either way — alarms still schedule, notification just won't show if denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Make sure letters written before this feature existed (or whose alarm
    // never got scheduled, e.g. app was force-stopped) still get one.
    // schedule() is a no-op for letters that are already open-able or opened.
    LaunchedEffect(letters) {
        letters.forEach { LetterAlarmScheduler.schedule(context.applicationContext, it) }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Letters to Future Me", color = colors.text, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                actions = {
                    IconButton(onClick = { showCompose = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Write letter", tint = colors.primary)
                    }
                }
            )
        }
    ) { padding ->

        if (letters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✉️", fontSize = 52.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No letters yet.\nWrite one for your future self.",
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { showCompose = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Write a letter", color = Color.White) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(letters.sortedBy { it.unlockAt }) { letter ->
                    val canOpen = nowMs >= letter.unlockAt
                    LetterCard(
                        letter    = letter,
                        canOpen   = canOpen,
                        accent    = colors.primary,
                        textColor = colors.text,
                        subColor  = colors.textSecondary,
                        cardColor = colors.card,
                        onOpen    = {
                            if (canOpen) {
                                scope.launch { FutureLetterManager.markOpened(context, letter.id) }
                                viewingLetter = letter
                            }
                        },
                        onDelete  = {
                            scope.launch { FutureLetterManager.deleteLetter(context, letter.id) }
                        }
                    )
                }
            }
        }
    }

    // Compose new letter dialog
    if (showCompose) {
        ComposeLetterDialog(
            accentColor = colors.primary,
            onDismiss   = { showCompose = false },
            onSave      = { letter ->
                scope.launch { FutureLetterManager.saveLetter(context, letter) }
                showCompose = false
            }
        )
    }

    // View opened letter
    viewingLetter?.let { letter ->
        ViewLetterDialog(
            letter     = letter,
            accentColor = colors.primary,
            onDismiss  = { viewingLetter = null }
        )
    }
}

@Composable
private fun LetterCard(
    letter: FutureLetter,
    canOpen: Boolean,
    accent: Color,
    textColor: Color,
    subColor: Color,
    cardColor: Color,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val daysUntil = ((letter.unlockAt - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)

    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lock icon
            Icon(
                imageVector = if (canOpen) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint   = if (canOpen) accent else subColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(letter.title, color = textColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                if (canOpen) {
                    Text("Opens: ${fmt.format(Date(letter.unlockAt))}", color = accent, fontSize = 12.sp)
                } else {
                    Text("Opens in $daysUntil day${if (daysUntil == 1L) "" else "s"}", color = subColor, fontSize = 12.sp)
                }
                Text(
                    "Written: ${fmt.format(Date(letter.writtenAt))}",
                    color = subColor.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            // Open / Delete buttons
            if (canOpen) {
                IconButton(onClick = onOpen) {
                    Text("📖", fontSize = 20.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = subColor.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeLetterDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (FutureLetter) -> Unit
) {
    var title   by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var unlockDateMs by remember { mutableLongStateOf(
        System.currentTimeMillis() + 365L * 86_400_000L  // default: 1 year
    ) }
    var showDatePicker by remember { mutableStateOf(false) }

    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1A1320),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Write to Future You", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor          = accentColor,
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Your letter...", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor          = accentColor,
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White
                    ),
                    maxLines = 8
                )

                Spacer(Modifier.height(12.dp))

                // Date selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Unlock on", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(fmt.format(Date(unlockDateMs)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onSave(
                                    FutureLetter(
                                        id        = UUID.randomUUID().toString(),
                                        title     = title.trim(),
                                        content   = content.trim(),
                                        writtenAt = System.currentTimeMillis(),
                                        unlockAt  = unlockDateMs
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Seal & Lock 🔒", color = Color.White) }
                }
            }
        }
    }

    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = unlockDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { unlockDateMs = it }
                    showDatePicker = false
                }) { Text("OK", color = accentColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1320))
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor    = accentColor,
                    todayDateBorderColor         = accentColor,
                    containerColor               = Color(0xFF1A1320),
                    titleContentColor            = Color.White,
                    headlineContentColor         = Color.White,
                    weekdayContentColor          = Color.White.copy(alpha = 0.6f),
                    dayContentColor              = Color.White,
                    currentYearContentColor      = accentColor
                )
            )
        }
    }
}

@Composable
private fun ViewLetterDialog(
    letter: FutureLetter,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val fmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1A1320),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("✉️ ${letter.title}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Written on ${fmt.format(Date(letter.writtenAt))}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(16.dp))
                Text(
                    text  = letter.content,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors  = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Close", color = Color.White) }
            }
        }
    }
}