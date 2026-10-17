package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseStatus
import javax.inject.Inject

/** Taking twice (from the notification and the app) keeps the first time taken. */
internal class TakeDose @Inject constructor(private val writer: DoseLogWriter) {
    suspend operator fun invoke(key: DoseKey) = writer.write(key) { current, schedule, now ->
        if (current?.status == DoseStatus.TAKEN) {
            null
        } else {
            (current ?: newLog(key, schedule, DoseStatus.TAKEN))
                .copy(status = DoseStatus.TAKEN, takenAt = now, snoozedUntil = null)
        }
    }
}
