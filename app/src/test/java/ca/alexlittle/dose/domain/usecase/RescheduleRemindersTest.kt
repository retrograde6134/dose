package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseLog
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.schedule.DueDose
import ca.alexlittle.dose.testing.ReminderHarness
import ca.alexlittle.dose.testing.medication
import ca.alexlittle.dose.testing.schedule
import ca.alexlittle.dose.testing.scheduleDraft
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Times are Toronto: EDT (UTC-4) until 2026-11-01, EST (UTC-5) after. */
class RescheduleRemindersTest {
    // 07:00 local.
    private val harness = ReminderHarness(now = at("2026-10-16T11:00:00Z"))
    private val reschedule = harness.reschedule

    private suspend fun dailyAt(vararg times: LocalTime, medicationName: String = "Metformin"): Pair<Long, Long> {
        val medicationId = harness.medications.insert(medication(name = medicationName))
        val scheduleId = harness.schedules.insert(schedule(medicationId = medicationId, timesOfDay = times.toList()))
        return medicationId to scheduleId
    }

    @Test
    fun `arms the next dose`() = runTest {
        dailyAt(LocalTime.of(8, 0), LocalTime.of(20, 0))

        reschedule()

        assertEquals(at("2026-10-16T12:00:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `cancels the alarm when nothing needs a reminder`() = runTest {
        val (_, scheduleId) = dailyAt(LocalTime.of(8, 0))
        reschedule()

        harness.schedules.delete(scheduleId)
        reschedule()

        assertNull(harness.alarms.armedAt)
    }

    @Test
    fun `the alarm shows the due dose and arms the next`() = runTest {
        val (_, scheduleId) = dailyAt(LocalTime.of(8, 0), LocalTime.of(20, 0))
        harness.clock.now = at("2026-10-16T12:00:02Z")

        reschedule.onAlarm(at("2026-10-16T12:00:00Z"))

        assertEquals(listOf(DoseKey(scheduleId, at("2026-10-16T12:00:00Z"))), harness.notifier.shown.map { it.key })
        assertEquals(at("2026-10-17T00:00:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `a late alarm also shows doses that came due while it waited`() = runTest {
        val (_, eight) = dailyAt(LocalTime.of(8, 0))
        val (_, quarterPast) = dailyAt(LocalTime.of(8, 15), medicationName = "Aspirin")
        harness.clock.now = at("2026-10-16T12:20:00Z")

        reschedule.onAlarm(at("2026-10-16T12:00:00Z"))

        assertEquals(
            listOf(DoseKey(eight, at("2026-10-16T12:00:00Z")), DoseKey(quarterPast, at("2026-10-16T12:15:00Z"))),
            harness.notifier.shown.map { it.key },
        )
    }

    @Test
    fun `an alarm armed for a time now in the future shows nothing and re-arms`() = runTest {
        // The user set the clock back after the alarm was armed.
        dailyAt(LocalTime.of(8, 0))

        reschedule.onAlarm(at("2026-10-16T12:00:00Z"))

        assertEquals(emptyList<DueDose>(), harness.notifier.shown)
        assertEquals(at("2026-10-16T12:00:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `after a reboot, doses due while the phone was off are shown once`() = runTest {
        val (_, scheduleId) = dailyAt(LocalTime.of(8, 0))
        val (_, takenId) = dailyAt(LocalTime.of(7, 45), medicationName = "Taken already")
        log(takenId, at("2026-10-16T11:45:00Z"), DoseStatus.TAKEN)
        reschedule()

        // Off from 07:50 to 08:20; alarms do not survive a reboot.
        harness.clock.now = at("2026-10-16T12:20:00Z")
        reschedule(catchUp = true)
        reschedule(catchUp = true)

        assertEquals(listOf(DoseKey(scheduleId, at("2026-10-16T12:00:00Z"))), harness.notifier.shown.map { it.key })
        assertEquals(at("2026-10-17T11:45:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `catch-up ignores doses older than the window`() = runTest {
        dailyAt(LocalTime.of(8, 0))

        harness.clock.now = at("2026-10-16T12:00:00Z").plus(RescheduleReminders.CATCH_UP_WINDOW).plusSeconds(1)
        reschedule(catchUp = true)

        assertEquals(emptyList<DueDose>(), harness.notifier.shown)
    }

    @Test
    fun `a snooze survives a reboot`() = runTest {
        val (_, scheduleId) = dailyAt(LocalTime.of(8, 0))
        val key = DoseKey(scheduleId, at("2026-10-16T12:00:00Z"))
        log(scheduleId, key.scheduledAt, DoseStatus.SNOOZED_PENDING, snoozedUntil = at("2026-10-16T12:10:00Z"))

        harness.clock.now = at("2026-10-16T12:05:00Z")
        reschedule(catchUp = true)
        assertEquals(at("2026-10-16T12:10:00Z"), harness.alarms.armedAt)

        harness.clock.now = at("2026-10-16T12:10:01Z")
        reschedule.onAlarm(at("2026-10-16T12:10:00Z"))
        assertEquals(listOf(key), harness.notifier.shown.map { it.key })
    }

    @Test
    fun `a snooze that ended while the phone was off is shown on boot`() = runTest {
        val (_, scheduleId) = dailyAt(LocalTime.of(8, 0))
        val key = DoseKey(scheduleId, at("2026-10-16T12:00:00Z"))
        log(scheduleId, key.scheduledAt, DoseStatus.SNOOZED_PENDING, snoozedUntil = at("2026-10-16T12:10:00Z"))

        harness.clock.now = at("2026-10-16T12:30:00Z")
        reschedule(catchUp = true)

        assertEquals(listOf(key), harness.notifier.shown.map { it.key })
    }

    @Test
    fun `a time zone change re-arms at the same local time in the new zone`() = runTest {
        dailyAt(LocalTime.of(8, 0))
        reschedule()

        harness.clock.moveTo(ZoneId.of("America/Vancouver"))
        reschedule(catchUp = true)

        // 08:00 PDT, not the 05:00 PDT that 08:00 Toronto would be.
        assertEquals(at("2026-10-16T15:00:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `a DST change keeps doses at the same wall-clock time`() = runTest {
        dailyAt(LocalTime.of(8, 0))

        // 09:00 EDT on the day before fall back.
        harness.clock.now = at("2026-10-31T13:00:00Z")
        reschedule()

        // 08:00 EST the next morning.
        assertEquals(at("2026-11-01T13:00:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `editing a schedule moves the alarm`() = runTest {
        val (medicationId, scheduleId) = dailyAt(LocalTime.of(8, 0))
        val save = SaveSchedule(harness.schedules, harness.medications, reschedule)

        save(scheduleDraft(medicationId, id = scheduleId, timesOfDay = listOf(LocalTime.of(9, 30))))

        assertEquals(at("2026-10-16T13:30:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `adding a schedule arms its first dose`() = runTest {
        val medicationId = harness.medications.insert(medication())
        val save = SaveSchedule(harness.schedules, harness.medications, reschedule)

        save(scheduleDraft(medicationId, timesOfDay = listOf(LocalTime.of(7, 30))))

        assertEquals(at("2026-10-16T11:30:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `deleting a schedule cancels its alarm and its notification`() = runTest {
        val (medicationId, scheduleId) = dailyAt(LocalTime.of(8, 0))
        val key = showing(scheduleId, medicationId)

        DeleteSchedule(harness.schedules, harness.notifier, reschedule)(scheduleId)

        assertNull(harness.alarms.armedAt)
        assertFalse(harness.notifier.isShowing(key))
    }

    @Test
    fun `archiving a medication stops its reminders and unarchiving brings them back`() = runTest {
        val (medicationId, scheduleId) = dailyAt(LocalTime.of(8, 0))
        val key = showing(scheduleId, medicationId)
        val setArchived = SetMedicationArchived(harness.medications, harness.notifier, reschedule)

        setArchived(medicationId, archived = true)
        assertNull(harness.alarms.armedAt)
        assertFalse(harness.notifier.isShowing(key))

        setArchived(medicationId, archived = false)
        assertEquals(at("2026-10-16T12:00:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `an archived medication is not caught up after a reboot`() = runTest {
        val (medicationId, _) = dailyAt(LocalTime.of(8, 0))
        harness.medications.setArchived(medicationId, archived = true)

        harness.clock.now = at("2026-10-16T12:20:00Z")
        reschedule(catchUp = true)

        assertEquals(emptyList<DueDose>(), harness.notifier.shown)
    }

    @Test
    fun `deleting a medication stops its reminders`() = runTest {
        val (medicationId, scheduleId) = dailyAt(LocalTime.of(8, 0))
        val key = showing(scheduleId, medicationId)

        DeleteMedication(harness.medications, harness.notifier, reschedule)(medicationId)

        assertNull(harness.alarms.armedAt)
        assertFalse(harness.notifier.isShowing(key))
    }

    private fun showing(scheduleId: Long, medicationId: Long): DoseKey {
        val key = DoseKey(scheduleId, at("2026-10-15T12:00:00Z"))
        harness.notifier.show(DueDose(key, medicationId, BigDecimal.ONE, key.scheduledAt), medication())
        return key
    }

    private suspend fun log(scheduleId: Long, scheduledAt: Instant, status: DoseStatus, snoozedUntil: Instant? = null) {
        val schedule = checkNotNull(harness.schedules.get(scheduleId))
        harness.logs.modify(DoseKey(scheduleId, scheduledAt)) {
            DoseLog(
                medicationId = schedule.medicationId,
                scheduleId = scheduleId,
                scheduledAt = scheduledAt,
                takenAt = null,
                status = status,
                quantity = BigDecimal.ONE,
                note = null,
                snoozedUntil = snoozedUntil,
            )
        }
    }
}

private fun at(text: String): Instant = Instant.parse(text)
