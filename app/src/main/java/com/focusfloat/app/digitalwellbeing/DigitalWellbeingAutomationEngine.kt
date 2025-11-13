package com.focusfloat.app.digitalwellbeing

import java.util.Locale

internal class DigitalWellbeingAutomationEngine {
    private var selectedPackages: MutableSet<String> = mutableSetOf()
    private var pendingPackages: MutableSet<String> = mutableSetOf()
    private var pendingVerificationAttempts: MutableMap<String, Int> = mutableMapOf()
    private var appListScrollAttempts = 0
    private var focusNavigationScrollAttempts = 0
    private var showAllAppsClicks = 0

    fun resetForRequest() {
        selectedPackages = mutableSetOf()
        pendingPackages = mutableSetOf()
        pendingVerificationAttempts = mutableMapOf()
        appListScrollAttempts = 0
        focusNavigationScrollAttempts = 0
        showAllAppsClicks = 0
    }

    fun enableFocusMode(
        request: DigitalWellbeingAutomationRequest,
        root: DigitalWellbeingAutomationNode,
        callbacks: Callbacks,
    ) {
        if (request.targets.isEmpty()) {
            callbacks.fail("No apps selected for Pixel Focus Mode")
            return
        }

        request.duplicateTargetLabel()?.let { duplicate ->
            callbacks.fail("Focus Mode assistant requires unique app labels. Duplicate label: ${duplicate.label}")
            return
        }

        if (!root.looksLikeFocusModeScreen()) {
            navigateToFocusMode(request, root, callbacks)
            return
        }

        focusNavigationScrollAttempts = 0
        val remaining = request.targets.filterNot { it.packageName in selectedPackages }
        if (
            remaining.isNotEmpty() &&
            showAllAppsClicks < MAX_SHOW_ALL_APPS_CLICKS &&
            root.clickText(SHOW_ALL_APPS_ACTION_TEXTS)
        ) {
            showAllAppsClicks += 1
            callbacks.updateProgress(
                message = "Showing all Pixel Focus Mode apps",
                completed = selectedPackages.size,
                total = request.targets.size,
            )
            return
        }

        if (remaining.isEmpty()) {
            val missingChecked = root.missingCheckedTargets(request.targets)
            if (missingChecked.isNotEmpty()) {
                callbacks.fail("Pixel Focus Mode did not verify selected apps: ${missingChecked.joinToString(", ")}")
                return
            }

            callbacks.updateProgress("Starting Pixel Focus Mode", request.targets.size, request.targets.size)
            val turnOnAction = root.findTextAction(TURN_ON_TEXTS, allowContains = false)
            when {
                turnOnAction?.click() == true || root.containsAnyText(FOCUS_ON_TEXTS) -> {
                    callbacks.complete("Pixel Focus Mode started")
                }
                root.hasNoSelectedFocusModeApps() -> {
                    callbacks.fail("No apps are selected in Pixel Focus Mode")
                }
                else -> {
                    callbacks.fail("Could not find enabled Turn on now in Pixel Focus Mode")
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
                callbacks.fail("Could not verify selectable Pixel Focus Mode row for ${target.label}")
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
                    callbacks.fail("Pixel Focus Mode did not verify ${target.label} as selected")
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

        callbacks.updateProgress(
            message = "Selected ${selectedPackages.size} of ${request.targets.size} apps",
            completed = selectedPackages.size,
            total = request.targets.size,
        )

        if (clickedAny || waitingForVerification) {
            appListScrollAttempts = 0
            return
        }

        val scrolled = appListScrollAttempts < MAX_APP_LIST_SCROLL_ATTEMPTS &&
            root.findScrollableNode(remaining.map { it.label })?.scrollForward() == true
        if (scrolled) {
            appListScrollAttempts += 1
            return
        }

        val missing = remaining.take(MAX_MISSING_LABELS).joinToString(", ") { it.label }
        callbacks.fail("Could not find ${remaining.size} apps in Pixel Focus Mode: $missing")
    }

    fun disableFocusMode(
        request: DigitalWellbeingAutomationRequest,
        root: DigitalWellbeingAutomationNode,
        callbacks: Callbacks,
    ) {
        if (!root.looksLikeFocusModeScreen()) {
            navigateToFocusMode(request, root, callbacks)
            return
        }

        focusNavigationScrollAttempts = 0

        val turnOffAction = root.findTextAction(TURN_OFF_TEXTS, allowContains = false)
        val focusModeIsOn = root.containsAnyText(FOCUS_ON_TEXTS)
        val focusModeIsOff = root.containsAnyText(FOCUS_OFF_TEXTS)

        if (turnOffAction?.isEnabled == true) {
            callbacks.updateProgress("Turning off selected Pixel Focus Mode apps", 0, 0)
            if (turnOffAction.click()) {
                callbacks.complete("Pixel Focus Mode stopped")
            } else {
                callbacks.fail("Could not turn off Pixel Focus Mode")
            }
            return
        }

        val noSelectedApps = root.hasNoSelectedFocusModeApps()
        when {
            focusModeIsOff -> callbacks.fail("Pixel Focus Mode is already off")
            noSelectedApps -> callbacks.fail("No apps are selected in Pixel Focus Mode")
            turnOffAction != null && !focusModeIsOn -> callbacks.fail("Pixel Focus Mode is already off")
            !focusModeIsOn -> callbacks.fail("Pixel Focus Mode is already off")
            else -> callbacks.fail("Could not find enabled Turn off in Pixel Focus Mode")
        }
    }

    private fun navigateToFocusMode(
        request: DigitalWellbeingAutomationRequest,
        root: DigitalWellbeingAutomationNode,
        callbacks: Callbacks,
    ) {
        val completed = if (request.mode == DigitalWellbeingAutomationMode.EnableFocusMode) selectedPackages.size else 0
        val total = if (request.mode == DigitalWellbeingAutomationMode.EnableFocusMode) request.targets.size else 0
        val message = when (request.mode) {
            DigitalWellbeingAutomationMode.EnableFocusMode -> "Opening Pixel Focus Mode to select apps"
            DigitalWellbeingAutomationMode.DisableFocusMode -> "Opening Pixel Focus Mode to check selected apps"
        }
        callbacks.updateProgress(message, completed, total)
        if (root.clickText(FOCUS_MODE_TEXTS)) {
            focusNavigationScrollAttempts = 0
            return
        }

        val scrolled = focusNavigationScrollAttempts < MAX_FOCUS_NAVIGATION_SCROLL_ATTEMPTS &&
            root.findScrollableNode(FOCUS_MODE_TEXTS)?.scrollForward() == true
        if (scrolled) {
            focusNavigationScrollAttempts += 1
            return
        }

        callbacks.fail("Could not find Pixel Focus Mode")
    }

    interface Callbacks {
        fun updateProgress(message: String, completed: Int, total: Int)
        fun complete(message: String)
        fun fail(message: String)
    }

    internal companion object {
        const val MAX_MISSING_LABELS = 5

        private const val MAX_FOCUS_NAVIGATION_SCROLL_ATTEMPTS = 12
        private const val MAX_APP_LIST_SCROLL_ATTEMPTS = 80
        private const val MAX_PENDING_VERIFY_ATTEMPTS = 3
        private const val MAX_SHOW_ALL_APPS_CLICKS = 2
        private const val PARENT_SEARCH_DEPTH = 6
        private const val MIN_SCROLLABLE_AREA = 120_000
        private const val DIAGNOSTIC_TEXT_LIMIT = 20
        private const val DIAGNOSTIC_TEXT_LENGTH = 40
        private const val DIAGNOSTIC_TOTAL_LENGTH = 500

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

        fun String.normalized(): String {
            return lowercase(Locale.ROOT)
                .replace('\n', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun DigitalWellbeingAutomationNode.visibleText(): String? {
            return sequenceOf(text, contentDescription)
                .mapNotNull { it?.trim() }
                .firstOrNull { it.isNotBlank() }
        }

        fun DigitalWellbeingAutomationNode.diagnosticSummary(): String {
            val texts = allNodes()
                .mapNotNull { it.visibleText()?.replace('\n', ' ')?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(DIAGNOSTIC_TEXT_LIMIT)
                .map { it.take(DIAGNOSTIC_TEXT_LENGTH) }
                .joinToString(", ")
                .take(DIAGNOSTIC_TOTAL_LENGTH)
            return "seen: package=${packageName ?: "unknown"}; texts=[$texts]"
        }

        fun String.matchesKeyword(keyword: String): MatchQuality? {
            val text = normalized()
            val needle = keyword.normalized()
            return when {
                text == needle -> MatchQuality.Exact
                text.contains(needle) -> MatchQuality.Contains(text.length)
                else -> null
            }
        }

        fun DigitalWellbeingAutomationNode.containsAnyText(keywords: List<String>): Boolean {
            return allNodes().any { node ->
                val text = node.visibleText()
                text != null && keywords.any { keyword -> text.matchesKeyword(keyword) != null }
            }
        }

        fun DigitalWellbeingAutomationNode.findTextAction(
            keywords: List<String>,
            allowContains: Boolean = true,
        ): TextAction? {
            val actions = allNodes()
                .mapNotNull { node ->
                    val text = node.visibleText() ?: return@mapNotNull null
                    val match = keywords
                        .mapNotNull { keyword -> text.matchesKeyword(keyword) }
                        .filter { allowContains || it == MatchQuality.Exact }
                        .minOrNull()
                        ?: return@mapNotNull null
                    TextAction(node.findClickableSelfOrParent(), match)
                }
                .sortedWith(compareBy<TextAction> { it.matchQuality }.thenBy { it.matchLength })
            return actions.firstOrNull { it.isEnabled } ?: actions.firstOrNull()
        }

        fun DigitalWellbeingAutomationNode.clickText(keywords: List<String>): Boolean {
            return findTextAction(keywords)?.click() == true
        }

        fun DigitalWellbeingAutomationNode.looksLikeFocusModeScreen(): Boolean {
            val hasFocusModeText = containsAnyText(FOCUS_MODE_SCREEN_TITLE_TEXTS)
            val hasActionText = containsAnyText(TURN_ON_TEXTS + TURN_OFF_TEXTS + FOCUS_ON_TEXTS + FOCUS_OFF_TEXTS)
            val hasListText = containsAnyText(FOCUS_SCREEN_TEXTS + SHOW_ALL_APPS_ACTION_TEXTS + APP_LIST_TEXTS)
            val hasSelectableFocusList = containsAnyText(FOCUS_SCREEN_TEXTS) && hasActionText
            return (hasFocusModeText && (hasActionText || hasListText)) || hasSelectableFocusList
        }

        fun DigitalWellbeingAutomationNode.hasNoSelectedFocusModeApps(): Boolean {
            if (containsAnyText(NO_SELECTED_APPS_TEXTS)) return true

            val checkableNodes = allNodes().filter { it.isCheckable }
            if (checkableNodes.isNotEmpty()) {
                return checkableNodes.none { it.isEffectivelyChecked() }
            }

            return containsAnyText(FOCUS_SCREEN_TEXTS)
        }

        fun DigitalWellbeingAutomationNode.findLabelNode(label: String): DigitalWellbeingAutomationNode? {
            val normalizedLabel = label.normalized()
            if (normalizedLabel.isBlank()) return null
            return allNodes().firstOrNull { node ->
                val text = node.visibleText()?.normalized() ?: return@firstOrNull false
                text == normalizedLabel || text.removeSuffix(" paused") == normalizedLabel
            }
        }

        fun DigitalWellbeingAutomationNode.findSelectableContainer(): DigitalWellbeingAutomationNode? {
            var current: DigitalWellbeingAutomationNode? = this
            repeat(PARENT_SEARCH_DEPTH) {
                current?.findCheckableDescendant()?.let { return it }
                if (current?.isCheckable == true) return current
                current = current?.parent
            }
            return findCheckableDescendant()
        }

        fun DigitalWellbeingAutomationNode.findCheckableDescendant(): DigitalWellbeingAutomationNode? {
            if (isCheckable) return this
            for (child in children) {
                val match = child.findCheckableDescendant()
                if (match != null) return match
            }
            return null
        }

        fun DigitalWellbeingAutomationNode.isEffectivelyChecked(): Boolean {
            if (isChecked) return true
            val text = sequenceOf(stateDescription, visibleText())
                .mapNotNull { it?.normalized() }
                .firstOrNull { it.isNotBlank() }
                ?: return false
            if (UNCHECKED_STATE_TEXTS.any { text.contains(it) }) return false
            return CHECKED_STATE_TEXTS.any { text.contains(it) }
        }

        fun DigitalWellbeingAutomationNode.clickSelectable(): Boolean {
            if (isEnabled && click()) return true
            val clickTarget = findClickableSelfOrParent() ?: return false
            return clickTarget.isEnabled && clickTarget.click()
        }

        fun DigitalWellbeingAutomationNode.findScrollableNode(preferredTexts: List<String>): DigitalWellbeingAutomationNode? {
            return allNodes()
                .filter { it.isScrollable }
                .maxWithOrNull(
                    compareBy<DigitalWellbeingAutomationNode> { it.scrollScore(preferredTexts) }
                        .thenBy { it.boundsArea },
                )
        }

        fun DigitalWellbeingAutomationNode.scrollScore(preferredTexts: List<String>): Int {
            var score = 0
            if (containsAnyText(preferredTexts)) score += 100
            if (containsAnyText(FOCUS_SCREEN_TEXTS + APP_LIST_TEXTS)) score += 40
            if (boundsArea > MIN_SCROLLABLE_AREA) score += 10
            return score
        }

        fun DigitalWellbeingAutomationNode.findClickableSelfOrParent(): DigitalWellbeingAutomationNode? {
            var current: DigitalWellbeingAutomationNode? = this
            repeat(PARENT_SEARCH_DEPTH) {
                val node = current ?: return null
                if (node.isClickable) return node
                current = node.parent
            }
            return null
        }

        fun DigitalWellbeingAutomationNode.allNodes(): List<DigitalWellbeingAutomationNode> {
            val result = mutableListOf<DigitalWellbeingAutomationNode>()
            fun visit(node: DigitalWellbeingAutomationNode?) {
                if (node == null) return
                result += node
                node.children.forEach(::visit)
            }
            visit(this)
            return result
        }

        private fun DigitalWellbeingAutomationNode.missingCheckedTargets(
            targets: List<DigitalWellbeingAutomationTarget>,
        ): List<String> {
            val visibleTargets = targets.mapNotNull { target ->
                val label = findLabelNode(target.label) ?: return@mapNotNull null
                target to label
            }
            if (visibleTargets.size != targets.size) return emptyList()
            return visibleTargets
                .filterNot { (_, label) -> label.findSelectableContainer()?.isEffectivelyChecked() == true }
                .map { (target, _) -> target.label }
        }

        private fun DigitalWellbeingAutomationRequest.duplicateTargetLabel(): DigitalWellbeingAutomationTarget? {
            val seen = mutableSetOf<String>()
            return targets.firstOrNull { target ->
                val normalizedLabel = target.label.normalized()
                normalizedLabel.isNotBlank() && !seen.add(normalizedLabel)
            }
        }
    }
}

internal sealed class MatchQuality : Comparable<MatchQuality> {
    abstract val rank: Int
    abstract val textLength: Int

    data object Exact : MatchQuality() {
        override val rank: Int = 0
        override val textLength: Int = 0
    }

    data class Contains(override val textLength: Int) : MatchQuality() {
        override val rank: Int = 1
    }

    override fun compareTo(other: MatchQuality): Int {
        return compareValuesBy(this, other, MatchQuality::rank, MatchQuality::textLength)
    }
}

internal data class TextAction(
    val clickTarget: DigitalWellbeingAutomationNode?,
    val matchQuality: MatchQuality,
) {
    val isEnabled: Boolean
        get() = clickTarget?.isEnabled == true
    val matchLength: Int
        get() = matchQuality.textLength

    fun click(): Boolean {
        return isEnabled && clickTarget?.click() == true
    }
}
