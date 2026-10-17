package ca.alexlittle.dose.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId ORDER BY id")
    fun observeForMedication(medicationId: Long): Flow<List<ScheduleEntity>>

    /** Schedules of non-archived medications. */
    @Query(
        """
        SELECT schedules.* FROM schedules
        INNER JOIN medications ON medications.id = schedules.medicationId
        WHERE medications.archived = 0
        ORDER BY schedules.id
        """,
    )
    fun observeActive(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun observe(id: Long): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun get(id: Long): ScheduleEntity?

    @Insert
    suspend fun insert(schedule: ScheduleEntity): Long

    /** Returns the number of rows updated. */
    @Update
    suspend fun update(schedule: ScheduleEntity): Int

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)
}
