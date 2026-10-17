package ca.alexlittle.dose.domain.usecase

import app.cash.turbine.test
import ca.alexlittle.dose.testing.FakeScheduleRepository
import ca.alexlittle.dose.testing.schedule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleLifecycleTest {
    private val repository = FakeScheduleRepository()

    @Test
    fun `observeSchedules lists a medication's schedules in insertion order`() = runTest {
        val first = repository.insert(schedule(medicationId = 1))
        repository.insert(schedule(medicationId = 2))
        val second = repository.insert(schedule(medicationId = 1))

        ObserveSchedules(repository)(1).test {
            assertEquals(listOf(first, second), awaitItem().map { it.id })
        }
    }

    @Test
    fun `deleteSchedule removes it and observers see null`() = runTest {
        val id = repository.insert(schedule())

        ObserveSchedule(repository)(id).test {
            assertEquals(id, awaitItem()?.id)
            DeleteSchedule(repository)(id)
            assertNull(awaitItem())
        }
    }
}
