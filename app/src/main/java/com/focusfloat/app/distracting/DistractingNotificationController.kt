package com.focusfloat.app.distracting

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focusfloat.app.MainActivity
import com.focusfloat.app.R
import kotlin.math.abs

class DistractingNotificationController(private val context: Context) {
    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DISTRACTING,
                "Distracting app reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun showReminder(appLabel: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannels()
        val body = "You opened $appLabel 10 minutes ago. ${quoteFor(appLabel)}"
        val notification = NotificationCompat.Builder(context, CHANNEL_DISTRACTING)
            .setSmallIcon(R.drawable.ic_stat_focusfloat)
            .setContentTitle("Close $appLabel?")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openFocusFloatIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicReminderNotification())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(appLabel), notification)
    }

    private fun publicReminderNotification() = NotificationCompat.Builder(context, CHANNEL_DISTRACTING)
        .setSmallIcon(R.drawable.ic_stat_focusfloat)
        .setContentTitle("Focus reminder")
        .setContentText("A distracting app is still open.")
        .setContentIntent(openFocusFloatIntent())
        .setAutoCancel(true)
        .build()

    private fun openFocusFloatIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            31_001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(appLabel: String): Int = 31_000 + abs(appLabel.hashCode() % 900)

    private fun quoteFor(appLabel: String): String {
        val quotes = listOf(
            "You could leave life right now. Let that determine what you do and say and think.",
            "If it is not right, do not do it; if it is not true, do not say it.",
            "The impediment to action advances action.",
        )
        return quotes[abs(appLabel.hashCode()) % quotes.size]
    }

    companion object {
        const val CHANNEL_DISTRACTING = "distracting_reminders"
    }
}
