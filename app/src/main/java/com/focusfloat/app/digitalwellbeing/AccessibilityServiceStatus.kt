package com.focusfloat.app.digitalwellbeing

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

class AccessibilityServiceStatus(private val context: Context) {
    fun isEnabled(serviceClass: Class<*>): Boolean {
        val expected = ComponentName(context, serviceClass).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
