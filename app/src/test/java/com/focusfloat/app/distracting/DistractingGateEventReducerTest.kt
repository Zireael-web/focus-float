package com.focusfloat.app.distracting

import com.focusfloat.app.core.model.AppRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistractingGateEventReducerTest {
    private val discord = AppRef("com.discord", userSerial = 10L)
    private val telegram = AppRef("org.telegram.messenger", userSerial = 10L)
    private val labels = mapOf(
        discord to "Discord",
        telegram to "Telegram",
    )
    private val distractingRefs = setOf(discord, telegram)
    private val ignoredPackages = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.focusfloat.app",
    )

    @Test
    fun keyboardWindowDoesNotCancelReminderOrTriggerGateAgain() {
        val reducer = DistractingGateEventReducer(FakeApprovals())
        reducer.reduce(event("com.discord"))

        assertTrue(reducer.reduce(event("com.google.android.inputmethod.latin")).isEmpty())
        val actions = reducer.reduce(event("com.discord"))

        assertNoAction<DistractingGateAction.CancelReminder>(actions)
        assertNoAction<DistractingGateAction.ScheduleReminder>(actions)
        assertNoAction<DistractingGateAction.ShowGate>(actions)
    }

    @Test
    fun systemUiWindowDoesNotCancelReminderOrTriggerGateAgain() {
        val reducer = DistractingGateEventReducer(FakeApprovals())
        reducer.reduce(event("com.discord"))

        assertTrue(reducer.reduce(event("com.android.systemui")).isEmpty())
        val actions = reducer.reduce(event("com.discord"))

        assertNoAction<DistractingGateAction.CancelReminder>(actions)
        assertNoAction<DistractingGateAction.ScheduleReminder>(actions)
        assertNoAction<DistractingGateAction.ShowGate>(actions)
    }

    @Test
    fun switchingDistractingAppsCancelsPreviousReminderAndShowsNewGate() {
        val reducer = DistractingGateEventReducer(FakeApprovals())
        reducer.reduce(event("com.discord"))

        val actions = reducer.reduce(event("org.telegram.messenger"))

        assertEquals(
            listOf("com.discord"),
            actions.filterIsInstance<DistractingGateAction.CancelReminder>().map { it.packageName },
        )
        assertEquals(
            listOf(telegram),
            actions.filterIsInstance<DistractingGateAction.ShowGate>().map { it.ref },
        )
    }

    @Test
    fun approvedGateActivityReturnSchedulesReminderOnce() {
        val approvals = FakeApprovals()
        val reducer = DistractingGateEventReducer(approvals)
        reducer.reduce(event("com.discord"))

        approvals.approve("com.discord")
        assertTrue(reducer.reduce(event("com.focusfloat.app")).isEmpty())
        val firstReturn = reducer.reduce(event("com.discord"))
        val secondReturn = reducer.reduce(event("com.discord"))

        assertEquals(
            listOf(discord),
            firstReturn.filterIsInstance<DistractingGateAction.ScheduleReminder>().map { it.ref },
        )
        assertNoAction<DistractingGateAction.ScheduleReminder>(secondReturn)
        assertNoAction<DistractingGateAction.ShowGate>(firstReturn)
        assertNoAction<DistractingGateAction.ShowGate>(secondReturn)
    }

    private fun event(packageName: String): DistractingGateWindowEvent {
        return DistractingGateWindowEvent(
            type = DistractingGateWindowEventType.WindowStateChanged,
            packageName = packageName,
            distractingRefs = distractingRefs,
            labelsByRef = labels,
            ignoredPackages = ignoredPackages,
        )
    }

    private inline fun <reified T : DistractingGateAction> assertNoAction(actions: List<DistractingGateAction>) {
        assertTrue(actions.filterIsInstance<T>().isEmpty())
    }

    private class FakeApprovals : DistractingGateApprovalRegistry {
        private val approvedPackages = mutableSetOf<String>()

        fun approve(packageName: String) {
            approvedPackages += packageName
        }

        override fun redeem(packageName: String): Boolean {
            return approvedPackages.remove(packageName)
        }
    }
}
