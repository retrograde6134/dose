package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.testing.ReminderHarness
import ca.alexlittle.dose.testing.START_DATE
import ca.alexlittle.dose.testing.medication
import ca.alexlittle.dose.testing.scheduleDraft
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveScheduleTest {
    private val harness = ReminderHarness()
    private val medications = harness.medications
    private val schedules = harness.schedules
    private val saveSchedule = SaveSchedule(schedules, medications, harness.reschedule)

    @Test
    fun `rejects invalid drafts with every applicable error`() = runTest {
        val medicationId = medications.insert(medication())
        data class Case(val draft: ScheduleDraft, val expected: Set<ScheduleError>)

        fun draft(recurrence: Recurrence = Recurrence.Daily) = scheduleDraft(medicationId, recurrence = recurrence)

        val cases = listOf(
            Case(draft().copy(timesOfDay = emptyList()), setOf(ScheduleError.NO_TIMES)),
            Case(draft().copy(doseQuantity = BigDecimal.ZERO), setOf(ScheduleError.QUANTITY_NOT_POSITIVE)),
            Case(draft().copy(doseQuantity = BigDecimal("-0.5")), setOf(ScheduleError.QUANTITY_NOT_POSITIVE)),
            Case(draft().copy(endDate = START_DATE.minusDays(1)), setOf(ScheduleError.END_BEFORE_START)),
            Case(draft(Recurrence.Weekly(emptySet())), setOf(ScheduleError.NO_WEEKDAYS)),
            Case(
                draft(Recurrence.Weekly(setOf(DayOfWeek.MONDAY), everyNWeeks = 0)),
                setOf(ScheduleError.INTERVAL_NOT_POSITIVE),
            ),
            Case(draft(Recurrence.EveryNDays(0)), setOf(ScheduleError.INTERVAL_NOT_POSITIVE)),
            Case(draft(Recurrence.Cyclic(onDays = 0, offDays = 7)), setOf(ScheduleError.CYCLE_NOT_POSITIVE)),
            Case(draft(Recurrence.Cyclic(onDays = 21, offDays = 0)), setOf(ScheduleError.CYCLE_NOT_POSITIVE)),
            Case(draft(Recurrence.AsNeeded(-1)), setOf(ScheduleError.MIN_INTERVAL_NEGATIVE)),
            Case(
                draft(Recurrence.Weekly(emptySet())).copy(timesOfDay = emptyList(), doseQuantity = BigDecimal.ZERO),
                setOf(ScheduleError.NO_WEEKDAYS, ScheduleError.NO_TIMES, ScheduleError.QUANTITY_NOT_POSITIVE),
            ),
        )

        for (case in cases) {
            assertEquals(case.toString(), SaveScheduleResult.Invalid(case.expected), saveSchedule(case.draft))
        }
        assertTrue(schedules.all.isEmpty())
    }

    @Test
    fun `accepts an end date equal to the start date`() = runTest {
        val medicationId = medications.insert(medication())

        val result = saveSchedule(scheduleDraft(medicationId, endDate = START_DATE))

        assertTrue(result is SaveScheduleResult.Saved)
    }

    @Test
    fun `inserts with times sorted, de-duplicated and truncated to the minute`() = runTest {
        val medicationId = medications.insert(medication())
        val times = listOf(LocalTime.of(20, 0), LocalTime.of(8, 0, 30), LocalTime.of(8, 0), LocalTime.of(12, 15))

        val result = saveSchedule(scheduleDraft(medicationId, timesOfDay = times))

        val saved = schedules.all.single()
        assertEquals(SaveScheduleResult.Saved(saved.id), result)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(12, 15), LocalTime.of(20, 0)), saved.timesOfDay)
    }

    @Test
    fun `normalizes quantity without changing its value`() = runTest {
        val medicationId = medications.insert(medication())

        saveSchedule(scheduleDraft(medicationId, doseQuantity = "0.50"))
        saveSchedule(scheduleDraft(medicationId, doseQuantity = "20"))

        assertEquals(listOf(BigDecimal("0.5"), BigDecimal("20")), schedules.all.map { it.doseQuantity })
    }

    @Test
    fun `as-needed drops times and treats a zero minimum interval as none`() = runTest {
        val medicationId = medications.insert(medication())

        saveSchedule(scheduleDraft(medicationId, recurrence = Recurrence.AsNeeded(0)))
        saveSchedule(
            scheduleDraft(medicationId, recurrence = Recurrence.AsNeeded(null), timesOfDay = emptyList()),
        )
        saveSchedule(scheduleDraft(medicationId, recurrence = Recurrence.AsNeeded(240)))

        assertEquals(
            listOf(Recurrence.AsNeeded(null), Recurrence.AsNeeded(null), Recurrence.AsNeeded(240)),
            schedules.all.map { it.recurrence },
        )
        assertTrue(schedules.all.all { it.timesOfDay.isEmpty() })
    }

    @Test
    fun `update replaces fields but keeps the original medication`() = runTest {
        val medicationId = medications.insert(medication())
        val otherId = medications.insert(medication(name = "Aspirin"))
        val id = (saveSchedule(scheduleDraft(medicationId)) as SaveScheduleResult.Saved).id

        val result = saveSchedule(
            scheduleDraft(otherId, id = id, recurrence = Recurrence.EveryNDays(2), doseQuantity = "2"),
        )

        assertEquals(SaveScheduleResult.Saved(id), result)
        val saved = schedules.all.single()
        assertEquals(medicationId, saved.medicationId)
        assertEquals(Recurrence.EveryNDays(2), saved.recurrence)
        assertEquals(BigDecimal("2"), saved.doseQuantity)
    }

    @Test
    fun `reports NotFound when the schedule or its medication is gone`() = runTest {
        val medicationId = medications.insert(medication())

        assertEquals(SaveScheduleResult.NotFound, saveSchedule(scheduleDraft(medicationId, id = 42)))
        assertEquals(SaveScheduleResult.NotFound, saveSchedule(scheduleDraft(medicationId = 99)))
        assertTrue(schedules.all.isEmpty())
    }
}
