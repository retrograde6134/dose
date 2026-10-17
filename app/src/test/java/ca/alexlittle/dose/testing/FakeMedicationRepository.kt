package ca.alexlittle.dose.testing

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakeMedicationRepository : MedicationRepository {
    private val medications = MutableStateFlow<Map<Long, Medication>>(emptyMap())
    private var nextId = 1L

    val all: List<Medication> get() = medications.value.values.toList()

    override fun observeActive(): Flow<List<Medication>> = sorted { !it.archived }

    override fun observeArchived(): Flow<List<Medication>> = sorted { it.archived }

    override fun observe(id: Long): Flow<Medication?> = medications.map { it[id] }

    override suspend fun get(id: Long): Medication? = medications.value[id]

    override suspend fun insert(medication: Medication): Long {
        val id = nextId++
        medications.update { it + (id to medication.copy(id = id)) }
        return id
    }

    override suspend fun update(medication: Medication): Boolean {
        if (medication.id !in medications.value) return false
        medications.update { it + (medication.id to medication) }
        return true
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        medications.update { map -> map[id]?.let { map + (id to it.copy(archived = archived)) } ?: map }
    }

    override suspend fun delete(id: Long) {
        medications.update { it - id }
    }

    private fun sorted(predicate: (Medication) -> Boolean) = medications.map { map ->
        map.values.filter(predicate).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}
