package ca.alexlittle.dose.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import ca.alexlittle.dose.domain.reminder.ReminderAlarms
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/**
 * Arms the single reminder alarm with [AlarmManager]. Every arm replaces the previous one, because the
 * pending intent is always the same; only its extra (the armed time) changes.
 *
 * `setAlarmClock` is deliberately not used: it shows an alarm icon in the status bar and is kept for
 * the optional full-screen reminder for critical medications (Phase 2).
 */
internal class AlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) : ReminderAlarms {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun arm(at: Instant) {
        val operation = PendingIntent.getBroadcast(
            context,
            0,
            ReminderReceiver.intent(context, at),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val millis = at.toEpochMilli()
        try {
            if (ReminderAccess.exactAlarmsAllowed(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
            } else {
                setInexact(millis, operation)
            }
        } catch (_: SecurityException) {
            // API 31-32: the user revoked "Alarms & reminders" between the check and the call.
            setInexact(millis, operation)
        }
    }

    override fun cancel() {
        PendingIntent.getBroadcast(
            context,
            0,
            ReminderReceiver.intent(context, Instant.EPOCH),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let(alarmManager::cancel)
    }

    // Still allowed in Doze, but the system may deliver it some minutes late; ReminderReceiver then
    // shows everything that came due in the meantime.
    private fun setInexact(millis: Long, operation: PendingIntent) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
    }
}
