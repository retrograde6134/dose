package ca.alexlittle.dose.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * A medication the user takes. Schedules and dose history reference it by [id].
 *
 * [strengthValue] and [strengthUnit] are either both set or both null.
 */
internal data class Medication(
    val id: Long = NEW_ID,
    val name: String,
    val strengthValue: BigDecimal?,
    val strengthUnit: String?,
    val form: MedicationForm,
    val colorArgb: Int,
    val notes: String?,
    val instructions: String?,
    val archived: Boolean,
    val createdAt: Instant,
) {
    companion object {
        /** Id of a medication that has not been saved yet. */
        const val NEW_ID = 0L
    }
}

/** Persisted by [name], so entries can be reordered but never renamed without a migration. */
internal enum class MedicationForm {
    TABLET,
    CAPSULE,
    LIQUID,
    INJECTION,
    DROPS,
    INHALER,
    OTHER,
}
