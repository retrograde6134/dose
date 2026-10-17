package ca.alexlittle.dose.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import ca.alexlittle.dose.domain.model.MedicationForm
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationDaoTest {
    private lateinit var database: DoseDatabase
    private lateinit var dao: MedicationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DoseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.medicationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and get round-trips every field`() = runTest {
        val id = dao.insert(medication(name = "Metformin", strength = BigDecimal("0.5"), unit = "mg"))

        val expected = medication(name = "Metformin", strength = BigDecimal("0.5"), unit = "mg").copy(id = id)
        assertEquals(expected, dao.get(id))
    }

    @Test
    fun `null strength round-trips`() = runTest {
        val id = dao.insert(medication(name = "Eye drops", strength = null, unit = null))

        val loaded = dao.get(id)
        assertNull(loaded?.strengthValue)
        assertNull(loaded?.strengthUnit)
    }

    @Test
    fun `active list excludes archived and sorts by name ignoring case`() = runTest {
        dao.insert(medication(name = "zinc"))
        dao.insert(medication(name = "Aspirin"))
        dao.insert(medication(name = "biotin"))
        dao.insert(medication(name = "Old med", archived = true))

        dao.observeActive().test {
            assertEquals(listOf("Aspirin", "biotin", "zinc"), awaitItem().map { it.name })
        }
    }

    @Test
    fun `archived list contains only archived medications`() = runTest {
        dao.insert(medication(name = "Current"))
        dao.insert(medication(name = "Old med", archived = true))

        dao.observeArchived().test {
            assertEquals(listOf("Old med"), awaitItem().map { it.name })
        }
    }

    @Test
    fun `update replaces fields and reports rows changed`() = runTest {
        val id = dao.insert(medication(name = "Metformin"))

        val changed = dao.update(medication(name = "Metformin XR", form = MedicationForm.CAPSULE).copy(id = id))

        assertEquals(1, changed)
        assertEquals("Metformin XR", dao.get(id)?.name)
        assertEquals(MedicationForm.CAPSULE, dao.get(id)?.form)
    }

    @Test
    fun `update of a missing row changes nothing`() = runTest {
        assertEquals(0, dao.update(medication(name = "Ghost").copy(id = 42)))
    }

    @Test
    fun `setArchived moves a medication between lists`() = runTest {
        val id = dao.insert(medication(name = "Metformin"))

        dao.observeActive().test {
            assertEquals(listOf(id), awaitItem().map { it.id })
            dao.setArchived(id, archived = true)
            assertEquals(emptyList<Long>(), awaitItem().map { it.id })
            dao.setArchived(id, archived = false)
            assertEquals(listOf(id), awaitItem().map { it.id })
        }
    }

    @Test
    fun `delete removes the row and observers see null`() = runTest {
        val id = dao.insert(medication(name = "Metformin"))

        dao.observe(id).test {
            assertEquals("Metformin", awaitItem()?.name)
            dao.delete(id)
            assertNull(awaitItem())
        }
        assertNull(dao.get(id))
    }

    private fun medication(
        name: String,
        strength: BigDecimal? = BigDecimal("500"),
        unit: String? = "mg",
        form: MedicationForm = MedicationForm.TABLET,
        archived: Boolean = false,
    ) = MedicationEntity(
        id = 0,
        name = name,
        strengthValue = strength,
        strengthUnit = unit,
        form = form,
        colorArgb = 0xFF1565C0.toInt(),
        notes = "notes",
        instructions = "with food",
        archived = archived,
        createdAt = Instant.parse("2026-10-16T12:00:00Z"),
    )
}
