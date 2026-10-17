package ca.alexlittle.dose.reminders

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val scheduler = AlarmScheduler(context)

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test
    fun `arms one exact wake-up alarm that works in Doze`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        scheduler.arm(AT)

        val alarm = alarmManager.scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertEquals(AT.toEpochMilli(), alarm.triggerAtMs)
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, alarm.windowLengthMs)
        assertTrue(alarm.isAllowWhileIdle)
        assertTrue(shadowOf(alarm.operation).isImmutable)
    }

    @Test
    fun `falls back to an inexact alarm without exact-alarm access`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.arm(AT)

        val alarm = alarmManager.scheduledAlarms.single()
        assertEquals(AT.toEpochMilli(), alarm.triggerAtMs)
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, alarm.windowLengthMs)
        assertTrue(alarm.isAllowWhileIdle)
    }

    @Test
    fun `arming again replaces the previous alarm and carries the new time`() {
        scheduler.arm(AT)
        scheduler.arm(AT.plusSeconds(3600))

        val alarm = alarmManager.scheduledAlarms.single()
        assertEquals(AT.plusSeconds(3600).toEpochMilli(), alarm.triggerAtMs)
        val intent = shadowOf(alarm.operation).savedIntent
        assertEquals(ReminderReceiver.intent(context, AT).action, intent.action)
        assertEquals(AT.plusSeconds(3600).toEpochMilli(), intent.getLongExtra("armedAt", -1))
    }

    @Test
    fun `cancel removes the alarm, and is safe when none is armed`() {
        scheduler.cancel()
        scheduler.arm(AT)

        scheduler.cancel()

        assertEquals(emptyList<ShadowAlarmManager.ScheduledAlarm>(), alarmManager.scheduledAlarms)
    }

    private companion object {
        val AT: Instant = Instant.parse("2026-10-16T12:00:00Z")
    }
}
