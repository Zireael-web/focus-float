package com.focusfloat.app.distracting

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DistractingReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val label = inputData.getString(DistractingReminderScheduler.KEY_APP_LABEL)
            ?.takeIf { it.isNotBlank() }
            ?: "this app"
        val packageName = inputData.getString(DistractingReminderScheduler.KEY_PACKAGE_NAME)
        if (packageName != null && !DistractingForegroundState.shouldShowReminder(packageName)) {
            return Result.success()
        }
        DistractingNotificationController(applicationContext).showReminder(label)
        return Result.success()
    }
}
