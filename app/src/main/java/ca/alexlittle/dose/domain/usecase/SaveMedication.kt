package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.repository.MedicationRepository
import java.math.BigDecimal
import java.time.Clock
import javax.inject.Inject

/** What the user entered on the edit screen. [id] is [Medication.NEW_ID] when adding. */
internal data class MedicationDraft(
    val id: Long = Medication.NEW_ID,
    val name: String,
    val strengthValue: BigDecimal?,
    val strengthUnit: String?,
    val form: MedicationForm,
    val colorArgb: Int,
    val notes: String?,
    val instructions: String?,
)

internal enum class MedicationError {
    NAME_BLANK,
    STRENGTH_NOT_POSITIVE,
    UNIT_MISSING,
}

internal sealed interface SaveMedicationResult {
    data class Saved(val id: Long) : SaveMedicationResult

    data class Invalid(val errors: Set<MedicationError>) : SaveMedicationResult

    /** The medication being edited was deleted in the meantime. */
    data object NotFound : SaveMedicationResult
}

internal class SaveMedication @Inject constructor(
    private val repository: MedicationRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: MedicationDraft): SaveMedicationResult {
        val name = draft.name.trim()
        val unit = draft.strengthUnit?.trim()?.ifEmpty { null }
        val strength = draft.strengthValue

        val errors = buildSet {
            if (name.isEmpty()) add(MedicationError.NAME_BLANK)
            if (strength != null && strength.signum() <= 0) add(MedicationError.STRENGTH_NOT_POSITIVE)
            if (strength != null && unit == null) add(MedicationError.UNIT_MISSING)
        }
        if (errors.isNotEmpty()) return SaveMedicationResult.Invalid(errors)

        // A unit with no amount means nothing on its own, so it is dropped rather than rejected.
        val fields = Medication(
            id = draft.id,
            name = name,
            strengthValue = strength?.normalized(),
            strengthUnit = unit.takeIf { strength != null },
            form = draft.form,
            colorArgb = draft.colorArgb,
            notes = draft.notes?.trim()?.ifEmpty { null },
            instructions = draft.instructions?.trim()?.ifEmpty { null },
            archived = false,
            createdAt = clock.instant(),
        )
        return if (fields.id == Medication.NEW_ID) insert(fields) else update(fields)
    }

    private suspend fun insert(medication: Medication): SaveMedicationResult =
        SaveMedicationResult.Saved(repository.insert(medication))

    private suspend fun update(fields: Medication): SaveMedicationResult {
        val existing = repository.get(fields.id)
        val saved = existing != null &&
            repository.update(fields.copy(archived = existing.archived, createdAt = existing.createdAt))
        return if (saved) SaveMedicationResult.Saved(fields.id) else SaveMedicationResult.NotFound
    }
}

// stripTrailingZeros alone turns 750 into 7.5E+2, which is not equal to the BigDecimal("750") read back from the DB.
private fun BigDecimal.normalized(): BigDecimal = stripTrailingZeros().let {
    if (it.scale() <
        0
    ) {
        it.setScale(0)
    } else {
        it
    }
}
