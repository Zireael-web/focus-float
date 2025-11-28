package com.focusfloat.app.distracting

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
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
    private val reducer = DistractingGateEventReducer()
    private var distractingRefs: Set<AppRef> = emptySet()
    private var labelsByRef: Map<AppRef, String> = emptyMap()
    private var defaultInputMethodPackage: String? = null
    private var defaultInputMethodPackageLoaded = false

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
        val eventType = event.windowEventType() ?: return

        val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        val gateEvent = DistractingGateWindowEvent(
            type = eventType,
            packageName = packageName,
            distractingRefs = distractingRefs,
            labelsByRef = labelsByRef,
            ignoredPackages = ignoredPackages(),
        )
        reducer.reduce(gateEvent).forEach(::applyAction)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun List<AppEntry>.associateLabels(): Map<AppRef, String> {
        return associate { it.key.ref to it.displayLabel }
    }

    private fun applyAction(action: DistractingGateAction) {
        val container = (application as FocusFloatApplication).container
        when (action) {
            is DistractingGateAction.UpdateForeground -> {
                DistractingForegroundState.update(action.packageName)
            }
            is DistractingGateAction.CancelReminder -> {
                container.distractingReminderScheduler.cancelPackage(action.packageName)
            }
            is DistractingGateAction.ScheduleReminder -> {
                container.distractingReminderScheduler.schedule(action.ref.packageName, action.ref.userSerial, action.label)
            }
            is DistractingGateAction.ShowGate -> {
                val intent = Intent(this, DistractingGateActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(DistractingGateActivity.EXTRA_PACKAGE_NAME, action.ref.packageName)
                    .putExtra(DistractingGateActivity.EXTRA_APP_LABEL, action.label)
                    .putExtra(DistractingGateActivity.EXTRA_USER_SERIAL, action.ref.userSerial)
                runCatching { startActivity(intent) }
            }
        }
    }

    private fun AccessibilityEvent.windowEventType(): DistractingGateWindowEventType? {
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> DistractingGateWindowEventType.WindowStateChanged
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> DistractingGateWindowEventType.WindowsChanged
            else -> null
        }
    }

    private fun ignoredPackages(): Set<String> {
        if (!defaultInputMethodPackageLoaded) {
            defaultInputMethodPackage = readDefaultInputMethodPackage()
            defaultInputMethodPackageLoaded = true
        }
        return buildSet {
            addAll(BASE_IGNORED_PACKAGES)
            add(packageName)
            defaultInputMethodPackage?.let(::add)
        }
    }

    private fun readDefaultInputMethodPackage(): String? {
        return runCatching {
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore("/")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private companion object {
        val BASE_IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.google.android.inputmethod.latin",
        )
    }
}
