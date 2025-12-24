package com.focusfloat.app.core.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FreshSystemDefaultZoneClockTest {
    @Test
    fun zoneIsResolvedOnEveryCall() {
        val instant = Instant.parse("2026-06-09T17:15:00Z")
        var zone = ZoneId.of("UTC")
        val clock = FreshSystemDefaultZoneClock { Clock.fixed(instant, zone) }

        assertEquals(ZoneId.of("UTC"), clock.zone)

        zone = ZoneId.of("Asia/Yekaterinburg")

        assertEquals(ZoneId.of("Asia/Yekaterinburg"), clock.zone)
        assertEquals(instant, clock.instant())
    }
}
