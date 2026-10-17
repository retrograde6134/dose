package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Archiving hides a medication but keeps its history, and stops its reminders. Unarchiving brings
 * them back from the next dose on.
 */
internal class SetMedicationArchived @Inject constructor(
    private val repository: MedicationRepository,
    private val notifier: ReminderNotifier,
    private val reschedule: RescheduleReminders,
) {
    suspend operator fun invoke(id: Long, archived: Boolean) {
        repository.setArchived(id, archived)
        if (archived) notifier.cancelForMedication(id)
        reschedule()
    }
}
