package ca.alexlittle.dose.ui.today

import app.cash.turbine.test
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.usecase.ObserveHasReminders
import ca.alexlittle.dose.testing.FakeScheduleRepository
import ca.alexlittle.dose.testing.schedule
import ca.alexlittle.dose.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TodayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val schedules = FakeScheduleRepository()

    @Test
    fun `emits empty state when nothing is scheduled`() = runTest {
        TodayViewModel(ObserveHasReminders(schedules)).uiState.test {
            assertEquals(TodayUiState(loading = false, hasReminders = false), awaitItem())
        }
    }

    @Test
    fun `knows when a schedule sends reminders, but not for as-needed only`() = runTest {
        schedules.insert(schedule(recurrence = Recurrence.AsNeeded(null), timesOfDay = emptyList()))

        TodayViewModel(ObserveHasReminders(schedules)).uiState.test {
            assertEquals(TodayUiState(loading = false, hasReminders = false), awaitItem())
            schedules.insert(schedule())
            assertEquals(TodayUiState(loading = false, hasReminders = true), awaitItem())
        }
    }
}
