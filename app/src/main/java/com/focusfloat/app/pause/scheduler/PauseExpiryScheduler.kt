package com.focusfloat.app.pause.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.max

class PauseExpiryScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(pausedUntil: Instant): PauseScheduleResult {
        val exactAlarmScheduled = runCatching { scheduleAlarm(pausedUntil) }.getOrDefault(false)
        scheduleWork(pausedUntil)
        return PauseScheduleResult(exactAlarmScheduled)
    }

    fun enqueueImmediate(reason: String) {
        val work = OneTimeWorkRequestBuilder<PauseReconcileWorker>()
            .addTag("pause_reconcile")
            .setInputData(PauseReconcileWorker.inputData(reason))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_RECONCILE_WORK,
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    private fun scheduleAlarm(pausedUntil: Instant): Boolean {
        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) return false
        val intent = Intent(context, PauseAlarmReceiver::class.java).setAction(ACTION_PAUSE_EXPIRED)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            10_001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            pausedUntil.toEpochMilli(),
            pendingIntent,
        )
        return true
    }

    private fun scheduleWork(pausedUntil: Instant) {
        val delayMs = max(0, pausedUntil.toEpochMilli() - System.currentTimeMillis())
        val work = OneTimeWorkRequestBuilder<PauseReconcileWorker>()
            .addTag("pause_reconcile")
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(PauseReconcileWorker.inputData("work"))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_EXPIRY_RECONCILE_WORK,
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    companion object {
        const val ACTION_PAUSE_EXPIRED = "com.focusfloat.app.action.PAUSE_EXPIRED"
        const val ACTION_RETRY_UNPAUSE = "com.focusfloat.app.action.RETRY_UNPAUSE"
        const val UNIQUE_IMMEDIATE_RECONCILE_WORK = "pause_reconcile_now"
        const val UNIQUE_EXPIRY_RECONCILE_WORK = "pause_reconcile_expiry"
    }
}

data class PauseScheduleResult(
    val exactAlarmScheduled: Boolean,
)
