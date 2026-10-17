package ca.alexlittle.dose.ui.medications.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.alexlittle.dose.domain.usecase.ObserveMedications
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class MedicationListViewModel @Inject constructor(observeMedications: ObserveMedications) : ViewModel() {
    val uiState: StateFlow<MedicationListUiState> = observeMedications()
        .map<_, MedicationListUiState> { MedicationListUiState.Content(active = it.active, archived = it.archived) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MedicationListUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
