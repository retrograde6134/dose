package ca.alexlittle.dose.di

import ca.alexlittle.dose.domain.reminder.ReminderAlarms
import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.reminders.AlarmScheduler
import ca.alexlittle.dose.reminders.ReminderNotifications
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ReminderModule {
    @Binds
    fun bindReminderAlarms(impl: AlarmScheduler): ReminderAlarms

    @Binds
    fun bindReminderNotifier(impl: ReminderNotifications): ReminderNotifier
}
