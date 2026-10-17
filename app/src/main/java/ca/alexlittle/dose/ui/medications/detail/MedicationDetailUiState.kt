package ca.alexlittle.dose.ui.medications.detail

import ca.alexlittle.dose.domain.model.Medication

internal sealed interface MedicationDetailUiState {
    data object Loading : MedicationDetailUiState

    data class Content(val medication: Medication) : MedicationDetailUiState

    /** Deleted, or the id never existed. The screen closes. */
    data object Missing : MedicationDetailUiState
}

internal sealed interface MedicationDetailEvent {
    data object Archived : MedicationDetailEvent
}
