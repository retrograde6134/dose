package ca.alexlittle.dose.data.repository

import ca.alexlittle.dose.data.db.DoseLogDao
import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseLog
import ca.alexlittle.dose.domain.repository.DoseLogRepository
import java.time.Instant
import javax.inject.Inject

internal class RoomDoseLogRepository @Inject constructor(private val dao: DoseLogDao) : DoseLogRepository {
    override suspend fun get(key: DoseKey): DoseLog? = dao.get(key.scheduleId, key.scheduledAt)?.toDomain()

    override suspend fun scheduledBetween(from: Instant, until: Instant): List<DoseLog> =
        dao.scheduledBetween(from, until).map { it.toDomain() }

    override suspend fun snoozedPending(): List<DoseLog> = dao.snoozedPending().map { it.toDomain() }

    override suspend fun modify(key: DoseKey, transform: (DoseLog?) -> DoseLog?): DoseLog? =
        dao.modify(key.scheduleId, key.scheduledAt) { current ->
            transform(current?.toDomain())
                ?.copy(scheduleId = key.scheduleId, scheduledAt = key.scheduledAt)
                ?.toEntity()
        }?.toDomain()
}
