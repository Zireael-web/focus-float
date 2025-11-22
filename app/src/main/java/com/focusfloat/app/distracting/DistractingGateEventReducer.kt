package com.focusfloat.app.distracting

import com.focusfloat.app.core.model.AppRef

internal enum class DistractingGateWindowEventType {
    WindowStateChanged,
    WindowsChanged,
}

internal data class DistractingGateWindowEvent(
    val type: DistractingGateWindowEventType,
    val packageName: String,
    val distractingRefs: Set<AppRef>,
    val labelsByRef: Map<AppRef, String>,
    val ignoredPackages: Set<String>,
)

internal sealed interface DistractingGateAction {
    data class UpdateForeground(val packageName: String) : DistractingGateAction
    data class CancelReminder(val packageName: String) : DistractingGateAction
    data class ScheduleReminder(val ref: AppRef, val label: String) : DistractingGateAction
    data class ShowGate(val ref: AppRef, val label: String) : DistractingGateAction
}

internal class DistractingGateEventReducer(
    private val approvals: DistractingGateApprovalRegistry = DistractingGateApprovals,
) {
    private var lastForegroundPackage: String? = null
    private var lastGatePackage: String? = null

    fun reduce(event: DistractingGateWindowEvent): List<DistractingGateAction> {
        if (event.packageName in event.ignoredPackages) return emptyList()

        val previous = lastForegroundPackage
        val ref = event.distractingRefs.firstOrNull { it.packageName == event.packageName }
        val actions = mutableListOf<DistractingGateAction>()

        if (
            previous != null &&
            previous != event.packageName &&
            event.distractingRefs.any { it.packageName == previous }
        ) {
            actions += DistractingGateAction.CancelReminder(previous)
        }

        lastForegroundPackage = event.packageName
        actions += DistractingGateAction.UpdateForeground(event.packageName)

        if (ref == null) {
            lastGatePackage = null
            return actions
        }

        val label = event.labelsByRef[ref] ?: event.packageName
        if (approvals.redeem(event.packageName)) {
            lastGatePackage = null
            actions += DistractingGateAction.ScheduleReminder(ref, label)
            return actions
        }

        if (previous == event.packageName || lastGatePackage == event.packageName) return actions

        lastGatePackage = event.packageName
        actions += DistractingGateAction.ShowGate(ref, label)
        return actions
    }
}
