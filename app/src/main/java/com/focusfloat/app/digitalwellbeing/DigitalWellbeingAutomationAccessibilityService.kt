package com.focusfloat.app.digitalwellbeing

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.focusfloat.app.FocusFloatApplication

class DigitalWellbeingAutomationAccessibilityService : AccessibilityService() {
    private lateinit var automator: DigitalWellbeingUiAutomator
    private val handler = Handler(Looper.getMainLooper())
    private var idlePulseCount = 0
    private val automationPulse = object : Runnable {
        override fun run() {
            val hasActiveRequest = if (::automator.isInitialized) {
                automator.handleEvent(null)
            } else {
                false
            }
            scheduleNextPulse(hasActiveRequest)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = (application as FocusFloatApplication).container
        automator = DigitalWellbeingUiAutomator(this, container.digitalWellbeingAutomationStore)
        idlePulseCount = 0
        handler.removeCallbacks(automationPulse)
        handler.post(automationPulse)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (::automator.isInitialized) {
            val hasActiveRequest = automator.handleEvent(event)
            if (hasActiveRequest) {
                idlePulseCount = 0
                handler.removeCallbacks(automationPulse)
                handler.postDelayed(automationPulse, ACTIVE_AUTOMATION_PULSE_MS)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(automationPulse)
        super.onDestroy()
    }

    private fun scheduleNextPulse(hasActiveRequest: Boolean) {
        if (hasActiveRequest) {
            idlePulseCount = 0
        } else {
            idlePulseCount = (idlePulseCount + 1).coerceAtMost(IDLE_PULSES_BEFORE_SLOW)
        }
        val delayMs = if (idlePulseCount >= IDLE_PULSES_BEFORE_SLOW) {
            IDLE_AUTOMATION_PULSE_MS
        } else {
            ACTIVE_AUTOMATION_PULSE_MS
        }
        handler.postDelayed(automationPulse, delayMs)
    }

    private companion object {
        const val ACTIVE_AUTOMATION_PULSE_MS = 700L
        const val IDLE_AUTOMATION_PULSE_MS = 2_500L
        const val IDLE_PULSES_BEFORE_SLOW = 3
    }
}
