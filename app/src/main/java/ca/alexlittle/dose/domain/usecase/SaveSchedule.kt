package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.repository.MedicationRepository
import ca.alexlittle.dose.domain.repository.ScheduleRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** What the user entered on the schedule editor. [id] is [Schedule.NEW_ID] when adding. */
internal data class ScheduleDraft(
    val id: Long = Schedule.NEW_ID,
    val medicationId: Long,
    val recurrence: Recurrence,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val timesOfDay: List<LocalTime>,
    val doseQuantity: BigDecimal,
)

internal enum class ScheduleError {
    NO_TIMES,
    QUANTITY_NOT_POSITIVE,
    END_BEFORE_START,
    NO_WEEKDAYS,
    INTERVAL_NOT_POSITIVE,
    CYCLE_NOT_POSITIVE,
    MIN_INTERVAL_NEGATIVE,
}

internal sealed interface SaveScheduleResult {
    data class Saved(val id: Long) : SaveScheduleResult

    data class Invalid(val errors: Set<ScheduleError>) : SaveScheduleResult

    /** The schedule being edited, or its medication, was deleted in the meantime. */
    data object NotFound : SaveScheduleResult
}

internal class SaveSchedule @Inject constructor(
    private val schedules: ScheduleRepository,
    private val medications: MedicationRepository,
) {
    suspend operator fun invoke(draft: ScheduleDraft): SaveScheduleResult {
        val errors = validate(draft)
        return when {
            errors.isNotEmpty() -> SaveScheduleResult.Invalid(errors)
            medications.get(draft.medicationId) == null -> SaveScheduleResult.NotFound
            draft.id == Schedule.NEW_ID -> insert(draft.toSchedule())
            else -> update(draft.toSchedule())
        }
    }

    private suspend fun insert(schedule: Schedule): SaveScheduleResult =
        SaveScheduleResult.Saved(schedules.insert(schedule))

    private suspend fun update(schedule: Schedule): SaveScheduleResult {
        // A schedule never moves to another medication; its dose history stays with the original.
        val existing = schedules.get(schedule.id)
        val saved = existing != null && schedules.update(schedule.copy(medicationId = existing.medicationId))
        return if (saved) SaveScheduleResult.Saved(schedule.id) else SaveScheduleResult.NotFound
    }
}

private fun validate(draft: ScheduleDraft): Set<ScheduleError> = buildSet {
    if (draft.recurrence !is Recurrence.AsNeeded && draft.timesOfDay.isEmpty()) add(ScheduleError.NO_TIMES)
    if (draft.doseQuantity.signum() <= 0) add(ScheduleError.QUANTITY_NOT_POSITIVE)
    if (draft.endDate != null && draft.endDate < draft.startDate) add(ScheduleError.END_BEFORE_START)
    addAll(recurrenceErrors(draft.recurrence))
}

private fun recurrenceErrors(recurrence: Recurrence): Set<ScheduleError> = when (recurrence) {
    Recurrence.Daily -> emptySet()

    is Recurrence.Weekly -> setOfNotNull(
        ScheduleError.NO_WEEKDAYS.takeIf { recurrence.days.isEmpty() },
        ScheduleError.INTERVAL_NOT_POSITIVE.takeIf { recurrence.everyNWeeks < 1 },
    )

    is Recurrence.EveryNDays -> setOfNotNull(ScheduleError.INTERVAL_NOT_POSITIVE.takeIf { recurrence.n < 1 })

    is Recurrence.Cyclic ->
        setOfNotNull(ScheduleError.CYCLE_NOT_POSITIVE.takeIf { recurrence.onDays < 1 || recurrence.offDays < 1 })

    is Recurrence.AsNeeded ->
        setOfNotNull(ScheduleError.MIN_INTERVAL_NEGATIVE.takeIf { (recurrence.minIntervalMinutes ?: 0) < 0 })
}

private fun ScheduleDraft.toSchedule() = Schedule(
    id = id,
    medicationId = medicationId,
    recurrence = recurrence.normalized(),
    startDate = startDate,
    endDate = endDate,
    timesOfDay = normalizedTimes(),
    doseQuantity = doseQuantity.normalized(),
)

// A zero minimum interval means no minimum.
private fun Recurrence.normalized(): Recurrence =
    if (this is Recurrence.AsNeeded && minIntervalMinutes == 0) Recurrence.AsNeeded(null) else this

// As-needed doses have no reminder times. Seconds are dropped so 08:00:30 and 08:00 cannot both exist.
private fun ScheduleDraft.normalizedTimes(): List<LocalTime> = if (recurrence is Recurrence.AsNeeded) {
    emptyList()
} else {
    timesOfDay.map { it.truncatedTo(ChronoUnit.MINUTES) }.distinct().sorted()
}
