package ca.alexlittle.dose.testing

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.DoseLog
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.repository.DoseLogRepository
import java.time.Instant

internal class FakeDoseLogRepository : DoseLogRepository {
    private val logs = mutableMapOf<Long, DoseLog>()
    private var nextId = 1L

    val all: List<DoseLog> get() = logs.values.sortedBy { it.id }

    override suspend fun get(key: DoseKey): DoseLog? = logs.values.find { it.key == key }

    override suspend fun scheduledBetween(from: Instant, until: Instant): List<DoseLog> = logs.values
        .filter { at -> at.scheduledAt?.let { !it.isBefore(from) && it.isBefore(until) } == true }
        .sortedWith(compareBy({ it.scheduledAt }, { it.id }))

    override suspend fun snoozedPending(): List<DoseLog> = logs.values.filter {
        it.status == DoseStatus.SNOOZED_PENDING
    }.sortedWith(compareBy({ it.snoozedUntil }, { it.id }))

    override suspend fun modify(key: DoseKey, transform: (DoseLog?) -> DoseLog?): DoseLog? {
        val current = get(key)
        val next =
            transform(current)?.copy(scheduleId = key.scheduleId, scheduledAt = key.scheduledAt) ?: return current
        val id = current?.id ?: nextId++
        return next.copy(id = id).also { logs[id] = it }
    }
}
