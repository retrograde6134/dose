package ca.alexlittle.dose.testing

import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.usecase.MedicationDraft
import ca.alexlittle.dose.domain.usecase.ScheduleDraft
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal val FIXED_INSTANT: Instant = Instant.parse("2026-10-16T12:00:00Z")

internal fun fixedClock(instant: Instant = FIXED_INSTANT): Clock = Clock.fixed(instant, ZoneId.of("America/Toronto"))

internal const val TEST_COLOR = 0xFF1565C0.toInt()

internal fun draft(
    id: Long = Medication.NEW_ID,
    name: String = "Metformin",
    strengthValue: String? = "500",
    strengthUnit: String? = "mg",
    form: MedicationForm = MedicationForm.TABLET,
    notes: String? = null,
    instructions: String? = null,
) = MedicationDraft(
    id = id,
    name = name,
    strengthValue = strengthValue?.let(::BigDecimal),
    strengthUnit = strengthUnit,
    form = form,
    colorArgb = TEST_COLOR,
    notes = notes,
    instructions = instructions,
)

internal fun medication(name: String = "Metformin", archived: Boolean = false, createdAt: Instant = FIXED_INSTANT) =
    Medication(
        name = name,
        strengthValue = BigDecimal("500"),
        strengthUnit = "mg",
        form = MedicationForm.TABLET,
        colorArgb = TEST_COLOR,
        notes = null,
        instructions = null,
        archived = archived,
        createdAt = createdAt,
    )

internal val START_DATE: LocalDate = LocalDate.of(2026, 10, 16)

internal fun scheduleDraft(
    medicationId: Long,
    id: Long = Schedule.NEW_ID,
    recurrence: Recurrence = Recurrence.Daily,
    startDate: LocalDate = START_DATE,
    endDate: LocalDate? = null,
    timesOfDay: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    doseQuantity: String = "1",
) = ScheduleDraft(
    id = id,
    medicationId = medicationId,
    recurrence = recurrence,
    startDate = startDate,
    endDate = endDate,
    timesOfDay = timesOfDay,
    doseQuantity = BigDecimal(doseQuantity),
)

internal fun schedule(
    medicationId: Long = 1,
    recurrence: Recurrence = Recurrence.Daily,
    startDate: LocalDate = START_DATE,
    endDate: LocalDate? = null,
    timesOfDay: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    doseQuantity: String = "1",
) = Schedule(
    medicationId = medicationId,
    recurrence = recurrence,
    startDate = startDate,
    endDate = endDate,
    timesOfDay = timesOfDay,
    doseQuantity = BigDecimal(doseQuantity),
)
