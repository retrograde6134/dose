package ca.alexlittle.dose.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * System clock that reads the device's time zone on every call.
 *
 * [Clock.systemDefaultZone] captures the zone once, so a long-lived injected instance would keep
 * resolving wall-clock schedule times in the old zone after the user travels or changes the zone.
 */
internal object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
