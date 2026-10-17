package ca.alexlittle.dose.ui.medications.edit

import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.ui.theme.MedicationPalette

/** Form fields hold the raw text the user typed; parsing happens on save. */
internal data class MedicationEditUiState(
    val isNew: Boolean,
    val loading: Boolean = false,
    val name: String = "",
    val strength: String = "",
    val unit: String = "",
    val form: MedicationForm = MedicationForm.TABLET,
    val colorArgb: Int = MedicationPalette.default.argb,
    val notes: String = "",
    val instructions: String = "",
    val errors: Set<MedicationEditError> = emptySet(),
    val saving: Boolean = false,
)

internal enum class MedicationEditError {
    NAME_BLANK,
    STRENGTH_INVALID,
    STRENGTH_NOT_POSITIVE,
    UNIT_MISSING,
}

internal sealed interface MedicationEditEvent {
    /** A new medication was saved; the screen moves on to it so schedules can be added next. */
    data class Added(val id: Long) : MedicationEditEvent

    /** An edit was saved, or the medication no longer exists. Either way the screen closes. */
    data object Done : MedicationEditEvent
}

/** Callbacks for every editable field, grouped to keep the screen signature readable. */
internal data class MedicationEditActions(
    val onNameChange: (String) -> Unit,
    val onStrengthChange: (String) -> Unit,
    val onUnitChange: (String) -> Unit,
    val onFormChange: (MedicationForm) -> Unit,
    val onColorChange: (Int) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onInstructionsChange: (String) -> Unit,
    val onSave: () -> Unit,
)
