package ca.alexlittle.dose.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MedicationDao {
    @Query("SELECT * FROM medications WHERE archived = 0 ORDER BY name COLLATE NOCASE, id")
    fun observeActive(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE archived = 1 ORDER BY name COLLATE NOCASE, id")
    fun observeArchived(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun observe(id: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun get(id: Long): MedicationEntity?

    @Insert
    suspend fun insert(medication: MedicationEntity): Long

    /** Returns the number of rows updated. */
    @Update
    suspend fun update(medication: MedicationEntity): Int

    @Query("UPDATE medications SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun delete(id: Long)
}
