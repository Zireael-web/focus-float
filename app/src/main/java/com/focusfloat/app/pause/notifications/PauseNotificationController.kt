package com.focusfloat.app.pause.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focusfloat.app.MainActivity
import com.focusfloat.app.R
import com.focusfloat.app.pause.model.PausePackageFailure
import com.focusfloat.app.pause.model.PauseSession
import com.focusfloat.app.pause.scheduler.PauseAlarmReceiver
import com.focusfloat.app.pause.scheduler.PauseExpiryScheduler

class PauseNotificationController(private val context: Context) {
    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PAUSE_STATUS,
                "Pause status",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun showShizukuNeededForUnpause(session: PauseSession): Boolean {
        return notify(
            id = session.id.toInt(),
            title = "Some apps are still paused",
            body = "Open FocusFloat, check pause setup, and retry unpause.",
        )
    }

    fun showUnpauseFailed(session: PauseSession, failures: List<PausePackageFailure>): Boolean {
        return notify(
            id = session.id.toInt(),
            title = "Could not unpause ${failures.size} app${if (failures.size == 1) "" else "s"}",
            body = "Open FocusFloat, check pause setup, and retry unpause.",
        )
    }

    fun showExactAlarmPermissionNeeded(): Boolean {
        return notify(
            id = EXACT_ALARM_NOTIFICATION_ID,
            title = "Midnight unpause may be delayed",
            body = "Allow exact alarms so FocusFloat can unpause apps at midnight.",
            actions = listOf(
                NotificationAction(
                    title = "Open settings",
                    intent = exactAlarmSettingsIntent(),
                    requestCode = 20_004,
                ),
            ),
        )
    }

    private fun notify(
        id: Int,
        title: String,
        body: String,
        actions: List<NotificationAction> = defaultActions(),
    ): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        ensureChannels()
        val builder = NotificationCompat.Builder(context, CHANNEL_PAUSE_STATUS)
            .setSmallIcon(R.drawable.ic_stat_focusfloat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openLauncherIntent())
            .setAutoCancel(true)

        actions.forEach { action ->
            val pendingIntent = if (action.broadcast) {
                PendingIntent.getBroadcast(
                    context,
                    action.requestCode,
                    action.intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    action.requestCode,
                    action.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
            builder.addAction(
                R.drawable.ic_stat_focusfloat,
                action.title,
                pendingIntent,
            )
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())
        return true
    }

    private fun openLauncherIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context,
            20_001,
            focusFloatIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun retryIntent(): PendingIntent {
        val intent = Intent(context, PauseAlarmReceiver::class.java)
            .setAction(PauseExpiryScheduler.ACTION_RETRY_UNPAUSE)
        return PendingIntent.getBroadcast(
            context,
            20_003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun exactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= 31) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    private fun defaultActions(): List<NotificationAction> = listOf(
        NotificationAction(
            title = "Open FocusFloat",
            intent = focusFloatIntent(),
            requestCode = 20_002,
        ),
        NotificationAction(
            title = "Retry",
            intent = Intent(context, PauseAlarmReceiver::class.java)
                .setAction(PauseExpiryScheduler.ACTION_RETRY_UNPAUSE),
            requestCode = 20_003,
            broadcast = true,
        ),
    )

    private fun focusFloatIntent(): Intent {
        return Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    companion object {
        const val CHANNEL_PAUSE_STATUS = "pause_status"
        private const val EXACT_ALARM_NOTIFICATION_ID = 30_001
    }
}

private data class NotificationAction(
    val title: String,
    val intent: Intent,
    val requestCode: Int,
    val broadcast: Boolean = false,
)
