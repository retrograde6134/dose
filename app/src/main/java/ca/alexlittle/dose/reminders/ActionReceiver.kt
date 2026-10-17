package ca.alexlittle.dose.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.alexlittle.dose.di.ApplicationScope
import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.usecase.SkipDose
import ca.alexlittle.dose.domain.usecase.SnoozeDose
import ca.alexlittle.dose.domain.usecase.TakeDose
import ca.alexlittle.dose.util.goAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Handles Taken, Snooze and Skip from a reminder notification. One tap, no confirmation. */
@AndroidEntryPoint
internal class ActionReceiver : BroadcastReceiver() {
    @Inject lateinit var takeDose: TakeDose

    @Inject lateinit var snoozeDose: SnoozeDose

    @Inject lateinit var skipDose: SkipDose

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val key = DoseIntents.key(intent) ?: return
        val action = intent.action?.takeIf { it in ACTIONS } ?: return
        goAsync(scope) {
            when (action) {
                ACTION_TAKE -> takeDose(key)
                ACTION_SNOOZE -> snoozeDose(key)
                ACTION_SKIP -> skipDose(key)
            }
        }
    }

    companion object {
        const val ACTION_TAKE = "ca.alexlittle.dose.action.TAKE"
        const val ACTION_SNOOZE = "ca.alexlittle.dose.action.SNOOZE"
        const val ACTION_SKIP = "ca.alexlittle.dose.action.SKIP"
        private val ACTIONS = setOf(ACTION_TAKE, ACTION_SNOOZE, ACTION_SKIP)

        fun intent(context: Context, action: String, key: DoseKey): Intent =
            Intent(context, ActionReceiver::class.java).setAction(action).setData(DoseIntents.uri(key))
    }
}
