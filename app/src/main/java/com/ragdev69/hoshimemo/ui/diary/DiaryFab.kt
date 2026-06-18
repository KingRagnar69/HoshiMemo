package com.ragdev69.hoshimemo.ui.diary

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors

/**
 * A floating action button that sits on the right side of the screen,
 * roughly 60% of the way down (above centre, away from the nav bar).
 *
 * Tapping it fans out two labelled options:
 *   • Diary  → opens DiaryScreen
 *   • Code   → opens DiaryScreen with Code tab active (or a dedicated code screen)
 *
 * Usage — drop this inside your scaffold's content Box, aligned to CenterEnd:
 *
 *   Box(Modifier.fillMaxSize()) {
 *       // ... your screen content ...
 *       DiaryFab(
 *           modifier     = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
 *           onDiaryClick = { showDiary = true },
 *           onCodeClick  = { showCode  = true }
 *       )
 *   }
 */
@Composable
fun DiaryFab(
    modifier:     Modifier = Modifier,
    onDiaryClick: () -> Unit,
    onCodeClick:  () -> Unit
) {
    val colors  = LocalHoshiColors.current
    var expanded by remember { mutableStateOf(false) }

    // Rotation of the main FAB icon: 0° = book, 45° = X (close)
    val iconRotation by animateFloatAsState(
        targetValue    = if (expanded) 45f else 0f,
        animationSpec  = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label          = "fab_rotation"
    )

    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.End,
        verticalArrangement   = Arrangement.spacedBy(12.dp)
    ) {
        // ── Expanded mini-FABs ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn(tween(200)) + slideInVertically(
                animationSpec  = tween(250, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 2 }
            ),
            exit    = fadeOut(tween(150)) + slideOutVertically(
                animationSpec  = tween(200, easing = FastOutSlowInEasing),
                targetOffsetY  = { it / 2 }
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Diary option
                FabOption(
                    label   = "Diary",
                    icon    = Icons.Filled.AutoStories,
                    onClick = {
                        expanded = false
                        onDiaryClick()
                    }
                )
                // Code option
                FabOption(
                    label   = "Code",
                    icon    = Icons.Filled.Key,
                    onClick = {
                        expanded = false
                        onCodeClick()
                    }
                )
            }
        }

        // ── Main FAB ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (expanded) Icons.Filled.Close else Icons.Filled.AutoStories,
                contentDescription = if (expanded) "Close" else "Open Diary",
                tint               = androidx.compose.ui.graphics.Color.White,
                modifier           = Modifier
                    .size(24.dp)
                    .rotate(iconRotation)
            )
        }
    }
}

@Composable
private fun FabOption(
    label:   String,
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalHoshiColors.current

    Row(
        modifier            = Modifier.clickable(onClick = onClick),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Label pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color      = colors.text,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Mini circular icon button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = colors.primary,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}