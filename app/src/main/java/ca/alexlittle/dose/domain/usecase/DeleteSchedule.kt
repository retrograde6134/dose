package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import javax.inject.Inject

/** Its dose history stays, unlinked from the schedule. Its reminders stop, including any on screen. */
internal class DeleteSchedule @Inject constructor(
    private val repository: ScheduleRepository,
    private val notifier: ReminderNotifier,
    private val reschedule: RescheduleReminders,
) {
    suspend operator fun invoke(id: Long) {
        repository.delete(id)
        notifier.cancelForSchedule(id)
        reschedule()
    }
}
