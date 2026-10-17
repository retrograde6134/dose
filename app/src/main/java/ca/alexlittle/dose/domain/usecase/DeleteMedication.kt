package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Permanently deletes a medication. Its schedules and dose history go with it (foreign keys cascade),
 * so the UI must confirm first. Its reminders stop, including any on screen.
 */
internal class DeleteMedication @Inject constructor(
    private val repository: MedicationRepository,
    private val notifier: ReminderNotifier,
    private val reschedule: RescheduleReminders,
) {
    suspend operator fun invoke(id: Long) {
        repository.delete(id)
        notifier.cancelForMedication(id)
        reschedule()
    }
}
