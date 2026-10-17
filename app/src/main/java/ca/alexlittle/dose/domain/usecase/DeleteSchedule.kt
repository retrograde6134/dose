package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.repository.ScheduleRepository
import javax.inject.Inject

/** Once reminders exist, this is also where the schedule's alarms get cancelled. */
internal class DeleteSchedule @Inject constructor(private val repository: ScheduleRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
