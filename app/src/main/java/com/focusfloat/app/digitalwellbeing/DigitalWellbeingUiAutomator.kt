package com.focusfloat.app.digitalwellbeing

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class DigitalWellbeingUiAutomator(
    private val service: AccessibilityService,
    private val store: DigitalWellbeingAutomationStore,
) {
    private var activeRequestId: String? = null
    private var selectedPackages: MutableSet<String> = mutableSetOf()
    private var pendingPackages: MutableSet<String> = mutableSetOf()
    private var pendingVerificationAttempts: MutableMap<String, Int> = mutableMapOf()
    private var appListScrollAttempts = 0
    private var focusNavigationScrollAttempts = 0
    private var hasEnteredAutomationPackage = false
    private var nonAutomationWindowSince = 0L
    private var lastStepAt = 0L

    fun handleEvent(event: AccessibilityEvent?) {
        val request = store.activeRequest() ?: return
        if (activeRequestId != request.id) {
            resetForRequest(request.id)
        }

        val root = service.rootInActiveWindow
        val eventPackage = event?.packageName?.toString()
        val rootPackage = root?.packageName?.toString()
        val isInAutomationPackage = eventPackage.isAutomationPackage() || rootPackage.isAutomationPackage()
        val now = SystemClock.elapsedRealtime()

        if (isInAutomationPackage) {
            hasEnteredAutomationPackage = true
            nonAutomationWindowSince = 0L
        } else {
            if (hasEnteredAutomationPackage && !eventPackage.isIgnoredExternalPackage()) {
                if (nonAutomationWindowSince == 0L) nonAutomationWindowSince = now
                if (now - nonAutomationWindowSince >= LEAVE_GRACE_MS) {
                    store.fail(request.id, "Focus Mode assistant was interrupted after leaving Pixel Focus Mode")
                }
            }
            return
        }

        if (root == null || !rootPackage.isAutomationPackage()) return

        if (now - lastStepAt < STEP_DEBOUNCE_MS) return
        lastStepAt = now

        runCatching {
            when (request.mode) {
                DigitalWellbeingAutomationMode.EnableFocusMode -> enableFocusMode(request, root)
                DigitalWellbeingAutomationMode.DisableFocusMode -> disableFocusMode(request, root)
            }
        }.onFailure { error ->
            store.fail(request.id, error.message ?: "Focus Mode assistant failed")
        }
    }

    private fun resetForRequest(requestId: String) {
        activeRequestId = requestId
        selectedPackages = mutableSetOf()
        pendingPackages = mutableSetOf()
        pendingVerificationAttempts = mutableMapOf()
        appListScrollAttempts = 0
        focusNavigationScrollAttempts = 0
        hasEnteredAutomationPackage = false
        nonAutomationWindowSince = 0L
        lastStepAt = 0L
    }

    private fun enableFocusMode(request: DigitalWellbeingAutomationRequest, root: AccessibilityNodeInfo) {
        if (request.targets.isEmpty()) {
            store.fail(request.id, "No apps selected for Pixel Focus Mode")
            return
        }

        request.duplicateTargetLabel()?.let { duplicate ->
            store.fail(
                request.id,
                "Focus Mode assistant requires unique app labels. Duplicate label: ${duplicate.label}",
            )
            return
        }

        if (!root.looksLikeFocusModeScreen()) {
            navigateToFocusMode(request, root)
            return
        }

        focusNavigationScrollAttempts = 0
        if (root.clickText(SHOW_ALL_APPS_ACTION_TEXTS)) {
            store.updateProgress(
                requestId = request.id,
                message = "Showing all Pixel Focus Mode apps",
                completed = selectedPackages.size,
                total = request.targets.size,
            )
            return
        }

        val remaining = request.targets.filterNot { it.packageName in selectedPackages }
        if (remaining.isEmpty()) {
            store.updateProgress(request.id, "Starting Pixel Focus Mode", request.targets.size, request.targets.size)
            val turnOnAction = root.findTextAction(TURN_ON_TEXTS)
            when {
                turnOnAction?.click() == true || root.containsAnyText(FOCUS_ON_TEXTS) -> {
                    store.complete(request.id, "Pixel Focus Mode started")
                }
                root.hasNoSelectedFocusModeApps() -> {
                    store.fail(request.id, "No apps are selected in Pixel Focus Mode")
                }
                else -> {
                    store.fail(request.id, "Could not find enabled Turn on now in Pixel Focus Mode")
                }
            }
            return
        }

        var clickedAny = false
        var waitingForVerification = false
        for (target in remaining) {
            val labelNode = root.findLabelNode(target.label) ?: continue
            val selectable = labelNode.findSelectableContainer()
            if (selectable == null) {
                store.fail(request.id, "Could not verify selectable Pixel Focus Mode row for ${target.label}")
                return
            }

            if (selectable.isEffectivelyChecked()) {
                selectedPackages += target.packageName
                pendingPackages -= target.packageName
                pendingVerificationAttempts -= target.packageName
                continue
            }

            if (target.packageName in pendingPackages) {
                val attempts = (pendingVerificationAttempts[target.packageName] ?: 0) + 1
                pendingVerificationAttempts[target.packageName] = attempts
                if (attempts >= MAX_PENDING_VERIFY_ATTEMPTS) {
                    store.fail(request.id, "Pixel Focus Mode did not verify ${target.label} as selected")
                    return
                }
                waitingForVerification = true
                continue
            }

            if (selectable.clickSelectable()) {
                pendingPackages += target.packageName
                pendingVerificationAttempts[target.packageName] = 0
                clickedAny = true
            }
        }

        store.updateProgress(
            requestId = request.id,
            message = "Selected ${selectedPackages.size} of ${request.targets.size} apps",
            completed = selectedPackages.size,
            total = request.targets.size,
        )

        if (clickedAny || waitingForVerification) {
            appListScrollAttempts = 0
            return
        }

        val scrolled = appListScrollAttempts < MAX_APP_LIST_SCROLL_ATTEMPTS &&
            root.findScrollableNode(remaining.map { it.label })?.performAction(
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            ) == true
        if (scrolled) {
            appListScrollAttempts += 1
            return
        }

        val missing = remaining.take(MAX_MISSING_LABELS).joinToString(", ") { it.label }
        store.fail(request.id, "Could not find ${remaining.size} apps in Pixel Focus Mode: $missing")
    }

    private fun disableFocusMode(request: DigitalWellbeingAutomationRequest, root: AccessibilityNodeInfo) {
        if (!root.looksLikeFocusModeScreen()) {
            navigateToFocusMode(request, root)
            return
        }

        focusNavigationScrollAttempts = 0

        val turnOffAction = root.findTextAction(TURN_OFF_TEXTS)
        val focusModeIsOn = root.containsAnyText(FOCUS_ON_TEXTS)
        val focusModeIsOff = root.containsAnyText(FOCUS_OFF_TEXTS)

        if (turnOffAction?.isEnabled == true) {
            store.updateProgress(request.id, "Turning off selected Pixel Focus Mode apps", 0, 0)
            if (turnOffAction.click()) {
                store.complete(request.id, "Pixel Focus Mode stopped")
            } else {
                store.fail(request.id, "Could not turn off Pixel Focus Mode")
            }
            return
        }

        val noSelectedApps = root.hasNoSelectedFocusModeApps()
        when {
            focusModeIsOff -> store.fail(request.id, "Pixel Focus Mode is already off")
            noSelectedApps -> store.fail(request.id, "No apps are selected in Pixel Focus Mode")
            turnOffAction != null && !focusModeIsOn -> store.fail(request.id, "Pixel Focus Mode is already off")
            !focusModeIsOn -> store.fail(request.id, "Pixel Focus Mode is already off")
            else -> store.fail(request.id, "Could not find enabled Turn off in Pixel Focus Mode")
        }
    }

    private fun navigateToFocusMode(
        request: DigitalWellbeingAutomationRequest,
        root: AccessibilityNodeInfo,
    ) {
        val completed = if (request.mode == DigitalWellbeingAutomationMode.EnableFocusMode) selectedPackages.size else 0
        val total = if (request.mode == DigitalWellbeingAutomationMode.EnableFocusMode) request.targets.size else 0
        val message = when (request.mode) {
            DigitalWellbeingAutomationMode.EnableFocusMode -> "Opening Pixel Focus Mode to select apps"
            DigitalWellbeingAutomationMode.DisableFocusMode -> "Opening Pixel Focus Mode to check selected apps"
        }
        store.updateProgress(request.id, message, completed, total)
        if (root.clickText(FOCUS_MODE_TEXTS)) {
            focusNavigationScrollAttempts = 0
            return
        }

        val scrolled = focusNavigationScrollAttempts < MAX_FOCUS_NAVIGATION_SCROLL_ATTEMPTS &&
            root.findScrollableNode(FOCUS_MODE_TEXTS)?.performAction(
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            ) == true
        if (scrolled) {
            focusNavigationScrollAttempts += 1
            return
        }

        store.fail(request.id, "Could not find Pixel Focus Mode")
    }

    private fun AccessibilityNodeInfo.looksLikeFocusModeScreen(): Boolean {
        val hasFocusModeText = containsAnyText(FOCUS_MODE_SCREEN_TITLE_TEXTS)
        val hasActionText = containsAnyText(TURN_ON_TEXTS + TURN_OFF_TEXTS + FOCUS_ON_TEXTS + FOCUS_OFF_TEXTS)
        val hasListText = containsAnyText(FOCUS_SCREEN_TEXTS + SHOW_ALL_APPS_ACTION_TEXTS + APP_LIST_TEXTS)
        val hasSelectableFocusList = containsAnyText(FOCUS_SCREEN_TEXTS) && hasActionText
        return (hasFocusModeText && (hasActionText || hasListText)) || hasSelectableFocusList
    }

    private fun AccessibilityNodeInfo.clickText(keywords: List<String>): Boolean {
        return findTextAction(keywords)?.click() == true
    }

    private fun AccessibilityNodeInfo.containsAnyText(keywords: List<String>): Boolean {
        return allNodes().any { node ->
            val text = node.visibleText()
            text != null && keywords.any { keyword -> text.matchesKeyword(keyword) }
        }
    }

    private fun AccessibilityNodeInfo.findTextAction(keywords: List<String>): TextAction? {
        val actions = allNodes()
            .filter { node ->
                val text = node.visibleText()
                text != null && keywords.any { keyword -> text.matchesKeyword(keyword) }
            }
            .map { node ->
                TextAction(node.findClickableSelfOrParent())
            }
        return actions.firstOrNull { it.isEnabled } ?: actions.firstOrNull()
    }

    private fun AccessibilityNodeInfo.hasNoSelectedFocusModeApps(): Boolean {
        if (containsAnyText(NO_SELECTED_APPS_TEXTS)) return true

        val checkableNodes = allNodes().filter { it.isCheckable }
        if (checkableNodes.isNotEmpty()) {
            return checkableNodes.none { it.isEffectivelyChecked() }
        }

        return containsAnyText(FOCUS_SCREEN_TEXTS)
    }

    private fun AccessibilityNodeInfo.findLabelNode(label: String): AccessibilityNodeInfo? {
        val normalizedLabel = label.normalized()
        if (normalizedLabel.isBlank()) return null
        return allNodes().firstOrNull { node ->
            val text = node.visibleText()?.normalized() ?: return@firstOrNull false
            text == normalizedLabel || text.removeSuffix(" paused") == normalizedLabel
        }
    }

    private fun AccessibilityNodeInfo.findSelectableContainer(): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = this
        repeat(PARENT_SEARCH_DEPTH) {
            current?.findCheckableDescendant()?.let { return it }
            if (current?.isCheckable == true) return current
            current = current?.parent
        }
        return findCheckableDescendant()
    }

    private fun AccessibilityNodeInfo.findCheckableDescendant(): AccessibilityNodeInfo? {
        if (isCheckable) return this
        for (index in 0 until childCount) {
            val match = getChild(index)?.findCheckableDescendant()
            if (match != null) return match
        }
        return null
    }

    private fun AccessibilityNodeInfo.isEffectivelyChecked(): Boolean {
        if (isChecked || isSelected) return true
        val text = visibleText()?.normalized() ?: return false
        if (UNCHECKED_STATE_TEXTS.any { text.contains(it) }) return false
        return CHECKED_STATE_TEXTS.any { text.contains(it) }
    }

    private fun AccessibilityNodeInfo.clickSelectable(): Boolean {
        if (isEnabled && performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        val clickTarget = findClickableSelfOrParent() ?: return false
        return clickTarget.isEnabled && clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun AccessibilityNodeInfo.findScrollableNode(preferredTexts: List<String>): AccessibilityNodeInfo? {
        return allNodes()
            .filter { it.isScrollable }
            .maxWithOrNull(
                compareBy<AccessibilityNodeInfo> { it.scrollScore(preferredTexts) }
                    .thenBy { it.boundsArea() },
            )
    }

    private fun AccessibilityNodeInfo.scrollScore(preferredTexts: List<String>): Int {
        var score = 0
        if (containsAnyText(preferredTexts)) score += 100
        if (containsAnyText(FOCUS_SCREEN_TEXTS + APP_LIST_TEXTS)) score += 40
        if (boundsArea() > MIN_SCROLLABLE_AREA) score += 10
        return score
    }

    private fun AccessibilityNodeInfo.boundsArea(): Int {
        val bounds = Rect()
        getBoundsInScreen(bounds)
        return bounds.width().coerceAtLeast(0) * bounds.height().coerceAtLeast(0)
    }

    private fun AccessibilityNodeInfo.findClickableSelfOrParent(): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = this
        repeat(PARENT_SEARCH_DEPTH) {
            val node = current ?: return null
            if (node.isClickable) return node
            current = node.parent
        }
        return null
    }

    private fun AccessibilityNodeInfo.allNodes(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null) return
            result += node
            for (index in 0 until node.childCount) {
                visit(node.getChild(index))
            }
        }
        visit(this)
        return result
    }

    private fun AccessibilityNodeInfo.visibleText(): String? {
        return sequenceOf(text, contentDescription)
            .mapNotNull { it?.toString()?.trim() }
            .firstOrNull { it.isNotBlank() }
    }

    private fun DigitalWellbeingAutomationRequest.duplicateTargetLabel(): DigitalWellbeingAutomationTarget? {
        val seen = mutableSetOf<String>()
        return targets.firstOrNull { target ->
            val normalizedLabel = target.label.normalized()
            normalizedLabel.isNotBlank() && !seen.add(normalizedLabel)
        }
    }

    private fun String?.isAutomationPackage(): Boolean {
        return this in AUTOMATION_PACKAGES
    }

    private fun String?.isIgnoredExternalPackage(): Boolean {
        return this == null || this in IGNORED_EXTERNAL_PACKAGES
    }

    private fun String.matchesKeyword(keyword: String): Boolean {
        val text = normalized()
        val needle = keyword.normalized()
        return text == needle || text.contains(needle)
    }

    private fun String.normalized(): String {
        return lowercase(Locale.getDefault())
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class TextAction(
        val clickTarget: AccessibilityNodeInfo?,
    ) {
        val isEnabled: Boolean
            get() = clickTarget?.isEnabled == true

        fun click(): Boolean {
            return isEnabled && clickTarget?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        }
    }

    private companion object {
        const val STEP_DEBOUNCE_MS = 650L
        const val LEAVE_GRACE_MS = 8_000L
        const val MAX_FOCUS_NAVIGATION_SCROLL_ATTEMPTS = 12
        const val MAX_APP_LIST_SCROLL_ATTEMPTS = 80
        const val MAX_PENDING_VERIFY_ATTEMPTS = 3
        const val MAX_MISSING_LABELS = 5
        const val PARENT_SEARCH_DEPTH = 6
        const val MIN_SCROLLABLE_AREA = 120_000

        val AUTOMATION_PACKAGES = setOf(
            DigitalWellbeingLauncher.WELLBEING_PACKAGE,
            DigitalWellbeingLauncher.SETTINGS_PACKAGE,
        )
        val IGNORED_EXTERNAL_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.google.android.inputmethod.latin",
            "com.focusfloat.app",
        )
        val FOCUS_MODE_TEXTS = listOf(
            "Focus mode",
            "Режим фокусировки",
            "Фокусировка",
        )
        val FOCUS_MODE_SCREEN_TITLE_TEXTS = FOCUS_MODE_TEXTS + listOf(
            "Focus",
            "Фокус",
        )
        val FOCUS_SCREEN_TEXTS = listOf(
            "Distracting apps",
            "Choose distracting apps",
            "Select distracting apps",
            "Выберите отвлекающие приложения",
            "Отвлекающие приложения",
        )
        val SHOW_ALL_APPS_ACTION_TEXTS = listOf(
            "Show all apps",
            "See all apps",
            "Показать все приложения",
        )
        val APP_LIST_TEXTS = listOf(
            "All apps",
            "Все приложения",
        )
        val TURN_ON_TEXTS = listOf(
            "Turn on now",
            "Start now",
            "Включить сейчас",
            "Запустить сейчас",
        )
        val TURN_OFF_TEXTS = listOf(
            "Turn off now",
            "Turn off Focus mode",
            "Turn off",
            "End now",
            "Выключить сейчас",
            "Отключить",
        )
        val NO_SELECTED_APPS_TEXTS = listOf(
            "0 apps selected",
            "0 app selected",
            "No apps selected",
            "No distracting apps selected",
            "0 приложений выбрано",
            "Приложения не выбраны",
            "Нет выбранных приложений",
        )
        val CHECKED_STATE_TEXTS = listOf(
            "checked",
            "selected",
            "выбрано",
            "отмечено",
        )
        val UNCHECKED_STATE_TEXTS = listOf(
            "not checked",
            "not selected",
            "unchecked",
            "не выбрано",
            "не отмечено",
        )
        val FOCUS_ON_TEXTS = listOf(
            "Focus mode is on",
            "Режим фокусировки включен",
        )
        val FOCUS_OFF_TEXTS = listOf(
            "Focus mode is off",
            "Режим фокусировки выключен",
        )
    }
}
