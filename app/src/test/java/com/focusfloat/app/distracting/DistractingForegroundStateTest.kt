package com.focusfloat.app.distracting

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistractingForegroundStateTest {
    @Test
    fun unknownForegroundDoesNotShowReminder() {
        DistractingForegroundState.update(null)

        assertFalse(DistractingForegroundState.shouldShowReminder("com.discord"))
    }

    @Test
    fun matchingForegroundShowsReminder() {
        DistractingForegroundState.update("com.discord")

        assertTrue(DistractingForegroundState.shouldShowReminder("com.discord"))
    }

    @Test
    fun differentForegroundDoesNotShowReminder() {
        DistractingForegroundState.update("org.telegram.messenger")

        assertFalse(DistractingForegroundState.shouldShowReminder("com.discord"))
    }
}
