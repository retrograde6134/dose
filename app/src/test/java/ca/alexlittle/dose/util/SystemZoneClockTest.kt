package ca.alexlittle.dose.util

import java.time.ZoneId
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemZoneClockTest {
    private val originalZone = TimeZone.getDefault()

    @After
    fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `zone follows device time zone changes`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Halifax"))
        assertEquals(ZoneId.of("America/Halifax"), SystemZoneClock.zone)

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        assertEquals(ZoneId.of("Asia/Tokyo"), SystemZoneClock.zone)
    }
}
