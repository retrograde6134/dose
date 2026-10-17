package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Whether any active schedule sends reminders, so a problem with notifications is worth mentioning. */
internal class ObserveHasReminders @Inject constructor(private val schedules: ScheduleRepository) {
    operator fun invoke(): Flow<Boolean> = schedules.observeActive()
        .map { list -> list.any { it.recurrence !is Recurrence.AsNeeded } }
        .distinctUntilChanged()
}
