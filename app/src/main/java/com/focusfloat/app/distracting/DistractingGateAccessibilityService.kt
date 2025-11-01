package com.focusfloat.app.distracting

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focusfloat.app.FocusFloatApplication
import com.focusfloat.app.core.model.AppEntry
import com.focusfloat.app.core.model.AppRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DistractingGateAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var distractingRefs: Set<AppRef> = emptySet()
    private var labelsByRef: Map<AppRef, String> = emptyMap()
    private var lastForegroundPackage: String? = null
    private var lastGatePackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = (application as FocusFloatApplication).container
        scope.launch {
            val distractingCategoryId = withContext(Dispatchers.IO) {
                container.categoryRepository.ensureDefaultDistractingCategory()
            }
            combine(
                container.categoryRepository.observeCategoryItems(distractingCategoryId),
                container.installedAppsRepository.observeApps(),
            ) { refs, apps ->
                refs.toSet() to apps.associateLabels()
            }.collect { (refs, labels) ->
                distractingRefs = refs
                labelsByRef = labels
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        val previous = lastForegroundPackage
        if (previous != null && previous != packageName && distractingRefs.any { it.packageName == previous }) {
            (application as FocusFloatApplication).container.distractingReminderScheduler.cancelPackage(previous)
        }

        if (packageName == this.packageName) {
            DistractingForegroundState.update(packageName)
            lastForegroundPackage = packageName
            lastGatePackage = null
            return
        }

        if (previous == packageName) return
        lastForegroundPackage = packageName
        DistractingForegroundState.update(packageName)

        val ref = distractingRefs.firstOrNull { it.packageName == packageName }
        if (ref == null) {
            lastGatePackage = null
            return
        }
        if (DistractingGateApprovals.isTemporarilyApproved(packageName)) {
            val label = labelsByRef[ref] ?: packageName
            (application as FocusFloatApplication).container.distractingReminderScheduler
                .schedule(packageName, ref.userSerial, label)
            lastGatePackage = null
            return
        }

        if (lastGatePackage == packageName) return
        lastGatePackage = packageName
        val intent = Intent(this, DistractingGateActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(DistractingGateActivity.EXTRA_PACKAGE_NAME, packageName)
            .putExtra(DistractingGateActivity.EXTRA_APP_LABEL, labelsByRef[ref] ?: packageName)
            .putExtra(DistractingGateActivity.EXTRA_USER_SERIAL, ref.userSerial)
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun List<AppEntry>.associateLabels(): Map<AppRef, String> {
        return associate { it.key.ref to it.displayLabel }
    }
}
