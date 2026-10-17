package ca.alexlittle.dose.ui.today

internal sealed interface TodayUiState {
    data object Loading : TodayUiState

    data object Empty : TodayUiState
}
