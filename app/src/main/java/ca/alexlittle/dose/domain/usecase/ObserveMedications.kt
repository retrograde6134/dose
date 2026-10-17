package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.repository.MedicationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class MedicationLists(val active: List<Medication>, val archived: List<Medication>)

internal class ObserveMedications @Inject constructor(private val repository: MedicationRepository) {
    operator fun invoke(): Flow<MedicationLists> =
        combine(repository.observeActive(), repository.observeArchived(), ::MedicationLists)
}
