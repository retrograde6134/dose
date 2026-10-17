package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.schedule.DueDose
import ca.alexlittle.dose.testing.ReminderHarness
import ca.alexlittle.dose.testing.medication
import ca.alexlittle.dose.testing.schedule
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DoseActionsTest {
    // 08:00 Toronto on 2026-10-16, the moment the 08:00 dose is due.
    private val harness = ReminderHarness(now = Instant.parse("2026-10-16T12:00:00Z"))
    private val writer =
        DoseLogWriter(harness.logs, harness.schedules, harness.notifier, harness.reschedule, harness.clock)
    private val take = TakeDose(writer)
    private val skip = SkipDose(writer)
    private val snooze = SnoozeDose(writer)

    private suspend fun dueDose(quantity: String = "1"): DoseKey {
        val medicationId = harness.medications.insert(medication())
        val scheduleId = harness.schedules.insert(schedule(medicationId = medicationId, doseQuantity = quantity))
        val key = DoseKey(scheduleId, harness.clock.now)
        harness.notifier.show(DueDose(key, medicationId, BigDecimal(quantity), key.scheduledAt), medication())
        return key
    }

    @Test
    fun `taking logs the dose and clears its notification`() = runTest {
        val key = dueDose(quantity = "0.5")

        take(key)

        val log = harness.logs.all.single()
        assertEquals(DoseStatus.TAKEN, log.status)
        assertEquals(harness.clock.now, log.takenAt)
        assertEquals(BigDecimal("0.5"), log.quantity)
        assertEquals(key, log.key)
        assertFalse(harness.notifier.isShowing(key))
    }

    @Test
    fun `taking twice logs once and keeps the first time`() = runTest {
        val key = dueDose()
        take(key)
        val first = harness.clock.now

        harness.clock.now = first.plusSeconds(90)
        take(key)

        assertEquals(first, harness.logs.all.single().takenAt)
    }

    @Test
    fun `the latest of take and skip wins`() = runTest {
        val key = dueDose()

        skip(key)
        take(key)
        assertEquals(DoseStatus.TAKEN, harness.logs.all.single().status)

        skip(key)
        val log = harness.logs.all.single()
        assertEquals(DoseStatus.SKIPPED, log.status)
        assertNull(log.takenAt)
    }

    @Test
    fun `snoozing reminds again after the default and clears the notification`() = runTest {
        val key = dueDose()

        snooze(key)

        val log = harness.logs.all.single()
        val until = harness.clock.now.plus(SnoozeDose.SNOOZE_DEFAULT)
        assertEquals(DoseStatus.SNOOZED_PENDING, log.status)
        assertEquals(until, log.snoozedUntil)
        assertEquals(until, harness.alarms.armedAt)
        assertFalse(harness.notifier.isShowing(key))
    }

    @Test
    fun `snoozing again pushes the reminder later`() = runTest {
        val key = dueDose()
        snooze(key)

        harness.clock.now = harness.clock.now.plus(SnoozeDose.SNOOZE_DEFAULT)
        snooze(key)

        assertEquals(harness.clock.now.plus(SnoozeDose.SNOOZE_DEFAULT), harness.logs.all.single().snoozedUntil)
    }

    @Test
    fun `a stale snooze never undoes a taken or skipped dose`() = runTest {
        val taken = dueDose()
        take(taken)
        snooze(taken)
        assertEquals(DoseStatus.TAKEN, harness.logs.get(taken)?.status)

        val skipped = dueDose()
        skip(skipped)
        snooze(skipped)
        assertEquals(DoseStatus.SKIPPED, harness.logs.get(skipped)?.status)
    }

    @Test
    fun `taking a snoozed dose ends the snooze`() = runTest {
        val key = dueDose()
        snooze(key)

        take(key)

        val log = harness.logs.all.single()
        assertEquals(DoseStatus.TAKEN, log.status)
        assertNull(log.snoozedUntil)
        // Tomorrow's 08:00, not the snooze.
        assertEquals(Instant.parse("2026-10-17T12:00:00Z"), harness.alarms.armedAt)
    }

    @Test
    fun `an action for a deleted schedule only clears the notification`() = runTest {
        val key = dueDose()
        harness.schedules.delete(key.scheduleId)

        take(key)

        assertEquals(emptyList<Any>(), harness.logs.all)
        assertFalse(harness.notifier.isShowing(key))
    }
}
