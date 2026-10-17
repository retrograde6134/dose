package ca.alexlittle.dose.domain.reminder

import java.time.Instant

/** The single reminder alarm. Arming replaces whatever was armed before. */
internal interface ReminderAlarms {
    fun arm(at: Instant)

    fun cancel()
}
