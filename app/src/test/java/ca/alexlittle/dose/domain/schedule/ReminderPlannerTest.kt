package ca.alexlittle.dose.domain.schedule

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseLog
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.testing.schedule
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPlannerTest {
    private val daily8 = schedule(timesOfDay = listOf(LocalTime.of(8, 0))).copy(id = 1)

    @Test
    fun `next wake-up is the next dose time`() {
        // 07:00 Toronto (EDT, UTC-4).
        val next = ReminderPlanner.nextWakeUp(listOf(daily8), emptyList(), at("2026-10-16T11:00:00Z"), TORONTO)

        assertEquals(at("2026-10-16T12:00:00Z"), next)
    }

    @Test
    fun `next wake-up is strictly after now`() {
        val next = ReminderPlanner.nextWakeUp(listOf(daily8), emptyList(), at("2026-10-16T12:00:00Z"), TORONTO)

        assertEquals(at("2026-10-17T12:00:00Z"), next)
    }

    @Test
    fun `logged doses of any status are skipped`() {
        for (status in DoseStatus.entries) {
            val logs = listOf(log(daily8, "2026-10-16T12:00:00Z", status, snoozedUntil = null))

            val next = ReminderPlanner.nextWakeUp(listOf(daily8), logs, at("2026-10-16T11:00:00Z"), TORONTO)

            assertEquals(status.name, at("2026-10-17T12:00:00Z"), next)
        }
    }

    @Test
    fun `a snooze ending before the next dose wins`() {
        val logs = listOf(snoozed(daily8, "2026-10-16T12:00:00Z", until = "2026-10-16T12:10:00Z"))

        val next = ReminderPlanner.nextWakeUp(listOf(daily8), logs, at("2026-10-16T12:01:00Z"), TORONTO)

        assertEquals(at("2026-10-16T12:10:00Z"), next)
    }

    @Test
    fun `a snooze of a deleted or inactive schedule is ignored`() {
        val other = schedule(timesOfDay = listOf(LocalTime.of(20, 0))).copy(id = 2)
        val logs = listOf(snoozed(daily8, "2026-10-16T12:00:00Z", until = "2026-10-16T12:10:00Z"))

        val next = ReminderPlanner.nextWakeUp(listOf(other), logs, at("2026-10-16T12:01:00Z"), TORONTO)

        assertEquals(at("2026-10-17T00:00:00Z"), next)
    }

    @Test
    fun `several medications at the same time are all due at one instant`() {
        val second = schedule(medicationId = 2, timesOfDay = listOf(LocalTime.of(8, 0))).copy(id = 2)
        val at = at("2026-10-16T12:00:00Z")

        val due = ReminderPlanner.due(listOf(second, daily8), emptyList(), at, at.plusNanos(1), TORONTO)

        assertEquals(listOf(DoseKey(1, at), DoseKey(2, at)), due.map { it.key })
        assertEquals(listOf(1L, 2L), due.map { it.medicationId })
    }

    @Test
    fun `due includes a scheduled dose and a snooze ending in the same window`() {
        val noon = schedule(timesOfDay = listOf(LocalTime.of(12, 0))).copy(id = 2)
        val logs = listOf(snoozed(daily8, "2026-10-16T12:00:00Z", until = "2026-10-16T16:00:00Z"))

        val due = ReminderPlanner.due(
            listOf(daily8, noon),
            logs,
            at("2026-10-16T15:00:00Z"),
            at("2026-10-16T17:00:00Z"),
            TORONTO,
        )

        assertEquals(
            listOf(
                DueDose(DoseKey(1, at("2026-10-16T12:00:00Z")), 1, BigDecimal.ONE, at("2026-10-16T16:00:00Z")),
                DueDose(DoseKey(2, at("2026-10-16T16:00:00Z")), 1, BigDecimal.ONE, at("2026-10-16T16:00:00Z")),
            ),
            due,
        )
    }

    @Test
    fun `due carries the schedule's fractional quantity`() {
        val half = daily8.copy(doseQuantity = BigDecimal("0.5"))
        val at = at("2026-10-16T12:00:00Z")

        val due = ReminderPlanner.due(listOf(half), emptyList(), at, at.plusNanos(1), TORONTO).single()

        assertEquals(BigDecimal("0.5"), due.quantity)
    }

    @Test
    fun `as-needed schedules never wake up`() {
        val prn = schedule(recurrence = Recurrence.AsNeeded(240), timesOfDay = emptyList()).copy(id = 1)

        assertNull(ReminderPlanner.nextWakeUp(listOf(prn), emptyList(), at("2026-10-16T11:00:00Z"), TORONTO))
    }

    @Test
    fun `nothing after the end date means no wake-up`() {
        val ended = daily8.copy(endDate = LocalDate.parse("2026-10-16"))

        assertNull(ReminderPlanner.nextWakeUp(listOf(ended), emptyList(), at("2026-10-16T13:00:00Z"), TORONTO))
    }

    @Test
    fun `an end date beyond the look-ahead still re-plans`() {
        val everyTenDays = daily8.copy(
            recurrence = Recurrence.EveryNDays(10),
            startDate = LocalDate.parse("2026-10-10"),
            endDate = LocalDate.parse("2026-10-20"),
        )
        val now = at("2026-10-16T11:00:00Z")

        assertEquals(
            now.plus(ReminderPlanner.LOOK_AHEAD),
            ReminderPlanner.nextWakeUp(listOf(everyTenDays), emptyList(), now, TORONTO),
        )
    }

    @Test
    fun `no active schedules means no wake-up`() {
        assertNull(ReminderPlanner.nextWakeUp(emptyList(), emptyList(), at("2026-10-16T11:00:00Z"), TORONTO))
    }

    @Test
    fun `a dose beyond the look-ahead re-plans at the end of the window`() {
        val everyTenDays = daily8.copy(
            recurrence = Recurrence.EveryNDays(10),
            startDate = LocalDate.parse("2026-10-10"),
        )
        val now = at("2026-10-16T11:00:00Z")

        val next = ReminderPlanner.nextWakeUp(listOf(everyTenDays), emptyList(), now, TORONTO)

        assertEquals(now.plus(ReminderPlanner.LOOK_AHEAD), next)
    }

    @Test
    fun `a schedule that has not started yet re-plans until it is near`() {
        val future = daily8.copy(startDate = LocalDate.parse("2026-11-01"))
        val now = at("2026-10-16T11:00:00Z")

        assertEquals(
            now.plus(ReminderPlanner.LOOK_AHEAD),
            ReminderPlanner.nextWakeUp(listOf(future), emptyList(), now, TORONTO),
        )
    }

    private data class ZoneCase(
        val name: String,
        val schedule: Schedule,
        val now: String,
        val zone: ZoneId,
        val expected: String,
    )

    @Test
    fun `wall-clock times resolve in the current zone`() {
        val cases = listOf(
            ZoneCase("Toronto EDT", daily8, "2026-10-16T11:00:00Z", TORONTO, "2026-10-16T12:00:00Z"),
            // Same schedule after the user flies to Vancouver: still 08:00 local.
            ZoneCase("Vancouver PDT", daily8, "2026-10-16T11:00:00Z", VANCOUVER, "2026-10-16T15:00:00Z"),
            ZoneCase("Tokyo, no DST", daily8, "2026-10-16T11:00:00Z", TOKYO, "2026-10-16T23:00:00Z"),
            // Fall back on 2026-11-01: 08:00 is EST (UTC-5) that day.
            ZoneCase("after fall back", daily8, "2026-11-01T05:00:00Z", TORONTO, "2026-11-01T13:00:00Z"),
            // 01:30 happens twice on fall-back day; the earlier offset is used, so one reminder.
            ZoneCase(
                "fall-back overlap",
                daily8.copy(timesOfDay = listOf(LocalTime.of(1, 30))),
                "2026-11-01T04:00:00Z",
                TORONTO,
                "2026-11-01T05:30:00Z",
            ),
            // Spring forward on 2027-03-14: 02:30 does not exist and moves to 03:30 EDT.
            ZoneCase(
                "spring-forward gap",
                daily8.copy(timesOfDay = listOf(LocalTime.of(2, 30))),
                "2027-03-14T05:00:00Z",
                TORONTO,
                "2027-03-14T07:30:00Z",
            ),
        )
        for (case in cases) {
            val next = ReminderPlanner.nextWakeUp(listOf(case.schedule), emptyList(), at(case.now), case.zone)
            assertEquals(case.name, at(case.expected), next)
        }
    }

    @Test
    fun `fall-back overlap gives exactly one reminder`() {
        val early = daily8.copy(timesOfDay = listOf(LocalTime.of(1, 30)))

        val due = ReminderPlanner.due(
            listOf(early),
            emptyList(),
            at("2026-11-01T04:00:00Z"),
            at("2026-11-01T08:00:00Z"),
            TORONTO,
        )

        assertEquals(listOf(at("2026-11-01T05:30:00Z")), due.map { it.remindAt })
    }

    @Test
    fun `editing a schedule's time moves the next wake-up`() {
        val now = at("2026-10-16T11:00:00Z")
        val edited = daily8.copy(timesOfDay = listOf(LocalTime.of(9, 15)))

        assertEquals(at("2026-10-16T13:15:00Z"), ReminderPlanner.nextWakeUp(listOf(edited), emptyList(), now, TORONTO))
    }

    @Test
    fun `deleting one of two schedules leaves only the other`() {
        val evening = schedule(timesOfDay = listOf(LocalTime.of(20, 0))).copy(id = 2)
        val now = at("2026-10-16T11:00:00Z")

        assertEquals(
            at("2026-10-16T12:00:00Z"),
            ReminderPlanner.nextWakeUp(listOf(daily8, evening), emptyList(), now, TORONTO),
        )
        assertEquals(at("2026-10-17T00:00:00Z"), ReminderPlanner.nextWakeUp(listOf(evening), emptyList(), now, TORONTO))
    }

    private fun snoozed(schedule: Schedule, scheduledAt: String, until: String) =
        log(schedule, scheduledAt, DoseStatus.SNOOZED_PENDING, at(until))

    private fun log(schedule: Schedule, scheduledAt: String, status: DoseStatus, snoozedUntil: Instant?) = DoseLog(
        medicationId = schedule.medicationId,
        scheduleId = schedule.id,
        scheduledAt = at(scheduledAt),
        takenAt = null,
        status = status,
        quantity = schedule.doseQuantity,
        note = null,
        snoozedUntil = snoozedUntil,
    )

    private fun at(text: String): Instant = Instant.parse(text)

    private companion object {
        val TORONTO: ZoneId = ZoneId.of("America/Toronto")
        val VANCOUVER: ZoneId = ZoneId.of("America/Vancouver")
        val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")
    }
}
