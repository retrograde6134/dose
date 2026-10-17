package ca.alexlittle.dose.testing

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.usecase.MedicationDraft
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal val FIXED_INSTANT: Instant = Instant.parse("2026-10-16T12:00:00Z")

internal fun fixedClock(instant: Instant = FIXED_INSTANT): Clock = Clock.fixed(instant, ZoneId.of("America/Toronto"))

internal const val TEST_COLOR = 0xFF1565C0.toInt()

internal fun draft(
    id: Long = Medication.NEW_ID,
    name: String = "Metformin",
    strengthValue: String? = "500",
    strengthUnit: String? = "mg",
    form: MedicationForm = MedicationForm.TABLET,
    notes: String? = null,
    instructions: String? = null,
) = MedicationDraft(
    id = id,
    name = name,
    strengthValue = strengthValue?.let(::BigDecimal),
    strengthUnit = strengthUnit,
    form = form,
    colorArgb = TEST_COLOR,
    notes = notes,
    instructions = instructions,
)

internal fun medication(name: String = "Metformin", archived: Boolean = false, createdAt: Instant = FIXED_INSTANT) =
    Medication(
        name = name,
        strengthValue = BigDecimal("500"),
        strengthUnit = "mg",
        form = MedicationForm.TABLET,
        colorArgb = TEST_COLOR,
        notes = null,
        instructions = null,
        archived = archived,
        createdAt = createdAt,
    )
