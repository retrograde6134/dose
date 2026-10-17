package ca.alexlittle.dose.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/** Persisted by [name], so entries can be reordered but never renamed without a migration. */
internal enum class ScheduleType {
    DAILY,
    WEEKDAYS,
    INTERVAL,
    CYCLIC,
    PRN,
}

/**
 * Flat form of a schedule. Only the parameter columns for [type] are set:
 * WEEKDAYS uses [daysOfWeekMask] (bit `DayOfWeek.value - 1`) and [intervalCount] as N weeks,
 * INTERVAL uses [intervalCount] as N days, CYCLIC uses [cycleOnDays] and [cycleOffDays],
 * and PRN uses [minIntervalMinutes] (optional).
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicationId")],
)
internal data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val medicationId: Long,
    val type: ScheduleType,
    val daysOfWeekMask: Int?,
    val intervalCount: Int?,
    val cycleOnDays: Int?,
    val cycleOffDays: Int?,
    val minIntervalMinutes: Int?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val timesOfDay: List<LocalTime>,
    val doseQuantity: BigDecimal,
)
