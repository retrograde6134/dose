package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.DoseLog
import ca.alexlittle.dose.domain.reminder.ReminderAlarms
import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.repository.DoseLogRepository
import ca.alexlittle.dose.domain.repository.MedicationRepository
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import ca.alexlittle.dose.domain.schedule.DueDose
import ca.alexlittle.dose.domain.schedule.ReminderPlanner
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The one path that arms the reminder alarm. Everything that can change when the next reminder is due
 * (a schedule edit, a dose action, a reboot, a clock or zone change) ends here.
 */
internal class RescheduleReminders @Inject constructor(
    private val schedules: ScheduleRepository,
    private val logs: DoseLogRepository,
    private val medications: MedicationRepository,
    private val alarms: ReminderAlarms,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    /**
     * Arms the alarm for the next reminder after now. With [catchUp], first shows reminders that came
     * due in the last [CATCH_UP_WINDOW] while no alarm could fire (the phone was off, or the clock
     * jumped), skipping any still on screen so nothing alerts twice.
     */
    suspend operator fun invoke(catchUp: Boolean = false) {
        val now = clock.instant()
        val zone = clock.zone
        val active = schedules.observeActive().first()
        val recent = recentLogs(now)
        if (catchUp) {
            val due = ReminderPlanner.due(active, recent, now.minus(CATCH_UP_WINDOW), now.plusNanos(1), zone)
            show(due.filterNot { notifier.isShowing(it.key) })
        }
        when (val next = ReminderPlanner.nextWakeUp(active, recent, now, zone)) {
            null -> alarms.cancel()
            else -> alarms.arm(next)
        }
    }

    /**
     * Called when the alarm armed for [armedAt] fires. Shows everything due from then until now, so
     * a late alarm (Doze, or no exact-alarm permission) still covers doses that came due meanwhile,
     * then arms the next one.
     */
    suspend fun onAlarm(armedAt: Instant) {
        val now = clock.instant()
        if (!armedAt.isAfter(now)) {
            val active = schedules.observeActive().first()
            show(ReminderPlanner.due(active, recentLogs(now, since = armedAt), armedAt, now.plusNanos(1), clock.zone))
        }
        invoke()
    }

    private suspend fun show(due: List<DueDose>) {
        for (dose in due) {
            val medication = medications.get(dose.medicationId) ?: continue
            notifier.show(dose, medication)
        }
    }

    // Snoozes can belong to doses scheduled long ago, so they are fetched separately from the window.
    private suspend fun recentLogs(now: Instant, since: Instant = now.minus(CATCH_UP_WINDOW)): List<DoseLog> = (
        logs.scheduledBetween(minOf(since, now.minus(CATCH_UP_WINDOW)), now.plus(ReminderPlanner.LOOK_AHEAD)) +
            logs.snoozedPending()
        )
        .distinctBy { it.id }

    companion object {
        val CATCH_UP_WINDOW: Duration = Duration.ofMinutes(60)
    }
}
