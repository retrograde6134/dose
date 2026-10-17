package ca.alexlittle.dose.ui.navigation

import ca.alexlittle.dose.domain.model.Schedule
import kotlinx.serialization.Serializable

/** [scheduleId] is [Schedule.NEW_ID] when adding a schedule to [medicationId]. */
@Serializable
internal data class ScheduleEditDestination(val medicationId: Long, val scheduleId: Long = Schedule.NEW_ID)
