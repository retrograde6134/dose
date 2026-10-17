package ca.alexlittle.dose.data.repository

import ca.alexlittle.dose.data.db.MedicationEntity
import ca.alexlittle.dose.domain.model.Medication

internal fun MedicationEntity.toDomain() = Medication(
    id = id,
    name = name,
    strengthValue = strengthValue,
    strengthUnit = strengthUnit,
    form = form,
    colorArgb = colorArgb,
    notes = notes,
    instructions = instructions,
    archived = archived,
    createdAt = createdAt,
)

internal fun Medication.toEntity() = MedicationEntity(
    id = id,
    name = name,
    strengthValue = strengthValue,
    strengthUnit = strengthUnit,
    form = form,
    colorArgb = colorArgb,
    notes = notes,
    instructions = instructions,
    archived = archived,
    createdAt = createdAt,
)
