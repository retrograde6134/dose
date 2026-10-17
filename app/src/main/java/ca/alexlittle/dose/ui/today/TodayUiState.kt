package ca.alexlittle.dose.ui.today

/** [hasReminders] is whether any active schedule sends reminders. */
internal data class TodayUiState(val loading: Boolean = true, val hasReminders: Boolean = false)

/** Something stopping reminders from arriving on time, most serious first. */
internal enum class ReminderProblem {
    NOTIFICATIONS_OFF,
    ALARMS_INEXACT,
}
