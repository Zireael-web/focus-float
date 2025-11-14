package com.focusfloat.app.digitalwellbeing

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

class AccessibilityServiceStatus(private val context: Context) {
    fun isEnabled(serviceClass: Class<*>): Boolean {
        val expected = ComponentName(context, serviceClass)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { entry ->
            val component = ComponentName.unflattenFromString(entry) ?: return@any false
            component == expected || accessibilityServiceEntryMatches(
                entry = entry,
                expectedPackageName = expected.packageName,
                expectedClassName = expected.className,
            )
        }
    }
}

internal fun accessibilityServiceEntryMatches(
    entry: String,
    expectedPackageName: String,
    expectedClassName: String,
): Boolean {
    val trimmed = entry.trim()
    if (trimmed.isBlank()) return false
    val slash = trimmed.indexOf('/')
    if (slash <= 0 || slash == trimmed.lastIndex) return false

    val packageName = trimmed.substring(0, slash)
    val classPart = trimmed.substring(slash + 1)
    if (!packageName.equals(expectedPackageName, ignoreCase = true)) return false

    val fullClassName = if (classPart.startsWith(".")) {
        packageName + classPart
    } else {
        classPart
    }
    return fullClassName.equals(expectedClassName, ignoreCase = true)
}
