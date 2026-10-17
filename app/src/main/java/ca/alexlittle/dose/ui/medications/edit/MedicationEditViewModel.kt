package ca.alexlittle.dose.ui.medications.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.usecase.MedicationDraft
import ca.alexlittle.dose.domain.usecase.MedicationError
import ca.alexlittle.dose.domain.usecase.ObserveMedication
import ca.alexlittle.dose.domain.usecase.SaveMedication
import ca.alexlittle.dose.domain.usecase.SaveMedicationResult
import ca.alexlittle.dose.ui.medications.formatStrength
import ca.alexlittle.dose.ui.medications.parseStrength
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
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

@HiltViewModel(assistedFactory = MedicationEditViewModel.Factory::class)
internal class MedicationEditViewModel @AssistedInject constructor(
    @Assisted private val medicationId: Long,
    private val observeMedication: ObserveMedication,
    private val saveMedication: SaveMedication,
    private val locale: Locale,
) : ViewModel() {
    private val isNew = medicationId == Medication.NEW_ID

    private val _uiState = MutableStateFlow(MedicationEditUiState(isNew = isNew, loading = !isNew))
    val uiState: StateFlow<MedicationEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<MedicationEditEvent>(Channel.BUFFERED)
    val events: Flow<MedicationEditEvent> = _events.receiveAsFlow()

    init {
        if (!isNew) load()
    }

    fun onNameChange(name: String) = _uiState.update {
        it.copy(name = name, errors = it.errors - MedicationEditError.NAME_BLANK)
    }

    fun onStrengthChange(strength: String) = _uiState.update {
        it.copy(
            strength = strength,
            errors =
                it.errors - STRENGTH_ERRORS,
        )
    }

    fun onUnitChange(unit: String) = _uiState.update {
        it.copy(
            unit = unit,
            errors =
                it.errors - MedicationEditError.UNIT_MISSING,
        )
    }

    fun onFormChange(form: MedicationForm) = _uiState.update { it.copy(form = form) }

    fun onColorChange(colorArgb: Int) = _uiState.update { it.copy(colorArgb = colorArgb) }

    fun onNotesChange(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun onInstructionsChange(instructions: String) = _uiState.update { it.copy(instructions = instructions) }

    fun onSave() {
        val state = _uiState.value
        if (state.loading || state.saving) return

        val strengthText = state.strength.trim()
        val strength = strengthText.takeIf { it.isNotEmpty() }?.let { parseStrength(it, locale) }
        if (strengthText.isNotEmpty() && strength == null) {
            // Report the unparseable number together with any other field the domain would reject.
            val nameError = MedicationEditError.NAME_BLANK.takeIf { state.name.isBlank() }
            _uiState.update { it.copy(errors = setOfNotNull(MedicationEditError.STRENGTH_INVALID, nameError)) }
            return
        }

        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = saveMedication(state.toDraft(strength))) {
                is SaveMedicationResult.Saved, SaveMedicationResult.NotFound -> _events.send(MedicationEditEvent.Done)

                is SaveMedicationResult.Invalid -> _uiState.update {
                    it.copy(saving = false, errors = result.errors.mapTo(mutableSetOf(), ::toEditError))
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val medication = observeMedication(medicationId).first()
            if (medication == null) {
                _events.send(MedicationEditEvent.Done)
            } else {
                _uiState.update { medication.toUiState() }
            }
        }
    }

    private fun Medication.toUiState() = MedicationEditUiState(
        isNew = false,
        name = name,
        strength = strengthValue?.let { formatStrength(it, locale) }.orEmpty(),
        unit = strengthUnit.orEmpty(),
        form = form,
        colorArgb = colorArgb,
        notes = notes.orEmpty(),
        instructions = instructions.orEmpty(),
    )

    private fun MedicationEditUiState.toDraft(strength: BigDecimal?) = MedicationDraft(
        id = medicationId,
        name = name,
        strengthValue = strength,
        strengthUnit = unit,
        form = form,
        colorArgb = colorArgb,
        notes = notes,
        instructions = instructions,
    )

    @AssistedFactory
    interface Factory {
        fun create(medicationId: Long): MedicationEditViewModel
    }

    private companion object {
        // Changing the amount can resolve a missing-unit error too (by clearing the amount).
        val STRENGTH_ERRORS = setOf(
            MedicationEditError.STRENGTH_INVALID,
            MedicationEditError.STRENGTH_NOT_POSITIVE,
            MedicationEditError.UNIT_MISSING,
        )

        fun toEditError(error: MedicationError) = when (error) {
            MedicationError.NAME_BLANK -> MedicationEditError.NAME_BLANK
            MedicationError.STRENGTH_NOT_POSITIVE -> MedicationEditError.STRENGTH_NOT_POSITIVE
            MedicationError.UNIT_MISSING -> MedicationEditError.UNIT_MISSING
        }
    }
}
