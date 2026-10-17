package ca.alexlittle.dose.domain.repository

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseLog
import java.time.Instant

internal interface DoseLogRepository {
    suspend fun get(key: DoseKey): DoseLog?

    /** Logs of scheduled doses due in `[from, until)`, oldest first. */
    suspend fun scheduledBetween(from: Instant, until: Instant): List<DoseLog>

    /** Snoozed doses still waiting for their reminder, earliest first. */
    suspend fun snoozedPending(): List<DoseLog>

    /**
     * Atomically replaces the log for [key] with [transform] applied to the current one. A null result
     * leaves it unchanged. The written key and id always come from [key] and the existing row.
     * Returns the log as it now stands.
     */
    suspend fun modify(key: DoseKey, transform: (DoseLog?) -> DoseLog?): DoseLog?
}
