package com.focusfloat.app.digitalwellbeing

import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationEngine.Companion.diagnosticSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitalWellbeingAutomationEngineTest {
    @Test
    fun persistentShowAllAppsDoesNotLoopForever() {
        val engine = DigitalWellbeingAutomationEngine()
        val checkbox = node(checkable = true, checked = false, clickAction = { checked = true })
        val root = focusRoot(
            node(text = "Show all apps", clickable = true),
            row("Calendar", checkbox),
            node(text = "Turn on now", clickable = true),
        )
        val callbacks = RecordingCallbacks(root)
        val request = enableRequest(target("pkg.calendar", "Calendar"))

        repeat(5) {
            engine.enableFocusMode(request, root, callbacks)
        }

        assertEquals(2, root.findTextNode("Show all apps")?.clicks)
        assertEquals(1, checkbox.clicks)
        assertEquals("Pixel Focus Mode started", callbacks.completedMessage)
    }

    @Test
    fun selectedButUncheckedNodeIsNotTreatedAsChecked() {
        val engine = DigitalWellbeingAutomationEngine()
        val checkbox = node(checkable = true, checked = false, selected = true)
        val root = focusRoot(row("Telegram", checkbox))
        val callbacks = RecordingCallbacks(root)

        engine.enableFocusMode(enableRequest(target("org.telegram", "Telegram")), root, callbacks)

        assertEquals(1, checkbox.clicks)
        assertFalse(callbacks.completed)
    }

    @Test
    fun finalVerificationFailsWhenVisibleTargetIsNoLongerChecked() {
        val engine = DigitalWellbeingAutomationEngine()
        val checkbox = node(checkable = true, checked = true)
        val root = focusRoot(row("YouTube", checkbox), node(text = "Turn on now", clickable = true))
        val callbacks = RecordingCallbacks(root)
        val request = enableRequest(target("com.google.android.youtube", "YouTube"))

        engine.enableFocusMode(request, root, callbacks)
        checkbox.checked = false
        engine.enableFocusMode(request, root, callbacks)

        assertTrue(callbacks.failedMessage.orEmpty().contains("did not verify selected apps: YouTube"))
    }

    @Test
    fun labelWithPausedSuffixMatchesTarget() {
        val engine = DigitalWellbeingAutomationEngine()
        val checkbox = node(checkable = true, checked = false)
        val root = focusRoot(row("Gmail paused", checkbox))
        val callbacks = RecordingCallbacks(root)

        engine.enableFocusMode(enableRequest(target("com.google.android.gm", "Gmail")), root, callbacks)

        assertEquals(1, checkbox.clicks)
    }

    @Test
    fun turnOffDoesNotClickScheduleRow() {
        val engine = DigitalWellbeingAutomationEngine()
        val schedule = node(text = "Turn off Focus mode automatically", clickable = true)
        val root = focusRoot(
            node(text = "Focus mode is on"),
            schedule,
        )
        val callbacks = RecordingCallbacks(root)

        engine.disableFocusMode(disableRequest(), root, callbacks)

        assertEquals(0, schedule.clicks)
        assertTrue(callbacks.failedMessage.orEmpty().isNotBlank())
    }

    @Test
    fun missingAppFailsWithDiagnosticContextFromRoot() {
        val engine = DigitalWellbeingAutomationEngine()
        val root = focusRoot(
            node(text = "Focus"),
            node(text = "Select distracting apps"),
            node(text = "Visible app"),
        )
        val callbacks = RecordingCallbacks(root)

        engine.enableFocusMode(enableRequest(target("missing.pkg", "Missing")), root, callbacks)

        val message = callbacks.failedMessage.orEmpty()
        assertTrue(message.contains("Could not find 1 apps"))
        assertTrue(message.contains("seen: package=com.google.android.apps.wellbeing"))
        assertTrue(message.contains("Select distracting apps"))
    }

    @Test
    fun accessibilityEntryMatcherHandlesFullShortGarbageAndEmptyEntries() {
        val expectedPackage = "com.focusfloat.app"
        val expectedClass = "com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationAccessibilityService"

        assertTrue(
            accessibilityServiceEntryMatches(
                "com.focusfloat.app/com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationAccessibilityService",
                expectedPackage,
                expectedClass,
            ),
        )
        assertTrue(
            accessibilityServiceEntryMatches(
                "com.focusfloat.app/.digitalwellbeing.DigitalWellbeingAutomationAccessibilityService",
                expectedPackage,
                expectedClass,
            ),
        )
        assertFalse(accessibilityServiceEntryMatches("not a component", expectedPackage, expectedClass))
        assertFalse(accessibilityServiceEntryMatches("", expectedPackage, expectedClass))
    }

    private fun focusRoot(vararg children: FakeNode): FakeNode {
        return node(
            packageName = DigitalWellbeingLauncher.WELLBEING_PACKAGE,
            children = listOf(
                node(text = "Focus"),
                node(text = "Select distracting apps"),
                *children,
            ),
        )
    }

    private fun row(label: String, checkbox: FakeNode): FakeNode {
        return node(children = listOf(node(text = label), checkbox))
    }

    private fun enableRequest(vararg targets: DigitalWellbeingAutomationTarget): DigitalWellbeingAutomationRequest {
        return DigitalWellbeingAutomationRequest(
            id = "request",
            mode = DigitalWellbeingAutomationMode.EnableFocusMode,
            targets = targets.toList(),
            createdAtEpochMs = 0L,
        )
    }

    private fun disableRequest(): DigitalWellbeingAutomationRequest {
        return DigitalWellbeingAutomationRequest(
            id = "request",
            mode = DigitalWellbeingAutomationMode.DisableFocusMode,
            targets = emptyList(),
            createdAtEpochMs = 0L,
        )
    }

    private fun target(packageName: String, label: String): DigitalWellbeingAutomationTarget {
        return DigitalWellbeingAutomationTarget(packageName, label, userSerial = 0L)
    }

    private fun node(
        packageName: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        stateDescription: String? = null,
        checkable: Boolean = false,
        checked: Boolean = false,
        selected: Boolean = false,
        enabled: Boolean = true,
        clickable: Boolean = false,
        scrollable: Boolean = false,
        boundsArea: Int = 200_000,
        children: List<FakeNode> = emptyList(),
        clickAction: (FakeNode.() -> Unit)? = null,
        scrollAction: (FakeNode.() -> Unit)? = null,
    ): FakeNode {
        return FakeNode(
            packageName = packageName,
            text = text,
            contentDescription = contentDescription,
            stateDescription = stateDescription,
            isCheckable = checkable,
            checked = checked,
            isSelected = selected,
            isEnabled = enabled,
            isClickable = clickable || checkable,
            isScrollable = scrollable,
            boundsAreaValue = boundsArea,
            clickAction = clickAction,
            scrollAction = scrollAction,
        ).also { parent ->
            children.forEach(parent::addChild)
        }
    }

    private class RecordingCallbacks(
        private val root: DigitalWellbeingAutomationNode,
    ) : DigitalWellbeingAutomationEngine.Callbacks {
        var completed = false
        var completedMessage: String? = null
        var failedMessage: String? = null

        override fun updateProgress(message: String, completed: Int, total: Int) = Unit

        override fun complete(message: String) {
            completed = true
            completedMessage = message
        }

        override fun fail(message: String) {
            failedMessage = "$message\n${root.diagnosticSummary()}"
        }
    }

    private class FakeNode(
        override val packageName: String?,
        override val text: String?,
        override val contentDescription: String?,
        override val stateDescription: String?,
        override val isCheckable: Boolean,
        var checked: Boolean,
        override val isSelected: Boolean,
        override val isEnabled: Boolean,
        override val isClickable: Boolean,
        override val isScrollable: Boolean,
        private val boundsAreaValue: Int,
        private val clickAction: (FakeNode.() -> Unit)?,
        private val scrollAction: (FakeNode.() -> Unit)?,
    ) : DigitalWellbeingAutomationNode {
        private val mutableChildren = mutableListOf<FakeNode>()
        private var mutableParent: FakeNode? = null
        var clicks = 0
        var scrolls = 0

        override val isChecked: Boolean
            get() = checked
        override val parent: DigitalWellbeingAutomationNode?
            get() = mutableParent
        override val children: List<DigitalWellbeingAutomationNode>
            get() = mutableChildren
        override val boundsArea: Int
            get() = boundsAreaValue

        fun addChild(child: FakeNode) {
            child.mutableParent = this
            mutableChildren += child
        }

        override fun click(): Boolean {
            if (!isEnabled || !isClickable) return false
            clicks += 1
            clickAction?.invoke(this)
            return true
        }

        override fun scrollForward(): Boolean {
            if (!isScrollable) return false
            scrolls += 1
            scrollAction?.invoke(this)
            return true
        }

        fun findTextNode(text: String): FakeNode? {
            if (this.text == text) return this
            return mutableChildren.firstNotNullOfOrNull { it.findTextNode(text) }
        }
    }
}
