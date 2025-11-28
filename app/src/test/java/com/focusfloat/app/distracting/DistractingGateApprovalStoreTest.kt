package com.focusfloat.app.distracting

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistractingGateApprovalStoreTest {
    private var now = 0L
    private val approvals = DistractingGateApprovalStore { now }

    @Test
    fun approveRedeemsAfterSlowColdStart() {
        approvals.approve("com.discord")
        now = 20_000L

        assertTrue(approvals.redeem("com.discord"))
    }

    @Test
    fun approveIsConsumedOnFirstEntry() {
        approvals.approve("com.discord")

        assertTrue(approvals.redeem("com.discord"))
        assertFalse(approvals.redeem("com.discord"))
    }

    @Test
    fun approveExpiresWithoutEntry() {
        approvals.approve("com.discord")
        now = 60_001L

        assertFalse(approvals.redeem("com.discord"))
    }
}
