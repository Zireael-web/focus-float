package com.focusfloat.app.pause.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PauseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PauseExpiryScheduler.ACTION_PAUSE_EXPIRED ||
            intent.action == PauseExpiryScheduler.ACTION_RETRY_UNPAUSE
        ) {
            PauseExpiryScheduler(context).enqueueImmediate(intent.action ?: "alarm")
        }
    }
}
