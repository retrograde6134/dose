package ca.alexlittle.dose.ui.schedules.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.usecase.DeleteSchedule
import ca.alexlittle.dose.domain.usecase.ObserveMedication
import ca.alexlittle.dose.domain.usecase.ObserveSchedule
import ca.alexlittle.dose.domain.usecase.SaveSchedule
import ca.alexlittle.dose.domain.usecase.SaveScheduleResult
import ca.alexlittle.dose.domain.usecase.ScheduleDraft
import ca.alexlittle.dose.domain.usecase.ScheduleError
import ca.alexlittle.dose.ui.medications.formatStrength
import ca.alexlittle.dose.ui.medications.parseStrength
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ScheduleEditViewModel.Factory::class)
internal class ScheduleEditViewModel @AssistedInject constructor(
    @Assisted("medicationId") private val medicationId: Long,
    @Assisted("scheduleId") private val scheduleId: Long,
    private val observeMedication: ObserveMedication,
    private val observeSchedule: ObserveSchedule,
    private val saveSchedule: SaveSchedule,
    private val deleteSchedule: DeleteSchedule,
    private val locale: Locale,
    clock: Clock,
) : ViewModel() {
    private val isNew = scheduleId == Schedule.NEW_ID

    private val _uiState = MutableStateFlow(ScheduleEditUiState(isNew = isNew, startDate = LocalDate.now(clock)))
    val uiState: StateFlow<ScheduleEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<ScheduleEditEvent>(Channel.BUFFERED)
    val events: Flow<ScheduleEditEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ScheduleEditAction) {
        when (action) {
            ScheduleEditAction.Save -> save()
            ScheduleEditAction.Delete -> delete()
            else -> _uiState.update { it.reduce(action) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val medication = observeMedication(medicationId).first()
            val schedule = if (isNew) null else observeSchedule(scheduleId).first()
            if (medication == null || (!isNew && schedule == null)) {
                _events.send(ScheduleEditEvent.Done)
            } else {
                _uiState.update { state ->
                    val loaded = schedule?.let { state.withSchedule(it) } ?: state
                    loaded.copy(loading = false, medicationName = medication.name, form = medication.form)
                }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.loading || state.saving) return

        val quantity = parseStrength(state.quantity, locale)
        if (quantity == null) {
            _uiState.update { it.copy(errors = it.errors + ScheduleEditError.QUANTITY_INVALID) }
            return
        }

        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = saveSchedule(state.toDraft(quantity))) {
                is SaveScheduleResult.Saved, SaveScheduleResult.NotFound -> _events.send(ScheduleEditEvent.Done)

                is SaveScheduleResult.Invalid -> _uiState.update {
                    it.copy(saving = false, errors = result.errors.mapNotNullTo(mutableSetOf(), ::toEditError))
                }
            }
        }
    }

    private fun delete() {
        if (isNew) return
        viewModelScope.launch {
            deleteSchedule(scheduleId)
            _events.send(ScheduleEditEvent.Done)
        }
    }

    private fun ScheduleEditUiState.withSchedule(schedule: Schedule): ScheduleEditUiState {
        val base = copy(
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            times = schedule.timesOfDay,
            quantity = formatStrength(schedule.doseQuantity, locale),
        )
        return when (val r = schedule.recurrence) {
            Recurrence.Daily -> base.copy(kind = ScheduleKind.DAILY)

            is Recurrence.Weekly ->
                base.copy(kind = ScheduleKind.WEEKDAYS, weekdays = r.days, everyNWeeks = r.everyNWeeks)

            is Recurrence.EveryNDays -> base.copy(kind = ScheduleKind.INTERVAL, everyNDays = r.n)

            is Recurrence.Cyclic ->
                base.copy(kind = ScheduleKind.CYCLIC, cycleOnDays = r.onDays, cycleOffDays = r.offDays)

            // As-needed has no times; keep the default so switching to a scheduled kind starts with one.
            is Recurrence.AsNeeded -> base.copy(
                kind = ScheduleKind.AS_NEEDED,
                minIntervalMinutes = r.minIntervalMinutes ?: 0,
                times = this.times,
            )
        }
    }

    private fun ScheduleEditUiState.toDraft(quantity: BigDecimal) = ScheduleDraft(
        id = scheduleId,
        medicationId = medicationId,
        recurrence = when (kind) {
            ScheduleKind.DAILY -> Recurrence.Daily
            ScheduleKind.WEEKDAYS -> Recurrence.Weekly(weekdays, everyNWeeks)
            ScheduleKind.INTERVAL -> Recurrence.EveryNDays(everyNDays)
            ScheduleKind.CYCLIC -> Recurrence.Cyclic(cycleOnDays, cycleOffDays)
            ScheduleKind.AS_NEEDED -> Recurrence.AsNeeded(minIntervalMinutes.takeIf { it > 0 })
        },
        startDate = startDate,
        endDate = endDate,
        timesOfDay = times,
        doseQuantity = quantity,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("medicationId") medicationId: Long,
            @Assisted("scheduleId") scheduleId: Long,
        ): ScheduleEditViewModel
    }

    private companion object {
        // The steppers keep counts in range, so the interval and cycle errors cannot come from this screen.
        fun toEditError(error: ScheduleError): ScheduleEditError? = when (error) {
            ScheduleError.NO_TIMES -> ScheduleEditError.NO_TIMES

            ScheduleError.NO_WEEKDAYS -> ScheduleEditError.NO_WEEKDAYS

            ScheduleError.QUANTITY_NOT_POSITIVE -> ScheduleEditError.QUANTITY_NOT_POSITIVE

            ScheduleError.END_BEFORE_START -> ScheduleEditError.END_BEFORE_START

            ScheduleError.INTERVAL_NOT_POSITIVE,
            ScheduleError.CYCLE_NOT_POSITIVE,
            ScheduleError.MIN_INTERVAL_NEGATIVE,
            -> null
        }
    }
}
