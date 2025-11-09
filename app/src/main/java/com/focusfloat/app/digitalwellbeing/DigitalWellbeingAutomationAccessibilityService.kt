package com.focusfloat.app.digitalwellbeing

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.focusfloat.app.FocusFloatApplication

class DigitalWellbeingAutomationAccessibilityService : AccessibilityService() {
    private lateinit var automator: DigitalWellbeingUiAutomator
    private val handler = Handler(Looper.getMainLooper())
    private val automationPulse = object : Runnable {
        override fun run() {
            if (::automator.isInitialized) {
                automator.handleEvent(null)
            }
            handler.postDelayed(this, AUTOMATION_PULSE_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = (application as FocusFloatApplication).container
        automator = DigitalWellbeingUiAutomator(this, container.digitalWellbeingAutomationStore)
        handler.removeCallbacks(automationPulse)
        handler.post(automationPulse)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (::automator.isInitialized) {
            automator.handleEvent(event)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(automationPulse)
        super.onDestroy()
    }

    private companion object {
        const val AUTOMATION_PULSE_MS = 700L
    }
}
