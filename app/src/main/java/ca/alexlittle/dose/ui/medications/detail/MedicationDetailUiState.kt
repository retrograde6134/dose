package ca.alexlittle.dose.ui.medications.detail

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.Schedule
import java.time.LocalDate

internal sealed interface MedicationDetailUiState {
    data object Loading : MedicationDetailUiState

    /** [today] decides whether a schedule's start date is still ahead. */
    data class Content(val medication: Medication, val schedules: List<Schedule>, val today: LocalDate) :
        MedicationDetailUiState

    /** Deleted, or the id never existed. The screen closes. */
    data object Missing : MedicationDetailUiState
}

internal sealed interface MedicationDetailEvent {
    data object Archived : MedicationDetailEvent
}
