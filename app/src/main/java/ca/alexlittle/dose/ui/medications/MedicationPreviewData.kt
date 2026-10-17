package ca.alexlittle.dose.ui.medications

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.ui.theme.MedicationPalette
import java.math.BigDecimal
import java.time.Instant

internal object MedicationPreviewData {
    private val createdAt = Instant.parse("2026-10-16T12:00:00Z")

    val metformin = Medication(
        id = 1,
        name = "Metformin",
        strengthValue = BigDecimal("500"),
        strengthUnit = "mg",
        form = MedicationForm.TABLET,
        colorArgb = MedicationPalette.colors[5].argb,
        notes = "Refill at the pharmacy on Main St.",
        instructions = "Take with food",
        archived = false,
        createdAt = createdAt,
    )

    val eyeDrops = metformin.copy(
        id = 2,
        name = "Lubricating eye drops",
        strengthValue = null,
        strengthUnit = null,
        form = MedicationForm.DROPS,
        colorArgb = MedicationPalette.colors[4].argb,
        notes = null,
        instructions = null,
    )

    val amoxicillin = metformin.copy(
        id = 3,
        name = "Amoxicillin",
        strengthValue = BigDecimal("250"),
        form = MedicationForm.CAPSULE,
        colorArgb = MedicationPalette.colors[1].argb,
        notes = null,
        instructions = null,
        archived = true,
    )
}
