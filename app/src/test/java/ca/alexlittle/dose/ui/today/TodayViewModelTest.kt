package ca.alexlittle.dose.ui.today

import app.cash.turbine.test
import ca.alexlittle.dose.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TodayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `emits empty state when nothing is scheduled`() = runTest {
        val viewModel = TodayViewModel()

        viewModel.uiState.test {
            assertEquals(TodayUiState.Empty, awaitItem())
        }
    }
}
