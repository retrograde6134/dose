package ca.alexlittle.dose.ui.schedules.edit

import app.cash.turbine.test
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.usecase.DeleteSchedule
import ca.alexlittle.dose.domain.usecase.ObserveMedication
import ca.alexlittle.dose.domain.usecase.ObserveSchedule
import ca.alexlittle.dose.domain.usecase.SaveSchedule
import ca.alexlittle.dose.testing.FakeMedicationRepository
import ca.alexlittle.dose.testing.FakeScheduleRepository
import ca.alexlittle.dose.testing.fixedClock
import ca.alexlittle.dose.testing.medication
import ca.alexlittle.dose.testing.schedule
import ca.alexlittle.dose.util.MainDispatcherRule
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScheduleEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val medications = FakeMedicationRepository()
    private val schedules = FakeScheduleRepository()

    private fun viewModel(medicationId: Long, scheduleId: Long = Schedule.NEW_ID, locale: Locale = Locale.CANADA) =
        ScheduleEditViewModel(
            medicationId = medicationId,
            scheduleId = scheduleId,
            observeMedication = ObserveMedication(medications),
            observeSchedule = ObserveSchedule(schedules),
            saveSchedule = SaveSchedule(schedules, medications),
            deleteSchedule = DeleteSchedule(schedules),
            locale = locale,
            clock = fixedClock(),
        )

    private suspend fun insertMedication() =
        medications.insert(medication(name = "Ventolin").copy(form = MedicationForm.INHALER))

    @Test
    fun `new schedule loads the medication and starts today with that weekday chosen`() = runTest {
        val state = viewModel(insertMedication()).uiState.value

        assertTrue(state.isNew)
        assertFalse(state.loading)
        assertEquals("Ventolin", state.medicationName)
        assertEquals(MedicationForm.INHALER, state.form)
        assertEquals(LocalDate.of(2026, 10, 16), state.startDate)
        assertEquals(setOf(DayOfWeek.FRIDAY), state.weekdays)
    }

    @Test
    fun `missing medication or schedule closes the screen`() = runTest {
        viewModel(medicationId = 42).events.test { assertEquals(ScheduleEditEvent.Done, awaitItem()) }
        viewModel(insertMedication(), scheduleId = 42).events.test { assertEquals(ScheduleEditEvent.Done, awaitItem()) }
    }

    @Test
    fun `existing schedule loads into the form using the locale separator`() = runTest {
        val medicationId = insertMedication()
        val id = schedules.insert(
            schedule(
                medicationId = medicationId,
                recurrence = Recurrence.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), everyNWeeks = 2),
                endDate = LocalDate.of(2026, 12, 31),
                timesOfDay = listOf(LocalTime.of(9, 0)),
                doseQuantity = "0.5",
            ),
        )

        val state = viewModel(medicationId, id, Locale.CANADA_FRENCH).uiState.value

        assertFalse(state.isNew)
        assertEquals(ScheduleKind.WEEKDAYS, state.kind)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), state.weekdays)
        assertEquals(2, state.everyNWeeks)
        assertEquals(listOf(LocalTime.of(9, 0)), state.times)
        assertEquals("0,5", state.quantity)
        assertEquals(LocalDate.of(2026, 12, 31), state.endDate)
    }

    @Test
    fun `as-needed schedule loads with a default time ready for other kinds`() = runTest {
        val medicationId = insertMedication()
        val id = schedules.insert(
            schedule(medicationId, recurrence = Recurrence.AsNeeded(240), timesOfDay = emptyList()),
        )

        val state = viewModel(medicationId, id).uiState.value

        assertEquals(ScheduleKind.AS_NEEDED, state.kind)
        assertEquals(240, state.minIntervalMinutes)
        assertEquals(listOf(DEFAULT_FIRST_TIME), state.times)
    }

    @Test
    fun `valid save inserts the selected kind and closes the screen`() = runTest {
        val medicationId = insertMedication()
        val viewModel = viewModel(medicationId)
        viewModel.onAction(ScheduleEditAction.EveryNDaysChange(3))
        viewModel.onAction(ScheduleEditAction.KindChange(ScheduleKind.INTERVAL))
        viewModel.onAction(ScheduleEditAction.KindChange(ScheduleKind.CYCLIC))
        viewModel.onAction(ScheduleEditAction.AddTime(LocalTime.of(20, 0)))
        viewModel.onAction(ScheduleEditAction.QuantityChange("1.5"))

        viewModel.events.test {
            viewModel.onAction(ScheduleEditAction.Save)
            assertEquals(ScheduleEditEvent.Done, awaitItem())
        }
        val saved = schedules.all.single()
        assertEquals(medicationId, saved.medicationId)
        assertEquals(Recurrence.Cyclic(onDays = 21, offDays = 7), saved.recurrence)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), saved.timesOfDay)
        assertEquals(BigDecimal("1.5"), saved.doseQuantity)
        assertEquals(LocalDate.of(2026, 10, 16), saved.startDate)
    }

    @Test
    fun `as-needed saves without times and with no minimum at zero`() = runTest {
        val medicationId = insertMedication()
        val viewModel = viewModel(medicationId)
        viewModel.onAction(ScheduleEditAction.KindChange(ScheduleKind.AS_NEEDED))
        viewModel.onAction(ScheduleEditAction.Save)

        val second = viewModel(medicationId)
        second.onAction(ScheduleEditAction.KindChange(ScheduleKind.AS_NEEDED))
        second.onAction(ScheduleEditAction.MinIntervalChange(240))
        second.onAction(ScheduleEditAction.Save)

        assertEquals(listOf(Recurrence.AsNeeded(null), Recurrence.AsNeeded(240)), schedules.all.map { it.recurrence })
        assertTrue(schedules.all.all { it.timesOfDay.isEmpty() })
    }

    @Test
    fun `domain errors show on the form and editing clears them`() = runTest {
        val viewModel = viewModel(insertMedication())
        viewModel.onAction(ScheduleEditAction.KindChange(ScheduleKind.WEEKDAYS))
        viewModel.onAction(ScheduleEditAction.WeekdayToggle(DayOfWeek.FRIDAY))
        viewModel.onAction(ScheduleEditAction.RemoveTime(DEFAULT_FIRST_TIME))
        viewModel.onAction(ScheduleEditAction.QuantityChange("0"))
        viewModel.onAction(ScheduleEditAction.EndDateChange(LocalDate.of(2026, 10, 1)))

        viewModel.onAction(ScheduleEditAction.Save)

        val state = viewModel.uiState.value
        assertEquals(
            setOf(
                ScheduleEditError.NO_WEEKDAYS,
                ScheduleEditError.NO_TIMES,
                ScheduleEditError.QUANTITY_NOT_POSITIVE,
                ScheduleEditError.END_BEFORE_START,
            ),
            state.errors,
        )
        assertFalse(state.saving)
        assertTrue(schedules.all.isEmpty())

        viewModel.onAction(ScheduleEditAction.WeekdayToggle(DayOfWeek.MONDAY))
        viewModel.onAction(ScheduleEditAction.AddTime(LocalTime.of(9, 0)))
        viewModel.onAction(ScheduleEditAction.QuantityChange("1"))
        viewModel.onAction(ScheduleEditAction.EndDateChange(null))
        assertEquals(emptySet<ScheduleEditError>(), viewModel.uiState.value.errors)
    }

    @Test
    fun `unparseable quantity is reported without saving`() = runTest {
        val viewModel = viewModel(insertMedication())
        viewModel.onAction(ScheduleEditAction.QuantityChange("half"))

        viewModel.onAction(ScheduleEditAction.Save)

        assertEquals(setOf(ScheduleEditError.QUANTITY_INVALID), viewModel.uiState.value.errors)
        assertTrue(schedules.all.isEmpty())
    }

    @Test
    fun `second save tap while saving does not insert twice`() = runTest {
        val viewModel = viewModel(insertMedication())

        viewModel.onAction(ScheduleEditAction.Save)
        viewModel.onAction(ScheduleEditAction.Save)

        assertEquals(1, schedules.all.size)
    }

    @Test
    fun `times stay sorted and distinct`() = runTest {
        val viewModel = viewModel(insertMedication())

        viewModel.onAction(ScheduleEditAction.AddTime(LocalTime.of(20, 0)))
        viewModel.onAction(ScheduleEditAction.AddTime(LocalTime.of(12, 0)))
        viewModel.onAction(ScheduleEditAction.AddTime(LocalTime.of(12, 0)))
        viewModel.onAction(ScheduleEditAction.ChangeTime(LocalTime.of(20, 0), LocalTime.of(6, 30)))

        assertEquals(
            listOf(LocalTime.of(6, 30), LocalTime.of(8, 0), LocalTime.of(12, 0)),
            viewModel.uiState.value.times,
        )
    }

    @Test
    fun `counts are clamped to their limits`() = runTest {
        val viewModel = viewModel(insertMedication())

        viewModel.onAction(ScheduleEditAction.EveryNWeeksChange(0))
        viewModel.onAction(ScheduleEditAction.EveryNDaysChange(1000))
        viewModel.onAction(ScheduleEditAction.MinIntervalChange(-60))

        val state = viewModel.uiState.value
        assertEquals(1, state.everyNWeeks)
        assertEquals(365, state.everyNDays)
        assertEquals(0, state.minIntervalMinutes)
    }

    @Test
    fun `delete removes the schedule and closes the screen`() = runTest {
        val medicationId = insertMedication()
        val id = schedules.insert(schedule(medicationId))
        val viewModel = viewModel(medicationId, id)

        viewModel.events.test {
            viewModel.onAction(ScheduleEditAction.Delete)
            assertEquals(ScheduleEditEvent.Done, awaitItem())
        }
        assertTrue(schedules.all.isEmpty())
    }
}
