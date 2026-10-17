package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class ObserveSchedule @Inject constructor(private val repository: ScheduleRepository) {
    operator fun invoke(id: Long): Flow<Schedule?> = repository.observe(id)
}
