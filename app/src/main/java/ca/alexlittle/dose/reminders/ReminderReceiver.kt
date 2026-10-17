package ca.alexlittle.dose.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.alexlittle.dose.di.ApplicationScope
import ca.alexlittle.dose.domain.usecase.RescheduleReminders
import ca.alexlittle.dose.util.goAsync
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Fires when the reminder alarm goes off: shows what is due, then arms the next alarm. */
@AndroidEntryPoint
internal class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var reschedule: RescheduleReminders

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val armedAt = intent.getLongExtra(EXTRA_ARMED_AT, -1L).takeIf { it >= 0 }?.let(Instant::ofEpochMilli) ?: return
        goAsync(scope) { reschedule.onAlarm(armedAt) }
    }

    companion object {
        private const val ACTION_REMIND = "ca.alexlittle.dose.action.REMIND"
        private const val EXTRA_ARMED_AT = "armedAt"

        fun intent(context: Context, armedAt: Instant): Intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .putExtra(EXTRA_ARMED_AT, armedAt.toEpochMilli())
    }
}
