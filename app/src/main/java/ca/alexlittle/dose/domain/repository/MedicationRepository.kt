package ca.alexlittle.dose.domain.repository

import ca.alexlittle.dose.domain.model.Medication
import kotlinx.coroutines.flow.Flow

internal interface MedicationRepository {
    /** Non-archived medications, sorted by name ignoring case. */
    fun observeActive(): Flow<List<Medication>>

    /** Archived medications, sorted by name ignoring case. */
    fun observeArchived(): Flow<List<Medication>>

    /** Emits null once the medication is deleted or if it never existed. */
    fun observe(id: Long): Flow<Medication?>

    suspend fun get(id: Long): Medication?

    /** Returns the new medication's id. */
    suspend fun insert(medication: Medication): Long

    /** Returns false if no medication with that id exists. */
    suspend fun update(medication: Medication): Boolean

    suspend fun setArchived(id: Long, archived: Boolean)

    suspend fun delete(id: Long)
}
