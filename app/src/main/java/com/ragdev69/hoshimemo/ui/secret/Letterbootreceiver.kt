package com.ragdev69.hoshimemo.ui.secret

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LetterBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext   = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val letters = FutureLetterManager.getLettersFlow(appContext).first()
                letters
                    .filter { !it.isOpened && it.unlockAt > System.currentTimeMillis() }
                    .forEach { LetterAlarmScheduler.schedule(appContext, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}