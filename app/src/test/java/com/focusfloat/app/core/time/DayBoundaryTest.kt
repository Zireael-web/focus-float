package com.focusfloat.app.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayBoundaryTest {
    @Test
    fun endOfTodayReturnsNextLocalMidnight() {
        val zone = ZoneId.of("Europe/Amsterdam")
        val clock = Clock.fixed(Instant.parse("2026-06-09T17:15:00Z"), zone)

        val result = endOfToday(clock)

        assertEquals(
            LocalDate.of(2026, 6, 10).atStartOfDay(zone).toInstant(),
            result,
        )
    }

    @Test
    fun endOfTodayHandlesDstBoundary() {
        val zone = ZoneId.of("Europe/Amsterdam")
        val clock = Clock.fixed(Instant.parse("2026-03-29T10:30:00Z"), zone)

        val result = endOfToday(clock)

        assertEquals(
            LocalDate.of(2026, 3, 30).atStartOfDay(zone).toInstant(),
            result,
        )
    }
}
