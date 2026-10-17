package ca.alexlittle.dose.domain.repository

import ca.alexlittle.dose.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

internal interface ScheduleRepository {
    /** A medication's schedules in the order they were added. */
    fun observeForMedication(medicationId: Long): Flow<List<Schedule>>

    /** Schedules of all non-archived medications. */
    fun observeActive(): Flow<List<Schedule>>

    /** Emits null once the schedule is deleted or if it never existed. */
    fun observe(id: Long): Flow<Schedule?>

    suspend fun get(id: Long): Schedule?

    /** Returns the new schedule's id. */
    suspend fun insert(schedule: Schedule): Long

    /** Returns false if no schedule with that id exists. */
    suspend fun update(schedule: Schedule): Boolean

    suspend fun delete(id: Long)
}
