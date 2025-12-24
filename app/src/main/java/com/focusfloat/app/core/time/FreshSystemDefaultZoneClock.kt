package com.focusfloat.app.core.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class FreshSystemDefaultZoneClock(
    private val delegate: () -> Clock = { Clock.systemDefaultZone() },
) : Clock() {
    override fun getZone(): ZoneId = delegate().zone

    override fun withZone(zone: ZoneId): Clock = delegate().withZone(zone)

    override fun instant(): Instant = delegate().instant()

    override fun millis(): Long = delegate().millis()
}
