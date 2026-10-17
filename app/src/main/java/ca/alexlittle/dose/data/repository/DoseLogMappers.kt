package ca.alexlittle.dose.data.repository

import ca.alexlittle.dose.data.db.DoseLogEntity
import ca.alexlittle.dose.domain.model.DoseLog

internal fun DoseLogEntity.toDomain() = DoseLog(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = scheduledAt,
    takenAt = takenAt,
    status = status,
    quantity = quantity,
    note = note,
    snoozedUntil = snoozedUntil,
)

internal fun DoseLog.toEntity() = DoseLogEntity(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = scheduledAt,
    takenAt = takenAt,
    status = status,
    quantity = quantity,
    note = note,
    snoozedUntil = snoozedUntil,
)
