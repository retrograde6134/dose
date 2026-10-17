package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.repository.MedicationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class ObserveMedication @Inject constructor(private val repository: MedicationRepository) {
    operator fun invoke(id: Long): Flow<Medication?> = repository.observe(id)
}
