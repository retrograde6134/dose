package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Archiving hides a medication but keeps its history. Once reminders exist, this is also where its
 * pending alarms get cancelled (on archive) or re-armed (on unarchive).
 */
internal class SetMedicationArchived @Inject constructor(private val repository: MedicationRepository) {
    suspend operator fun invoke(id: Long, archived: Boolean) = repository.setArchived(id, archived)
}
