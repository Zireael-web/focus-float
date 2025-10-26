package com.focusfloat.app.distracting

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

object DistractingGateApprovals {
    private const val APPROVAL_WINDOW_MS = 8_000L
    private val approvedUntil = ConcurrentHashMap<String, Long>()

    fun approve(packageName: String) {
        approvedUntil[packageName] = SystemClock.elapsedRealtime() + APPROVAL_WINDOW_MS
    }

    fun isTemporarilyApproved(packageName: String): Boolean {
        val expiry = approvedUntil[packageName] ?: return false
        val active = SystemClock.elapsedRealtime() <= expiry
        if (!active) approvedUntil.remove(packageName, expiry)
        return active
    }
}

object DistractingForegroundState {
    @Volatile
    private var foregroundPackage: String? = null

    fun update(packageName: String?) {
        foregroundPackage = packageName
    }

    fun shouldShowReminder(packageName: String): Boolean {
        return foregroundPackage == packageName
    }
}
