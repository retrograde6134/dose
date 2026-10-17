package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseStatus
import java.time.Duration
import javax.inject.Inject

/**
 * Reminds again after [SNOOZE_DEFAULT]. A dose already taken or skipped is left alone, so a stale
 * notification can never undo it.
 */
internal class SnoozeDose @Inject constructor(private val writer: DoseLogWriter) {
    suspend operator fun invoke(key: DoseKey) = writer.write(key) { current, schedule, now ->
        when (current?.status) {
            null, DoseStatus.SNOOZED_PENDING, DoseStatus.MISSED ->
                (current ?: newLog(key, schedule, DoseStatus.SNOOZED_PENDING))
                    .copy(status = DoseStatus.SNOOZED_PENDING, snoozedUntil = now.plus(SNOOZE_DEFAULT))

            DoseStatus.TAKEN, DoseStatus.SKIPPED -> null
        }
    }

    companion object {
        /** Until settings make it configurable. */
        val SNOOZE_DEFAULT: Duration = Duration.ofMinutes(10)
    }
}
