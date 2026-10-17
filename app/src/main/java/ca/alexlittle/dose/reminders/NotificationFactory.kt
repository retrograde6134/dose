package ca.alexlittle.dose.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import ca.alexlittle.dose.MainActivity
import ca.alexlittle.dose.R
import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.schedule.DueDose
import ca.alexlittle.dose.ui.medications.formatStrength
import ca.alexlittle.dose.ui.schedules.ScheduleFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Builds dose reminder notifications and owns their channel. */
internal class NotificationFactory @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Sound and vibration are left to the channel, so the user controls them in system settings.
     * Creating an existing channel again only updates its name and description.
     */
    fun createChannels() {
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notification_channel_reminders_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Title is the medication name and text the amount, e.g. "1 tablet · 500 mg". The time shown is
     * when the dose was scheduled, also for a snoozed reminder. On the lock screen only a generic
     * version is shown, so medication names stay private.
     */
    fun reminder(dose: DueDose, medication: Medication): Notification {
        val resources = context.resources
        val locale = resources.configuration.locales[0]
        val formatter = ScheduleFormatter(resources, locale, DateFormat.is24HourFormat(context))
        val quantity = formatter.quantity(dose.quantity, medication.form)
        val text = medication.strengthValue?.let {
            val strength = resources.getString(
                R.string.medication_strength,
                formatStrength(it, locale),
                medication.strengthUnit.orEmpty(),
            )
            resources.getString(R.string.reminder_text, quantity, strength)
        } ?: quantity

        val public = builder()
            .setContentTitle(resources.getString(R.string.reminder_public_title))
            .build()

        return builder()
            .setContentTitle(medication.name)
            .setContentText(text)
            .setWhen(dose.key.scheduledAt.toEpochMilli())
            .setShowWhen(true)
            .setColor(medication.colorArgb)
            .setContentIntent(openApp(dose.key))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .addAction(
                action(dose.key, ActionReceiver.ACTION_TAKE, R.drawable.ic_check, R.string.reminder_action_taken),
            )
            .addAction(
                action(dose.key, ActionReceiver.ACTION_SNOOZE, R.drawable.ic_snooze, R.string.reminder_action_snooze),
            )
            .addAction(action(dose.key, ActionReceiver.ACTION_SKIP, R.drawable.ic_close, R.string.reminder_action_skip))
            .build()
    }

    private fun builder() = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
        .setSmallIcon(R.drawable.ic_notification)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    private fun action(key: DoseKey, action: String, icon: Int, label: Int): NotificationCompat.Action {
        val intent = ActionReceiver.intent(context, action, key)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(icon, context.getString(label), pending).build()
    }

    private fun openApp(key: DoseKey): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData(DoseIntents.uri(key))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_REMINDERS = "dose_reminders"
    }
}
