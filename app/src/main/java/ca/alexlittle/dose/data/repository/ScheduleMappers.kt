package ca.alexlittle.dose.data.repository

import ca.alexlittle.dose.data.db.ScheduleEntity
import ca.alexlittle.dose.data.db.ScheduleType
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import java.time.DayOfWeek

// A row missing its type's parameters can only come from a bug or a bad import. Failing loudly
// beats guessing a recurrence and reminding the user on the wrong days.
internal fun ScheduleEntity.toDomain() = Schedule(
    id = id,
    medicationId = medicationId,
    recurrence = recurrence(),
    startDate = startDate,
    endDate = endDate,
    timesOfDay = timesOfDay,
    doseQuantity = doseQuantity,
)

private fun ScheduleEntity.recurrence(): Recurrence = when (type) {
    ScheduleType.DAILY -> Recurrence.Daily

    ScheduleType.WEEKDAYS -> Recurrence.Weekly(
        days = daysFromMask(required(daysOfWeekMask, "daysOfWeekMask")),
        everyNWeeks = required(intervalCount, "intervalCount"),
    )

    ScheduleType.INTERVAL -> Recurrence.EveryNDays(required(intervalCount, "intervalCount"))

    ScheduleType.CYCLIC -> Recurrence.Cyclic(
        onDays = required(cycleOnDays, "cycleOnDays"),
        offDays = required(cycleOffDays, "cycleOffDays"),
    )

    ScheduleType.PRN -> Recurrence.AsNeeded(minIntervalMinutes)
}

private fun ScheduleEntity.required(value: Int?, column: String): Int =
    checkNotNull(value) { "Schedule $id of type $type has no $column" }

internal fun Schedule.toEntity(): ScheduleEntity {
    val base = ScheduleEntity(
        id = id,
        medicationId = medicationId,
        type = ScheduleType.DAILY,
        daysOfWeekMask = null,
        intervalCount = null,
        cycleOnDays = null,
        cycleOffDays = null,
        minIntervalMinutes = null,
        startDate = startDate,
        endDate = endDate,
        timesOfDay = timesOfDay,
        doseQuantity = doseQuantity,
    )
    return when (val r = recurrence) {
        Recurrence.Daily -> base

        is Recurrence.Weekly -> base.copy(
            type = ScheduleType.WEEKDAYS,
            daysOfWeekMask = maskFromDays(r.days),
            intervalCount = r.everyNWeeks,
        )

        is Recurrence.EveryNDays -> base.copy(type = ScheduleType.INTERVAL, intervalCount = r.n)

        is Recurrence.Cyclic -> base.copy(type = ScheduleType.CYCLIC, cycleOnDays = r.onDays, cycleOffDays = r.offDays)

        is Recurrence.AsNeeded -> base.copy(type = ScheduleType.PRN, minIntervalMinutes = r.minIntervalMinutes)
    }
}

internal fun maskFromDays(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

internal fun daysFromMask(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(mutableSetOf()) { mask and (1 shl (it.value - 1)) != 0 }
