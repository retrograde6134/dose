package ca.alexlittle.dose.ui.schedules.edit

import ca.alexlittle.dose.domain.model.MedicationForm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

internal val DEFAULT_FIRST_TIME: LocalTime = LocalTime.of(8, 0)

internal enum class ScheduleKind {
    DAILY,
    WEEKDAYS,
    INTERVAL,
    CYCLIC,
    AS_NEEDED,
}

/**
 * Every kind's fields are kept while the user switches between kinds, so trying "Every few days"
 * and coming back to "Days of the week" doesn't lose the chosen days. Only [kind]'s fields are saved.
 * [quantity] holds the raw text typed; parsing happens on save.
 */
internal data class ScheduleEditUiState(
    val isNew: Boolean,
    val startDate: LocalDate,
    val loading: Boolean = true,
    val medicationName: String = "",
    val form: MedicationForm = MedicationForm.TABLET,
    val kind: ScheduleKind = ScheduleKind.DAILY,
    val weekdays: Set<DayOfWeek> = setOf(startDate.dayOfWeek),
    val everyNWeeks: Int = 1,
    val everyNDays: Int = 2,
    val cycleOnDays: Int = 21,
    val cycleOffDays: Int = 7,
    /** 0 means no minimum. */
    val minIntervalMinutes: Int = 0,
    val times: List<LocalTime> = listOf(DEFAULT_FIRST_TIME),
    val quantity: String = "1",
    val endDate: LocalDate? = null,
    val errors: Set<ScheduleEditError> = emptySet(),
    val saving: Boolean = false,
)

internal enum class ScheduleEditError {
    NO_TIMES,
    NO_WEEKDAYS,
    QUANTITY_INVALID,
    QUANTITY_NOT_POSITIVE,
    END_BEFORE_START,
}

internal sealed interface ScheduleEditAction {
    data class KindChange(val kind: ScheduleKind) : ScheduleEditAction

    data class WeekdayToggle(val day: DayOfWeek) : ScheduleEditAction

    data class EveryNWeeksChange(val weeks: Int) : ScheduleEditAction

    data class EveryNDaysChange(val days: Int) : ScheduleEditAction

    data class CycleOnDaysChange(val days: Int) : ScheduleEditAction

    data class CycleOffDaysChange(val days: Int) : ScheduleEditAction

    data class MinIntervalChange(val minutes: Int) : ScheduleEditAction

    data class AddTime(val time: LocalTime) : ScheduleEditAction

    data class ChangeTime(val old: LocalTime, val new: LocalTime) : ScheduleEditAction

    data class RemoveTime(val time: LocalTime) : ScheduleEditAction

    data class QuantityChange(val text: String) : ScheduleEditAction

    data class StartDateChange(val date: LocalDate) : ScheduleEditAction

    data class EndDateChange(val date: LocalDate?) : ScheduleEditAction

    data object Save : ScheduleEditAction

    data object Delete : ScheduleEditAction
}

internal sealed interface ScheduleEditEvent {
    /** Saved without reminders, deleted, or the schedule or medication no longer exists. The screen closes. */
    data object Done : ScheduleEditEvent

    /** Saved a schedule that sends reminders: ask for notification permission if needed, then close. */
    data object SavedWithReminders : ScheduleEditEvent
}

/** Limits for the steppers, which keep every count valid so the domain never sees zero or negatives. */
internal object ScheduleLimits {
    val WEEKS = 1..52
    val DAYS = 1..365
    val MIN_INTERVAL_MINUTES = 0..72 * 60
    const val MIN_INTERVAL_STEP = 60
}
