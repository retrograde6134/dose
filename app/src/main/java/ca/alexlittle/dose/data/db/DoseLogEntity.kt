package ca.alexlittle.dose.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ca.alexlittle.dose.domain.model.DoseStatus
import java.math.BigDecimal
import java.time.Instant

/**
 * The unique index on (scheduleId, scheduledAt) is what makes logging a dose twice impossible. As-needed
 * rows leave both null, and SQLite treats nulls as distinct, so any number of them can exist.
 */
@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("medicationId"),
        Index(value = ["scheduleId", "scheduledAt"], unique = true),
        Index("scheduledAt"),
    ],
)
internal data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val medicationId: Long,
    val scheduleId: Long?,
    val scheduledAt: Instant?,
    val takenAt: Instant?,
    val status: DoseStatus,
    val quantity: BigDecimal,
    val note: String?,
    val snoozedUntil: Instant?,
)
