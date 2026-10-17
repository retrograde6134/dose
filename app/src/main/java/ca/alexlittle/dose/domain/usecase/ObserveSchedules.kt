package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** A medication's schedules in the order they were added. */
internal class ObserveSchedules @Inject constructor(private val repository: ScheduleRepository) {
    operator fun invoke(medicationId: Long): Flow<List<Schedule>> = repository.observeForMedication(medicationId)
}
