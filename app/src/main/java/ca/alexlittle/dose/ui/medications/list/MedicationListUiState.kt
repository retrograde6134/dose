package ca.alexlittle.dose.ui.medications.list

import ca.alexlittle.dose.domain.model.Medication

internal sealed interface MedicationListUiState {
    data object Loading : MedicationListUiState

    data class Content(val active: List<Medication>, val archived: List<Medication>) : MedicationListUiState
}
