package ca.alexlittle.dose.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.alexlittle.dose.data.repository.toEntity
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoseLogDaoTest {
    private lateinit var database: DoseDatabase
    private lateinit var medications: MedicationDao
    private lateinit var schedules: ScheduleDao
    private lateinit var dao: DoseLogDao
    private var medicationId = 0L
    private var scheduleId = 0L

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DoseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medications = database.medicationDao()
        schedules = database.scheduleDao()
        dao = database.doseLogDao()
        medicationId = medications.insert(medication())
        scheduleId = schedules.insert(schedule(medicationId).toEntity())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a log round-trips with fractional quantity and snooze time`() = runTest {
        val log = log(status = DoseStatus.SNOOZED_PENDING, quantity = "0.5", snoozedUntil = AT.plusSeconds(600))

        val id = dao.insert(log)

        assertEquals(log.copy(id = id), dao.get(scheduleId, AT))
    }

    @Test
    fun `the same dose cannot be inserted twice`() = runTest {
        dao.insert(log())

        assertThrows(SQLiteConstraintException::class.java) { runBlocking { dao.insert(log()) } }
    }

    @Test
    fun `as-needed logs without a dose identity can repeat`() = runTest {
        dao.insert(log(scheduleId = null, scheduledAt = null))
        dao.insert(log(scheduleId = null, scheduledAt = null))

        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM dose_logs").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
    }

    @Test
    fun `modify inserts once then updates the same row`() = runTest {
        val first = dao.modify(scheduleId, AT) { log(status = DoseStatus.SKIPPED) }
        val second = dao.modify(scheduleId, AT) { current -> current?.copy(status = DoseStatus.TAKEN, takenAt = AT) }

        assertEquals(first?.id, second?.id)
        assertEquals(DoseStatus.TAKEN, dao.get(scheduleId, AT)?.status)
        assertEquals(listOf(second), dao.scheduledBetween(AT, AT.plusSeconds(1)))
    }

    @Test
    fun `modify returning null leaves the row unchanged`() = runTest {
        val existing = dao.modify(scheduleId, AT) { log() }

        val result = dao.modify(scheduleId, AT) { null }

        assertEquals(existing, result)
        assertEquals(existing, dao.get(scheduleId, AT))
    }

    @Test
    fun `scheduledBetween is half-open and ordered`() = runTest {
        val later = AT.plusSeconds(3600)
        dao.insert(log(scheduledAt = later))
        dao.insert(log())
        dao.insert(log(scheduledAt = AT.plusSeconds(7200)))

        assertEquals(listOf(AT, later), dao.scheduledBetween(AT, AT.plusSeconds(7200)).map { it.scheduledAt })
    }

    @Test
    fun `snoozedPending lists only snoozed logs, earliest first`() = runTest {
        dao.insert(log(scheduledAt = AT, status = DoseStatus.SNOOZED_PENDING, snoozedUntil = AT.plusSeconds(900)))
        dao.insert(log(scheduledAt = AT.plusSeconds(1), status = DoseStatus.TAKEN))
        dao.insert(
            log(
                scheduledAt = AT.plusSeconds(2),
                status = DoseStatus.SNOOZED_PENDING,
                snoozedUntil = AT.plusSeconds(300),
            ),
        )

        assertEquals(listOf(AT.plusSeconds(300), AT.plusSeconds(900)), dao.snoozedPending().map { it.snoozedUntil })
    }

    @Test
    fun `deleting a schedule keeps its history without the schedule link`() = runTest {
        val id = dao.insert(log())

        schedules.delete(scheduleId)

        val kept = dao.scheduledBetween(AT, AT.plusSeconds(1)).single()
        assertEquals(id, kept.id)
        assertNull(kept.scheduleId)
    }

    @Test
    fun `deleting a medication deletes its history`() = runTest {
        dao.insert(log())

        medications.delete(medicationId)

        assertEquals(emptyList<DoseLogEntity>(), dao.scheduledBetween(AT, AT.plusSeconds(1)))
    }

    private fun log(
        scheduleId: Long? = this.scheduleId,
        scheduledAt: Instant? = AT,
        status: DoseStatus = DoseStatus.TAKEN,
        quantity: String = "1",
        snoozedUntil: Instant? = null,
    ) = DoseLogEntity(
        id = 0,
        medicationId = medicationId,
        scheduleId = scheduleId,
        scheduledAt = scheduledAt,
        takenAt = if (status == DoseStatus.TAKEN) AT else null,
        status = status,
        quantity = BigDecimal(quantity),
        note = null,
        snoozedUntil = snoozedUntil,
    )

    private fun schedule(medicationId: Long) = Schedule(
        medicationId = medicationId,
        recurrence = Recurrence.Daily,
        startDate = LocalDate.of(2026, 10, 16),
        endDate = null,
        timesOfDay = listOf(LocalTime.of(8, 0)),
        doseQuantity = BigDecimal.ONE,
    )

    private fun medication() = MedicationEntity(
        id = 0,
        name = "Metformin",
        strengthValue = null,
        strengthUnit = null,
        form = MedicationForm.TABLET,
        colorArgb = 0xFF1565C0.toInt(),
        notes = null,
        instructions = null,
        archived = false,
        createdAt = Instant.parse("2026-10-16T12:00:00Z"),
    )

    private companion object {
        val AT: Instant = Instant.parse("2026-10-16T12:00:00Z")
    }
}
