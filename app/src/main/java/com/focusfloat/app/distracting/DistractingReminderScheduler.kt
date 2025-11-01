package com.focusfloat.app.distracting

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class DistractingReminderScheduler(private val context: Context) {
    fun schedule(packageName: String, userSerial: Long, appLabel: String) {
        val work = OneTimeWorkRequestBuilder<DistractingReminderWorker>()
            .setInitialDelay(REMINDER_DELAY_MINUTES, TimeUnit.MINUTES)
            .setInputData(inputData(packageName, userSerial, appLabel))
            .addTag(TAG_DISTRACTING_REMINDER)
            .addTag(packageTag(packageName))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(packageName, userSerial),
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    fun cancel(packageName: String, userSerial: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(packageName, userSerial))
    }

    fun cancelPackage(packageName: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(packageTag(packageName))
    }

    private fun inputData(packageName: String, userSerial: Long, appLabel: String): Data {
        return Data.Builder()
            .putString(KEY_PACKAGE_NAME, packageName)
            .putLong(KEY_USER_SERIAL, userSerial)
            .putString(KEY_APP_LABEL, appLabel)
            .build()
    }

    companion object {
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_USER_SERIAL = "user_serial"
        const val KEY_APP_LABEL = "app_label"
        const val TAG_DISTRACTING_REMINDER = "distracting_reminder"
        private const val REMINDER_DELAY_MINUTES = 10L

        fun uniqueWorkName(packageName: String, userSerial: Long): String = "distracting_reminder_${packageName}_$userSerial"
        fun packageTag(packageName: String): String = "distracting_package_$packageName"
    }
}
