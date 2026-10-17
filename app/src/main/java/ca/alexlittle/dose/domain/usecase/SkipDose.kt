package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseStatus
import javax.inject.Inject

/** The latest choice wins, so skipping a dose taken by mistake undoes it. */
internal class SkipDose @Inject constructor(private val writer: DoseLogWriter) {
    suspend operator fun invoke(key: DoseKey) = writer.write(key) { current, schedule, _ ->
        (current ?: newLog(key, schedule, DoseStatus.SKIPPED))
            .copy(status = DoseStatus.SKIPPED, takenAt = null, snoozedUntil = null)
    }
}
