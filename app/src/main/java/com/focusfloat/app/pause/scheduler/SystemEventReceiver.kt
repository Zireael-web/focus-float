package com.focusfloat.app.pause.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PauseExpiryScheduler(context).enqueueImmediate(intent.action ?: "system")
    }
}
