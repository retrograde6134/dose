package ca.alexlittle.dose.ui.schedules.edit

import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val QUANTITY_ERRORS = setOf(ScheduleEditError.QUANTITY_INVALID, ScheduleEditError.QUANTITY_NOT_POSITIVE)

/**
 * Applies a field edit. Each edit clears the errors it could have fixed. Save and Delete are handled by
 * the ViewModel and leave the state unchanged here.
 */
internal fun ScheduleEditUiState.reduce(action: ScheduleEditAction): ScheduleEditUiState = when (action) {
    is ScheduleEditAction.KindChange -> copy(
        kind = action.kind,
        errors = errors - ScheduleEditError.NO_WEEKDAYS - ScheduleEditError.NO_TIMES,
    )

    is ScheduleEditAction.WeekdayToggle -> copy(
        weekdays = if (action.day in weekdays) weekdays - action.day else weekdays + action.day,
        errors = errors - ScheduleEditError.NO_WEEKDAYS,
    )

    is ScheduleEditAction.EveryNWeeksChange,
    is ScheduleEditAction.EveryNDaysChange,
    is ScheduleEditAction.CycleOnDaysChange,
    is ScheduleEditAction.CycleOffDaysChange,
    is ScheduleEditAction.MinIntervalChange,
    -> reduceCount(action)

    is ScheduleEditAction.AddTime -> copy(
        times = withTimes(times + action.time),
        errors =
            errors - ScheduleEditError.NO_TIMES,
    )

    is ScheduleEditAction.ChangeTime -> copy(times = withTimes(times - action.old + action.new))

    is ScheduleEditAction.RemoveTime -> copy(times = times - action.time)

    is ScheduleEditAction.QuantityChange -> copy(quantity = action.text, errors = errors - QUANTITY_ERRORS)

    is ScheduleEditAction.StartDateChange ->
        copy(startDate = action.date, errors = errors - ScheduleEditError.END_BEFORE_START)

    is ScheduleEditAction.EndDateChange ->
        copy(endDate = action.date, errors = errors - ScheduleEditError.END_BEFORE_START)

    ScheduleEditAction.Save, ScheduleEditAction.Delete -> this
}

// Steppers already respect the limits; clamping again keeps a stray value from ever reaching the domain.
private fun ScheduleEditUiState.reduceCount(action: ScheduleEditAction): ScheduleEditUiState = when (action) {
    is ScheduleEditAction.EveryNWeeksChange -> copy(everyNWeeks = action.weeks.coerceIn(ScheduleLimits.WEEKS))

    is ScheduleEditAction.EveryNDaysChange -> copy(everyNDays = action.days.coerceIn(ScheduleLimits.DAYS))

    is ScheduleEditAction.CycleOnDaysChange -> copy(cycleOnDays = action.days.coerceIn(ScheduleLimits.DAYS))

    is ScheduleEditAction.CycleOffDaysChange -> copy(cycleOffDays = action.days.coerceIn(ScheduleLimits.DAYS))

    is ScheduleEditAction.MinIntervalChange ->
        copy(minIntervalMinutes = action.minutes.coerceIn(ScheduleLimits.MIN_INTERVAL_MINUTES))

    else -> this
}

// Sorted and distinct, so adding a time that already exists changes nothing.
private fun withTimes(times: List<LocalTime>) = times.map { it.truncatedTo(ChronoUnit.MINUTES) }.distinct().sorted()
