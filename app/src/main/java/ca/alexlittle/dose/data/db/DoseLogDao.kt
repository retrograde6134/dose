package ca.alexlittle.dose.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.time.Instant

@Dao
internal interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE scheduleId = :scheduleId AND scheduledAt = :scheduledAt")
    suspend fun get(scheduleId: Long, scheduledAt: Instant): DoseLogEntity?

    /** Logs of scheduled doses due in `[from, until)`, oldest first. */
    @Query("SELECT * FROM dose_logs WHERE scheduledAt >= :from AND scheduledAt < :until ORDER BY scheduledAt, id")
    suspend fun scheduledBetween(from: Instant, until: Instant): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE status = 'SNOOZED_PENDING' ORDER BY snoozedUntil, id")
    suspend fun snoozedPending(): List<DoseLogEntity>

    @Insert
    suspend fun insert(log: DoseLogEntity): Long

    @Update
    suspend fun update(log: DoseLogEntity): Int

    /**
     * Reads the log for a dose and writes [transform]'s result in one transaction, so a tap on the
     * notification and a tap in the app cannot both insert. A null result leaves the row as it was.
     */
    @Transaction
    suspend fun modify(
        scheduleId: Long,
        scheduledAt: Instant,
        transform: (DoseLogEntity?) -> DoseLogEntity?,
    ): DoseLogEntity? {
        val current = get(scheduleId, scheduledAt)
        val next = transform(current) ?: return current
        return when (current) {
            null -> next.copy(id = insert(next.copy(id = 0)))
            else -> next.copy(id = current.id).also { update(it) }
        }
    }
}
