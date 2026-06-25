package com.ragdev69.hoshimemo.ui.sound

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.ragdev69.hoshimemo.ui.theme.SecretTheme

// ── Drop-in replacements for .clickable{} that also fire the nav sound ────────

fun Modifier.clickWithSound(
    secretTheme: SecretTheme,
    muted: Boolean,
    onClick: () -> Unit
): Modifier = this.clickable {
    SoundManager.playNav(secretTheme, muted)
    onClick()
}

fun Modifier.clickWithSoundNoRipple(
    secretTheme: SecretTheme,
    muted: Boolean,
    onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication        = null,
        onClick           = {
            SoundManager.playNav(secretTheme, muted)
            onClick()
        }
    )
}