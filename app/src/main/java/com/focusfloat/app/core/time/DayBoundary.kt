package com.focusfloat.app.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun endOfToday(clock: Clock = Clock.systemDefaultZone()): Instant {
    val zone = clock.zone
    return LocalDate.now(clock).plusDays(1).atStartOfDay(zone).toInstant()
}

fun formatTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
    return DateTimeFormatter.ofPattern("HH:mm", Locale.US).format(instant.atZone(zoneId))
}

fun nowClockText(clock: Clock = Clock.systemDefaultZone()): String {
    return DateTimeFormatter.ofPattern("HH:mm", Locale.US).format(Instant.now(clock).atZone(clock.zone))
}

fun nowDateText(clock: Clock = Clock.systemDefaultZone()): String {
    return DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)
        .format(Instant.now(clock).atZone(clock.zone))
}
