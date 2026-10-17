package ca.alexlittle.dose.data.repository

import ca.alexlittle.dose.data.db.ScheduleDao
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomScheduleRepository @Inject constructor(private val dao: ScheduleDao) : ScheduleRepository {
    override fun observeForMedication(medicationId: Long): Flow<List<Schedule>> =
        dao.observeForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<List<Schedule>> = dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<Schedule?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun get(id: Long): Schedule? = dao.get(id)?.toDomain()

    override suspend fun insert(schedule: Schedule): Long = dao.insert(schedule.toEntity())

    override suspend fun update(schedule: Schedule): Boolean = dao.update(schedule.toEntity()) > 0

    override suspend fun delete(id: Long) = dao.delete(id)
}
