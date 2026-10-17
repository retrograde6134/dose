package ca.alexlittle.dose

import android.app.Application
import ca.alexlittle.dose.di.ApplicationScope
import ca.alexlittle.dose.domain.usecase.RescheduleReminders
import ca.alexlittle.dose.reminders.NotificationFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class DoseApp : Application() {
    @Inject internal lateinit var notifications: NotificationFactory

    @Inject internal lateinit var reschedule: RescheduleReminders

    @Inject @ApplicationScope
    internal lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
        // A force stop clears the app's alarms and sends no broadcast, so every process start re-arms.
        // No catch-up here: that would bring back reminders the user swiped away.
        scope.launch { reschedule() }
    }
}
