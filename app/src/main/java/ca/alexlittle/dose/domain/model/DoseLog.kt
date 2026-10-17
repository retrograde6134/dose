package ca.alexlittle.dose.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Identity of one scheduled dose. Derived from the schedule, never stored ahead of time, and stable
 * across re-scheduling so the same dose can never be logged twice.
 */
internal data class DoseKey(val scheduleId: Long, val scheduledAt: Instant)

/**
 * Something that actually happened to a dose. Future doses are never stored; they come from the schedule.
 *
 * [scheduleId] and [scheduledAt] are null for as-needed doses. [scheduleId] also becomes null if the
 * schedule is deleted, so history outlives it. [snoozedUntil] is set only while [status] is
 * [DoseStatus.SNOOZED_PENDING], and is stored so a snooze survives a reboot.
 */
internal data class DoseLog(
    val id: Long = NEW_ID,
    val medicationId: Long,
    val scheduleId: Long?,
    val scheduledAt: Instant?,
    val takenAt: Instant?,
    val status: DoseStatus,
    val quantity: BigDecimal,
    val note: String?,
    val snoozedUntil: Instant?,
) {
    val key: DoseKey?
        get() = if (scheduleId != null && scheduledAt != null) DoseKey(scheduleId, scheduledAt) else null

    companion object {
        /** Id of a log entry that has not been saved yet. */
        const val NEW_ID = 0L
    }
}

/** Persisted by [name], so entries can be reordered but never renamed without a migration. */
internal enum class DoseStatus {
    TAKEN,
    SKIPPED,
    MISSED,
    SNOOZED_PENDING,
}
