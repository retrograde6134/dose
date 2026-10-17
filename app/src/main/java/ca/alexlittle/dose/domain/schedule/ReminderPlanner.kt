package ca.alexlittle.dose.domain.schedule

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseLog
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A dose that needs a reminder at [remindAt]: its scheduled time, or the end of a snooze. */
internal data class DueDose(val key: DoseKey, val medicationId: Long, val quantity: BigDecimal, val remindAt: Instant)

/**
 * Decides which reminders are due and when the single reminder alarm should next fire. Pure, like
 * [ScheduleEngine]: callers pass the active schedules, the logs covering the window, and the device's
 * current zone.
 *
 * A scheduled dose needs a reminder at its scheduled time unless it already has a log of any status.
 * A snoozed dose needs one at its `snoozedUntil`, as long as its schedule is still active.
 */
internal object ReminderPlanner {
    /** How far ahead the next alarm is looked for. Only one alarm is ever armed. */
    val LOOK_AHEAD: Duration = Duration.ofHours(48)

    /** Reminders in `[from, until)`, earliest first, then by dose identity so the order is stable. */
    fun due(
        schedules: List<Schedule>,
        logs: List<DoseLog>,
        from: Instant,
        until: Instant,
        zone: ZoneId,
    ): List<DueDose> {
        val logged = logs.mapNotNullTo(HashSet()) { it.key }
        val scheduled = schedules.asSequence().flatMap { schedule ->
            ScheduleEngine.doseTimes(schedule, from, until, zone).asSequence()
                .map { DueDose(DoseKey(schedule.id, it), schedule.medicationId, schedule.doseQuantity, it) }
        }.filter { it.key !in logged }

        val byId = schedules.associateBy { it.id }
        val snoozed = logs.asSequence()
            .filter { it.status == DoseStatus.SNOOZED_PENDING }
            .mapNotNull { log ->
                val key = log.key ?: return@mapNotNull null
                val schedule = byId[key.scheduleId] ?: return@mapNotNull null
                val at = log.snoozedUntil ?: return@mapNotNull null
                DueDose(key, schedule.medicationId, log.quantity, at)
            }
            .filter { !it.remindAt.isBefore(from) && it.remindAt.isBefore(until) }

        return (scheduled + snoozed)
            .distinctBy { it.key }
            .sortedWith(compareBy({ it.remindAt }, { it.key.scheduleId }, { it.key.scheduledAt }))
            .toList()
    }

    /**
     * When the reminder alarm should next fire, strictly after [now]: the first due reminder within
     * [LOOK_AHEAD]. If there is none but a schedule can still produce doses later (every two weeks,
     * or the off days of a cycle), the end of the window, so the plan is redone then rather than never.
     * Null when nothing can ever need a reminder.
     */
    fun nextWakeUp(schedules: List<Schedule>, logs: List<DoseLog>, now: Instant, zone: ZoneId): Instant? {
        val horizon = now.plus(LOOK_AHEAD)
        due(schedules, logs, now.plusNanos(1), horizon, zone).firstOrNull()?.let { return it.remindAt }
        // An end date inside the window means every remaining dose was already searched.
        val lastSearched = horizon.atZone(zone).toLocalDate()
        val anyLater = schedules.any {
            it.recurrence !is Recurrence.AsNeeded && (it.endDate == null || it.endDate >= lastSearched)
        }
        return horizon.takeIf { anyLater }
    }
}
