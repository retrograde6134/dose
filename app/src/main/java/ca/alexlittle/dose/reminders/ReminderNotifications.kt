package ca.alexlittle.dose.reminders

import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.schedule.DueDose
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * One notification per dose, tagged with the dose identity and numbered with the medication id, so
 * showing the same dose again replaces it, and a schedule's or medication's reminders can be found.
 */
internal class ReminderNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val factory: NotificationFactory,
) : ReminderNotifier {
    private val manager = context.getSystemService(NotificationManager::class.java)

    override fun show(dose: DueDose, medication: Medication) {
        if (!ReminderAccess.notificationsAllowed(context)) return
        manager.notify(DoseIntents.tag(dose.key), medication.id.toInt(), factory.reminder(dose, medication))
    }

    override fun isShowing(key: DoseKey): Boolean = reminders().any { it.second == key }

    override fun cancel(key: DoseKey) = cancelWhere { _, k -> k == key }

    override fun cancelForSchedule(scheduleId: Long) = cancelWhere { _, k -> k.scheduleId == scheduleId }

    override fun cancelForMedication(medicationId: Long) = cancelWhere { n, _ -> n.id == medicationId.toInt() }

    private fun reminders(): List<Pair<StatusBarNotification, DoseKey>> =
        manager.activeNotifications.mapNotNull { n -> DoseIntents.key(n.tag)?.let { n to it } }

    private fun cancelWhere(predicate: (StatusBarNotification, DoseKey) -> Boolean) {
        for ((notification, key) in reminders()) {
            if (predicate(notification, key)) manager.cancel(notification.tag, notification.id)
        }
    }
}
