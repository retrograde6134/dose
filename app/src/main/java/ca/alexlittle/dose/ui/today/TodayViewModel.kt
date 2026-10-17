package ca.alexlittle.dose.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.alexlittle.dose.domain.usecase.ObserveHasReminders
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class TodayViewModel @Inject constructor(observeHasReminders: ObserveHasReminders) : ViewModel() {
    val uiState: StateFlow<TodayUiState> = observeHasReminders()
        .map { TodayUiState(loading = false, hasReminders = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TodayUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
