package com.focusfloat.app.distracting

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

internal interface DistractingGateApprovalRegistry {
    fun redeem(packageName: String): Boolean
}

internal class DistractingGateApprovalStore(
    private val nowElapsedMs: () -> Long,
) : DistractingGateApprovalRegistry {
    private val approvedUntil = ConcurrentHashMap<String, Long>()

    fun approve(packageName: String) {
        approvedUntil[packageName] = nowElapsedMs() + APPROVAL_TTL_MS
    }

    override fun redeem(packageName: String): Boolean {
        val expiry = approvedUntil[packageName] ?: return false
        val active = nowElapsedMs() <= expiry
        approvedUntil.remove(packageName, expiry)
        return active
    }

    fun clear() {
        approvedUntil.clear()
    }

    private companion object {
        private const val APPROVAL_TTL_MS = 60_000L
    }
}

object DistractingGateApprovals : DistractingGateApprovalRegistry {
    private val store = DistractingGateApprovalStore(SystemClock::elapsedRealtime)

    fun approve(packageName: String) {
        store.approve(packageName)
    }

    override fun redeem(packageName: String): Boolean {
        return store.redeem(packageName)
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
