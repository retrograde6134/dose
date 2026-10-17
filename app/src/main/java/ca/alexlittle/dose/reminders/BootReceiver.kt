package ca.alexlittle.dose.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.alexlittle.dose.di.ApplicationScope
import ca.alexlittle.dose.domain.usecase.RescheduleReminders
import ca.alexlittle.dose.util.goAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Re-arms the reminder alarm whenever it may be gone or wrong: alarms do not survive a reboot, a time
 * or zone change moves when wall-clock doses fall, and on API 31-32 exact alarms can be granted again.
 * Also shows reminders that came due while no alarm could fire.
 */
@AndroidEntryPoint
internal class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var reschedule: RescheduleReminders

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        goAsync(scope) { reschedule(catchUp = true) }
    }

    private companion object {
        // The API 31 action is a plain string; older versions simply never send it.
        @SuppressLint("InlinedApi")
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            // API 31+. Sent only when the permission is granted; revoking it kills the app instead.
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
