package ca.alexlittle.dose.ui.medications.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.alexlittle.dose.domain.usecase.DeleteMedication
import ca.alexlittle.dose.domain.usecase.ObserveMedication
import ca.alexlittle.dose.domain.usecase.ObserveSchedules
import ca.alexlittle.dose.domain.usecase.SetMedicationArchived
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = MedicationDetailViewModel.Factory::class)
internal class MedicationDetailViewModel @AssistedInject constructor(
    @Assisted private val medicationId: Long,
    observeMedication: ObserveMedication,
    observeSchedules: ObserveSchedules,
    clock: Clock,
    private val setMedicationArchived: SetMedicationArchived,
    private val deleteMedication: DeleteMedication,
) : ViewModel() {
    val uiState: StateFlow<MedicationDetailUiState> =
        combine(observeMedication(medicationId), observeSchedules(medicationId)) { medication, schedules ->
            if (medication == null) {
                MedicationDetailUiState.Missing
            } else {
                MedicationDetailUiState.Content(medication, schedules, LocalDate.now(clock))
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MedicationDetailUiState.Loading,
            )

    private val _events = Channel<MedicationDetailEvent>(Channel.BUFFERED)
    val events: Flow<MedicationDetailEvent> = _events.receiveAsFlow()

    fun onArchive() {
        viewModelScope.launch {
            setMedicationArchived(medicationId, archived = true)
            _events.send(MedicationDetailEvent.Archived)
        }
    }

    fun onUnarchive() {
        viewModelScope.launch { setMedicationArchived(medicationId, archived = false) }
    }

    fun onDelete() {
        viewModelScope.launch { deleteMedication(medicationId) }
    }

    @AssistedFactory
    interface Factory {
        fun create(medicationId: Long): MedicationDetailViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
