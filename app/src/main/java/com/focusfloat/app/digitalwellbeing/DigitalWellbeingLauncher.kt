package com.focusfloat.app.digitalwellbeing

import android.content.Context
import android.content.Intent

class DigitalWellbeingLauncher(private val context: Context) {
    fun open(): Boolean {
        val intents = listOfNotNull(
            Intent(ACTION_FOCUS_MODE)
                .setPackage(WELLBEING_PACKAGE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(ACTION_DIGITAL_WELLBEING_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            context.packageManager.getLaunchIntentForPackage(WELLBEING_PACKAGE)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        for (intent in intents) {
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    companion object {
        const val WELLBEING_PACKAGE = "com.google.android.apps.wellbeing"
        const val SETTINGS_PACKAGE = "com.android.settings"
        private const val ACTION_FOCUS_MODE = "com.google.android.apps.wellbeing.action.FOCUS_MODE"
        private const val ACTION_DIGITAL_WELLBEING_SETTINGS = "android.settings.DIGITAL_WELLBEING_SETTINGS"
    }
}
