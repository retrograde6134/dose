package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Permanently deletes a medication. Its schedules and dose history go with it (foreign keys cascade),
 * so the UI must confirm first. Once reminders exist, this is also where its alarms get cancelled.
 */
internal class DeleteMedication @Inject constructor(private val repository: MedicationRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
