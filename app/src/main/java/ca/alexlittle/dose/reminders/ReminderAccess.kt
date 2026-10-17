package ca.alexlittle.dose.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** What the system lets this app do for reminders, and where the user can change it. */
internal object ReminderAccess {
    /** API 33+ needs POST_NOTIFICATIONS. On any version the user can turn off the app or the channel. */
    fun notificationsAllowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(NotificationFactory.CHANNEL_REMINDERS)
        return !needsNotificationPermission(context) &&
            manager.areNotificationsEnabled() &&
            channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** True on API 33+ until POST_NOTIFICATIONS is granted. */
    fun needsNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    /**
     * Always true below API 31. On API 33+ USE_EXACT_ALARM grants it permanently; on API 31-32 it relies
     * on SCHEDULE_EXACT_ALARM, which is granted by default there but can be revoked by the user.
     */
    fun exactAlarmsAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** The app's notification settings. Turning notifications on there also grants the API 33 permission. */
    fun notificationSettings(context: Context): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    /**
     * API 31+ "Alarms & reminders" page for this app. Only used when [exactAlarmsAllowed] is false,
     * which cannot happen below API 31.
     */
    @SuppressLint("InlinedApi")
    fun exactAlarmSettings(context: Context): Intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        .setData(Uri.fromParts("package", context.packageName, null))
}
