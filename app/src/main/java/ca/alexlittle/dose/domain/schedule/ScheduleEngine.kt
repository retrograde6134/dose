package ca.alexlittle.dose.domain.schedule

import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Turns a [Schedule] into concrete dose times. Pure: the zone is passed in so callers resolve wall-clock
 * times in the device's *current* zone, and future doses are always derived here rather than stored.
 *
 * Expects schedules that passed `SaveSchedule` validation (positive intervals and cycle lengths).
 */
internal object ScheduleEngine {
    /** Whether [schedule] has doses on [date]. Always false for as-needed schedules. */
    fun occursOn(schedule: Schedule, date: LocalDate): Boolean =
        date >= schedule.startDate && (schedule.endDate == null || date <= schedule.endDate) &&
            matchesRecurrence(schedule, date)

    /**
     * Dose instants of [schedule] in `[from, until)`, sorted and distinct, with each time of day resolved
     * in [zone] on the dates the schedule occurs.
     *
     * DST: a time inside a spring-forward gap moves later by the length of the gap (02:30 becomes 03:30),
     * so the dose still happens that day. A time inside a fall-back overlap uses the earlier offset, so it
     * happens once, not twice. Two times that land on the same instant this way are one dose.
     */
    fun doseTimes(schedule: Schedule, from: Instant, until: Instant, zone: ZoneId): List<Instant> {
        if (!from.isBefore(until) || schedule.timesOfDay.isEmpty()) return emptyList()
        // A gap can push a dose past midnight into the next local date, so start one day early.
        val firstDate = maxOf(from.atZone(zone).toLocalDate().minusDays(1), schedule.startDate)
        val lastDate = until.atZone(zone).toLocalDate().let { last ->
            schedule.endDate?.let { minOf(it, last) } ?: last
        }

        return generateSequence(firstDate) { it.plusDays(1) }
            .takeWhile { it <= lastDate }
            .filter { occursOn(schedule, it) }
            .flatMap { date -> schedule.timesOfDay.asSequence().map { ZonedDateTime.of(date, it, zone).toInstant() } }
            .filter { !it.isBefore(from) && it.isBefore(until) }
            .distinct()
            .sorted()
            .toList()
    }

    private fun matchesRecurrence(schedule: Schedule, date: LocalDate): Boolean {
        val daysSinceStart = ChronoUnit.DAYS.between(schedule.startDate, date)
        return when (val r = schedule.recurrence) {
            Recurrence.Daily -> true

            is Recurrence.Weekly ->
                date.dayOfWeek in r.days &&
                    ChronoUnit.WEEKS.between(weekStart(schedule.startDate), weekStart(date)) % r.everyNWeeks == 0L

            is Recurrence.EveryNDays -> daysSinceStart % r.n == 0L

            is Recurrence.Cyclic -> daysSinceStart % (r.onDays + r.offDays) < r.onDays

            is Recurrence.AsNeeded -> false
        }
    }

    // Weeks are counted Monday to Sunday everywhere so the every-N-weeks cycle never shifts with the locale.
    private fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
