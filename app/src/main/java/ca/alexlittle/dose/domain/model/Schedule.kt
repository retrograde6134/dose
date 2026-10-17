package ca.alexlittle.dose.domain.model

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * When and how much of a medication to take. A medication can have several schedules.
 *
 * Times are local wall-clock times so "8:00" stays 8:00 across DST and travel. [timesOfDay] is
 * sorted and distinct, and empty only for [Recurrence.AsNeeded]. [endDate] is inclusive.
 */
internal data class Schedule(
    val id: Long = NEW_ID,
    val medicationId: Long,
    val recurrence: Recurrence,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val timesOfDay: List<LocalTime>,
    val doseQuantity: BigDecimal,
) {
    companion object {
        /** Id of a schedule that has not been saved yet. */
        const val NEW_ID = 0L
    }
}

/** Which days a [Schedule] falls on. Interval and cycle counts are anchored to the start date. */
internal sealed interface Recurrence {
    data object Daily : Recurrence

    /** On [days] of every [everyNWeeks]th week, counted from the week (Monday-based) of the start date. */
    data class Weekly(val days: Set<DayOfWeek>, val everyNWeeks: Int = 1) : Recurrence

    /** Every [n]th day starting on the start date. */
    data class EveryNDays(val n: Int) : Recurrence

    /** [onDays] days on then [offDays] days off, repeating from the start date. */
    data class Cyclic(val onDays: Int, val offDays: Int) : Recurrence

    /** No reminders; logged manually, optionally no sooner than [minIntervalMinutes] after the last dose. */
    data class AsNeeded(val minIntervalMinutes: Int?) : Recurrence
}
