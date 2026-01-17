package com.focusfloat.app.digitalwellbeing

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationEngine.Companion.AUTOMATION_PACKAGES
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationEngine.Companion.IGNORED_EXTERNAL_PACKAGES
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationEngine.Companion.diagnosticSummary

class DigitalWellbeingUiAutomator(
    private val service: AccessibilityService,
    private val store: DigitalWellbeingAutomationStore,
) {
    private val engine = DigitalWellbeingAutomationEngine()
    private var activeRequestId: String? = null
    private var hasEnteredAutomationPackage = false
    private var nonAutomationWindowSince = 0L
    private var lastStepAt = 0L

    fun handleEvent(event: AccessibilityEvent?): Boolean {
        val request = store.activeRequest() ?: return false
        if (activeRequestId != request.id) {
            resetForRequest(request.id)
        }

        val root = service.rootInActiveWindow
        val eventPackage = event?.packageName?.toString()
        val rootPackage = root?.packageName?.toString()
        val activeWindowPackage = eventPackage ?: rootPackage
        val isInAutomationPackage = eventPackage.isAutomationPackage() || rootPackage.isAutomationPackage()
        val now = SystemClock.elapsedRealtime()

        if (isInAutomationPackage) {
            hasEnteredAutomationPackage = true
            nonAutomationWindowSince = 0L
        } else {
            if (hasEnteredAutomationPackage && (root == null || !activeWindowPackage.isIgnoredExternalPackage())) {
                if (nonAutomationWindowSince == 0L) nonAutomationWindowSince = now
                if (now - nonAutomationWindowSince >= LEAVE_GRACE_MS) {
                    fail(
                        request = request,
                        message = "Focus Mode assistant was interrupted after leaving Pixel Focus Mode",
                        root = root?.let(::AccessibilityNodeAutomationNode),
                    )
                }
            }
            return true
        }

        if (root == null || !rootPackage.isAutomationPackage()) return true

        if (now - lastStepAt < STEP_DEBOUNCE_MS) return true
        lastStepAt = now

        val automationRoot = AccessibilityNodeAutomationNode(root)
        val callbacks = StoreCallbacks(request, automationRoot)
        runCatching {
            when (request.mode) {
                DigitalWellbeingAutomationMode.EnableFocusMode -> engine.enableFocusMode(request, automationRoot, callbacks)
                DigitalWellbeingAutomationMode.DisableFocusMode -> engine.disableFocusMode(request, automationRoot, callbacks)
            }
        }.onFailure { error ->
            fail(request, error.message ?: "Focus Mode assistant failed", automationRoot)
        }
        return true
    }

    private fun resetForRequest(requestId: String) {
        activeRequestId = requestId
        engine.resetForRequest()
        hasEnteredAutomationPackage = false
        nonAutomationWindowSince = 0L
        lastStepAt = 0L
    }

    private fun fail(
        request: DigitalWellbeingAutomationRequest,
        message: String,
        root: DigitalWellbeingAutomationNode?,
    ) {
        val diagnostic = root?.diagnosticSummary()
        val fullMessage = listOfNotNull(message, diagnostic).joinToString("\n")
        store.fail(request.id, fullMessage)
    }

    private fun String?.isAutomationPackage(): Boolean {
        return this in AUTOMATION_PACKAGES
    }

    private fun String?.isIgnoredExternalPackage(): Boolean {
        return this == null || this in IGNORED_EXTERNAL_PACKAGES
    }

    private inner class StoreCallbacks(
        private val request: DigitalWellbeingAutomationRequest,
        private val root: DigitalWellbeingAutomationNode,
    ) : DigitalWellbeingAutomationEngine.Callbacks {
        override fun updateProgress(message: String, completed: Int, total: Int) {
            store.updateProgress(request.id, message, completed, total)
        }

        override fun complete(message: String) {
            store.complete(request.id, message)
        }

        override fun fail(message: String) {
            fail(request, message, root)
        }
    }

    private companion object {
        const val STEP_DEBOUNCE_MS = 650L
        const val LEAVE_GRACE_MS = 8_000L
    }
}
