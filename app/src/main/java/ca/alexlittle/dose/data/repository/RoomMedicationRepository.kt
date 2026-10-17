package ca.alexlittle.dose.data.repository

import ca.alexlittle.dose.data.db.MedicationDao
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.repository.MedicationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomMedicationRepository @Inject constructor(private val dao: MedicationDao) : MedicationRepository {
    override fun observeActive(): Flow<List<Medication>> = dao.observeActive().map { list ->
        list.map { it.toDomain() }
    }

    override fun observeArchived(): Flow<List<Medication>> =
        dao.observeArchived().map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<Medication?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun get(id: Long): Medication? = dao.get(id)?.toDomain()

    override suspend fun insert(medication: Medication): Long = dao.insert(medication.toEntity())

    override suspend fun update(medication: Medication): Boolean = dao.update(medication.toEntity()) > 0

    override suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived)

    override suspend fun delete(id: Long) = dao.delete(id)
}
