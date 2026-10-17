package ca.alexlittle.dose.testing

import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** [isActive] stands in for the join on non-archived medications. */
internal class FakeScheduleRepository(private val isActive: (medicationId: Long) -> Boolean = { true }) :
    ScheduleRepository {
    private val schedules = MutableStateFlow<Map<Long, Schedule>>(emptyMap())
    private var nextId = 1L

    val all: List<Schedule> get() = schedules.value.values.sortedBy { it.id }

    override fun observeForMedication(medicationId: Long): Flow<List<Schedule>> =
        schedules.map { map -> map.values.filter { it.medicationId == medicationId }.sortedBy { it.id } }

    override fun observeActive(): Flow<List<Schedule>> =
        schedules.map { map -> map.values.filter { isActive(it.medicationId) }.sortedBy { it.id } }

    override fun observe(id: Long): Flow<Schedule?> = schedules.map { it[id] }

    override suspend fun get(id: Long): Schedule? = schedules.value[id]

    override suspend fun insert(schedule: Schedule): Long {
        val id = nextId++
        schedules.update { it + (id to schedule.copy(id = id)) }
        return id
    }

    override suspend fun update(schedule: Schedule): Boolean {
        if (schedule.id !in schedules.value) return false
        schedules.update { it + (schedule.id to schedule) }
        return true
    }

    override suspend fun delete(id: Long) {
        schedules.update { it - id }
    }
}
