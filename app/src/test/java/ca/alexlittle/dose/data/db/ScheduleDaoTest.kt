package ca.alexlittle.dose.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import ca.alexlittle.dose.data.repository.toDomain
import ca.alexlittle.dose.data.repository.toEntity
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleDaoTest {
    private lateinit var database: DoseDatabase
    private lateinit var medications: MedicationDao
    private lateinit var dao: ScheduleDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DoseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medications = database.medicationDao()
        dao = database.scheduleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `every recurrence type round-trips through the database`() = runTest {
        val medicationId = medications.insert(medication("Metformin"))
        val recurrences = listOf(
            Recurrence.Daily,
            Recurrence.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY), everyNWeeks = 2),
            Recurrence.EveryNDays(3),
            Recurrence.Cyclic(onDays = 21, offDays = 7),
            Recurrence.AsNeeded(minIntervalMinutes = 240),
            Recurrence.AsNeeded(minIntervalMinutes = null),
        )

        for (recurrence in recurrences) {
            val times = if (recurrence is Recurrence.AsNeeded) emptyList() else TIMES
            val schedule = schedule(medicationId, recurrence, times)
            val id = dao.insert(schedule.toEntity())

            assertEquals(schedule.copy(id = id), dao.get(id)?.toDomain())
        }
    }

    @Test
    fun `fractional quantity and end date survive exactly`() = runTest {
        val medicationId = medications.insert(medication("Metformin"))
        val schedule = schedule(medicationId).copy(
            doseQuantity = BigDecimal("0.5"),
            endDate = LocalDate.of(2027, 2, 28),
        )

        val loaded = dao.get(dao.insert(schedule.toEntity()))?.toDomain()

        assertEquals(BigDecimal("0.5"), loaded?.doseQuantity)
        assertEquals(LocalDate.of(2027, 2, 28), loaded?.endDate)
    }

    @Test
    fun `deleting a medication deletes its schedules`() = runTest {
        val medicationId = medications.insert(medication("Metformin"))
        val id = dao.insert(schedule(medicationId).toEntity())

        medications.delete(medicationId)

        assertNull(dao.get(id))
    }

    @Test
    fun `observeForMedication lists only that medication's schedules`() = runTest {
        val first = medications.insert(medication("Metformin"))
        val second = medications.insert(medication("Aspirin"))
        val a = dao.insert(schedule(first).toEntity())
        dao.insert(schedule(second).toEntity())
        val b = dao.insert(schedule(first).toEntity())

        dao.observeForMedication(first).test {
            assertEquals(listOf(a, b), awaitItem().map { it.id })
        }
    }

    @Test
    fun `observeActive excludes schedules of archived medications`() = runTest {
        val active = medications.insert(medication("Metformin"))
        val archived = medications.insert(medication("Old med", archived = true))
        val kept = dao.insert(schedule(active).toEntity())
        dao.insert(schedule(archived).toEntity())

        dao.observeActive().test {
            assertEquals(listOf(kept), awaitItem().map { it.id })
            medications.setArchived(active, archived = true)
            assertEquals(emptyList<Long>(), awaitItem().map { it.id })
        }
    }

    @Test
    fun `update replaces fields and reports rows changed`() = runTest {
        val medicationId = medications.insert(medication("Metformin"))
        val id = dao.insert(schedule(medicationId).toEntity())

        val changed = dao.update(schedule(medicationId, Recurrence.EveryNDays(2)).copy(id = id).toEntity())

        assertEquals(1, changed)
        assertEquals(Recurrence.EveryNDays(2), dao.get(id)?.toDomain()?.recurrence)
        assertEquals(0, dao.update(schedule(medicationId).copy(id = 999).toEntity()))
    }

    @Test
    fun `delete removes the row and observers see null`() = runTest {
        val medicationId = medications.insert(medication("Metformin"))
        val id = dao.insert(schedule(medicationId).toEntity())

        dao.observe(id).test {
            assertEquals(id, awaitItem()?.id)
            dao.delete(id)
            assertNull(awaitItem())
        }
    }

    private fun schedule(
        medicationId: Long,
        recurrence: Recurrence = Recurrence.Daily,
        times: List<LocalTime> = TIMES,
    ) = Schedule(
        medicationId = medicationId,
        recurrence = recurrence,
        startDate = LocalDate.of(2026, 10, 16),
        endDate = null,
        timesOfDay = times,
        doseQuantity = BigDecimal.ONE,
    )

    private fun medication(name: String, archived: Boolean = false) = MedicationEntity(
        id = 0,
        name = name,
        strengthValue = null,
        strengthUnit = null,
        form = MedicationForm.TABLET,
        colorArgb = 0xFF1565C0.toInt(),
        notes = null,
        instructions = null,
        archived = archived,
        createdAt = Instant.parse("2026-10-16T12:00:00Z"),
    )

    private companion object {
        val TIMES = listOf(LocalTime.of(8, 0), LocalTime.of(20, 30))
    }
}
