package ca.alexlittle.dose.reminders

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.schedule.DueDose
import ca.alexlittle.dose.testing.medication
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rCA")
class ReminderNotificationsTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val factory = NotificationFactory(context)
    private val notifications = ReminderNotifications(context, factory)

    private val metformin = medication(name = "Metformin").copy(id = 1)
    private val aspirin = medication(name = "Aspirin").copy(id = 2, strengthValue = null, strengthUnit = null)

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        factory.createChannels()
    }

    @Test
    fun `the reminder names the medication and amount, with three actions`() {
        notifications.show(dose(scheduleId = 1, medicationId = 1, quantity = "0.5"), metformin)

        val notification = posted().single()
        assertEquals("Metformin", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("0.5 tablets · 500 mg", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(NotificationFactory.CHANNEL_REMINDERS, notification.channelId)
        assertEquals(Notification.CATEGORY_REMINDER, notification.category)
        assertEquals(AT.toEpochMilli(), notification.`when`)
        assertEquals(listOf("Taken", "Snooze", "Skip"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun `without a strength the text is just the amount`() {
        notifications.show(dose(scheduleId = 1, medicationId = 2), aspirin)

        assertEquals("1 tablet", posted().single().extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun `the lock screen shows a generic version without the medication name`() {
        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)

        val notification = posted().single()
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals("Time for a dose", notification.publicVersion.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun `actions are immutable and distinct for every dose`() {
        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)
        notifications.show(dose(scheduleId = 2, medicationId = 2), aspirin)

        val actions = posted().flatMap { n -> n.actions.map { it.actionIntent } }
        assertEquals(6, actions.size)
        assertEquals(6, actions.toSet().size)
        for (pending in actions) assertTrue(shadowOf(pending).isImmutable)

        val take = shadowOf(actions.first()).savedIntent
        assertEquals(ActionReceiver.ACTION_TAKE, take.action)
        assertEquals(DoseKey(1, AT), DoseIntents.key(take))
    }

    @Test
    fun `tapping the reminder opens the app`() {
        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)

        val content: PendingIntent = posted().single().contentIntent
        assertTrue(shadowOf(content).isActivity)
        assertTrue(shadowOf(content).isImmutable)
    }

    @Test
    fun `showing the same dose again replaces it`() {
        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)
        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)

        assertEquals(1, manager.activeNotifications.size)
    }

    @Test
    fun `isShowing and cancel find a dose by its identity`() {
        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)
        notifications.show(dose(scheduleId = 1, medicationId = 1, at = AT.plusSeconds(3600)), metformin)

        notifications.cancel(DoseKey(1, AT))

        assertFalse(notifications.isShowing(DoseKey(1, AT)))
        assertTrue(notifications.isShowing(DoseKey(1, AT.plusSeconds(3600))))
    }

    @Test
    fun `cancelling by schedule or medication clears only those reminders`() {
        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)
        notifications.show(dose(scheduleId = 3, medicationId = 1), metformin)
        notifications.show(dose(scheduleId = 2, medicationId = 2), aspirin)

        notifications.cancelForSchedule(3)
        assertEquals(setOf(DoseKey(1, AT), DoseKey(2, AT)), showingKeys())

        notifications.cancelForMedication(2)
        assertEquals(setOf(DoseKey(1, AT)), showingKeys())
    }

    @Test
    fun `nothing is posted without notification permission`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifications.show(dose(scheduleId = 1, medicationId = 1), metformin)

        assertEquals(0, manager.activeNotifications.size)
    }

    @Test
    fun `a dose identity survives the round trip through an intent`() {
        val key = DoseKey(42, Instant.parse("2026-11-01T05:30:00Z"))

        assertEquals(key, DoseIntents.key(ActionReceiver.intent(context, ActionReceiver.ACTION_SKIP, key)))
        assertEquals(key, DoseIntents.key(DoseIntents.tag(key)))
        assertEquals(null, DoseIntents.key("not-a-dose"))
        assertNotEquals(DoseIntents.tag(key), DoseIntents.tag(key.copy(scheduleId = 43)))
    }

    private fun posted(): List<Notification> = manager.activeNotifications.sortedBy {
        it.postTime
    }.map { it.notification }

    private fun showingKeys(): Set<DoseKey> = manager.activeNotifications.mapNotNull { DoseIntents.key(it.tag) }.toSet()

    private fun dose(scheduleId: Long, medicationId: Long, quantity: String = "1", at: Instant = AT) =
        DueDose(DoseKey(scheduleId, at), medicationId, BigDecimal(quantity), at)

    private companion object {
        val AT: Instant = Instant.parse("2026-10-16T12:00:00Z")
    }
}
