package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseLog
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.repository.DoseLogRepository
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Shared by [TakeDose], [SkipDose] and [SnoozeDose]: writes the dose's log atomically, clears its
 * notification, and re-arms the alarm. Routine actions, so none of them ever asks for confirmation.
 */
internal class DoseLogWriter @Inject constructor(
    private val logs: DoseLogRepository,
    private val schedules: ScheduleRepository,
    private val notifier: ReminderNotifier,
    private val reschedule: RescheduleReminders,
    private val clock: Clock,
) {
    /** [update] gets the current log (if any), the dose's schedule and now; null leaves the log unchanged. */
    suspend fun write(key: DoseKey, update: (current: DoseLog?, schedule: Schedule, now: Instant) -> DoseLog?) {
        // A tap on a reminder for a schedule deleted since only needs the notification gone.
        val schedule = schedules.get(key.scheduleId)
        if (schedule != null) {
            val now = clock.instant()
            logs.modify(key) { current -> update(current, schedule, now) }
        }
        notifier.cancel(key)
        reschedule()
    }
}

/** A fresh log for a dose that has none yet. */
internal fun newLog(key: DoseKey, schedule: Schedule, status: DoseStatus) = DoseLog(
    medicationId = schedule.medicationId,
    scheduleId = key.scheduleId,
    scheduledAt = key.scheduledAt,
    takenAt = null,
    status = status,
    quantity = schedule.doseQuantity,
    note = null,
    snoozedUntil = null,
)
