package ca.alexlittle.dose.testing

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.reminder.ReminderAlarms
import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.schedule.DueDose
import ca.alexlittle.dose.domain.usecase.RescheduleReminders
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal class FakeReminderAlarms : ReminderAlarms {
    var armedAt: Instant? = null
        private set

    override fun arm(at: Instant) {
        armedAt = at
    }

    override fun cancel() {
        armedAt = null
    }
}

internal class FakeReminderNotifier : ReminderNotifier {
    /** What is on screen, by dose. */
    val showing = mutableMapOf<DoseKey, DueDose>()

    /** Every show, including repeats, so tests can tell a re-alert from a single one. */
    val shown = mutableListOf<DueDose>()

    override fun show(dose: DueDose, medication: Medication) {
        showing[dose.key] = dose
        shown += dose
    }

    override fun isShowing(key: DoseKey): Boolean = key in showing

    override fun cancel(key: DoseKey) {
        showing -= key
    }

    override fun cancelForSchedule(scheduleId: Long) {
        showing.keys.removeAll { it.scheduleId == scheduleId }
    }

    override fun cancelForMedication(medicationId: Long) {
        showing.values.removeAll { it.medicationId == medicationId }
    }
}

/** A clock tests can move, and move between zones, like a phone that is rebooted or travels. */
internal class MutableClock(var now: Instant, private var currentZone: ZoneId = ZoneId.of("America/Toronto")) :
    Clock() {
    override fun getZone(): ZoneId = currentZone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun moveTo(zone: ZoneId) {
        currentZone = zone
    }
}

/** The reminder pipeline wired to fakes, sharing one set of repositories. */
internal class ReminderHarness(now: Instant = FIXED_INSTANT) {
    val clock = MutableClock(now)
    val medications = FakeMedicationRepository()
    val schedules = FakeScheduleRepository { id -> medications.all.any { it.id == id && !it.archived } }
    val logs = FakeDoseLogRepository()
    val alarms = FakeReminderAlarms()
    val notifier = FakeReminderNotifier()
    val reschedule = RescheduleReminders(schedules, logs, medications, alarms, notifier, clock)
}
