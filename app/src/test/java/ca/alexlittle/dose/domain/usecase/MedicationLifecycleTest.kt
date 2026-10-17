package ca.alexlittle.dose.domain.usecase

import app.cash.turbine.test
import ca.alexlittle.dose.testing.FakeMedicationRepository
import ca.alexlittle.dose.testing.medication
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MedicationLifecycleTest {
    private val repository = FakeMedicationRepository()
    private val observeMedications = ObserveMedications(repository)
    private val observeMedication = ObserveMedication(repository)
    private val setArchived = SetMedicationArchived(repository)
    private val deleteMedication = DeleteMedication(repository)

    @Test
    fun `observe medications splits active and archived`() = runTest {
        repository.insert(medication(name = "Zinc"))
        repository.insert(medication(name = "aspirin"))
        repository.insert(medication(name = "Old", archived = true))

        observeMedications().test {
            val lists = awaitItem()
            assertEquals(listOf("aspirin", "Zinc"), lists.active.map { it.name })
            assertEquals(listOf("Old"), lists.archived.map { it.name })
        }
    }

    @Test
    fun `archive and unarchive move a medication between lists`() = runTest {
        val id = repository.insert(medication())

        setArchived(id, archived = true)
        observeMedications().test {
            val lists = awaitItem()
            assertEquals(emptyList<Long>(), lists.active.map { it.id })
            assertEquals(listOf(id), lists.archived.map { it.id })
        }

        setArchived(id, archived = false)
        assertEquals(false, repository.get(id)?.archived)
    }

    @Test
    fun `delete removes the medication and observers see null`() = runTest {
        val id = repository.insert(medication())

        observeMedication(id).test {
            assertEquals(id, awaitItem()?.id)
            deleteMedication(id)
            assertNull(awaitItem())
        }
    }
}
