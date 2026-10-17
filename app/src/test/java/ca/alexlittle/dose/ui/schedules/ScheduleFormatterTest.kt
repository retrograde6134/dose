package ca.alexlittle.dose.ui.schedules

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.testing.schedule
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleFormatterTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val us = ScheduleFormatter(resources, Locale.US, is24Hour = false)

    @Test
    fun `summarizes each recurrence`() {
        val cases = mapOf(
            Recurrence.Daily to "Every day",
            Recurrence.Weekly(setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY)) to "Mon, Thu",
            Recurrence.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), everyNWeeks = 2) to "Sun, Mon, every 2 weeks",
            Recurrence.Weekly(DayOfWeek.entries.toSet()) to "Every day",
            Recurrence.EveryNDays(1) to "Every day",
            Recurrence.EveryNDays(3) to "Every 3 days",
            Recurrence.Cyclic(onDays = 21, offDays = 7) to "21 days on, 7 days off",
            Recurrence.Cyclic(onDays = 1, offDays = 1) to "1 day on, 1 day off",
            Recurrence.AsNeeded(null) to "As needed",
        )

        for ((recurrence, expected) in cases) {
            assertEquals(expected, us.recurrence(recurrence))
        }
    }

    @Test
    fun `week order follows the locale`() {
        assertEquals(DayOfWeek.SUNDAY, us.weekOrder.first())
        assertEquals(DayOfWeek.MONDAY, ScheduleFormatter(resources, Locale.FRANCE, is24Hour = true).weekOrder.first())
    }

    @Test
    fun `summary joins recurrence, times and quantity`() {
        val schedule = schedule(timesOfDay = listOf(LocalTime.of(8, 0), LocalTime.of(20, 30)))

        assertEquals("Every day · 8:00 AM, 8:30 PM · 1 tablet", us.summary(schedule, MedicationForm.TABLET))
    }

    @Test
    fun `times follow the 24-hour setting`() {
        val formatter = ScheduleFormatter(resources, Locale.US, is24Hour = true)

        assertEquals("20:30", formatter.time(LocalTime.of(20, 30)))
    }

    @Test
    fun `as-needed summary shows the minimum interval instead of times`() {
        val withInterval = schedule(recurrence = Recurrence.AsNeeded(240), timesOfDay = emptyList(), doseQuantity = "2")
        val withMinutes = withInterval.copy(recurrence = Recurrence.AsNeeded(90))
        val without = withInterval.copy(recurrence = Recurrence.AsNeeded(null))

        assertEquals("As needed · at least 4 hours apart · 2 puffs", us.summary(withInterval, MedicationForm.INHALER))
        assertEquals("As needed · at least 90 minutes apart · 2 puffs", us.summary(withMinutes, MedicationForm.INHALER))
        assertEquals("As needed · 2 puffs", us.summary(without, MedicationForm.INHALER))
    }

    @Test
    fun `quantity uses plural rules that handle fractions`() {
        val french = ScheduleFormatter(resources, Locale.FRANCE, is24Hour = true)

        assertEquals("1 tablet", us.quantity(BigDecimal.ONE, MedicationForm.TABLET))
        assertEquals("2 drops", us.quantity(BigDecimal("2"), MedicationForm.DROPS))
        assertEquals("0.5 tablets", us.quantity(BigDecimal("0.5"), MedicationForm.TABLET))
        assertEquals("1.5 capsules", us.quantity(BigDecimal("1.5"), MedicationForm.CAPSULE))
        // Only the formatting is localized here (no French strings yet), but the separator must follow the locale.
        assertEquals("0,5", french.quantity(BigDecimal("0.5"), MedicationForm.TABLET).substringBefore(' '))
    }

    @Test
    fun `dates show a future start and any end`() {
        val today = LocalDate.of(2026, 10, 16)

        assertNull(us.dates(schedule(startDate = today), today))
        assertNull(us.dates(schedule(startDate = today.minusDays(3)), today))
        assertEquals("Starts Oct 20, 2026", us.dates(schedule(startDate = LocalDate.of(2026, 10, 20)), today))
        assertEquals(
            "Until Nov 30, 2026",
            us.dates(schedule(startDate = today, endDate = LocalDate.of(2026, 11, 30)), today),
        )
        assertEquals(
            "Oct 20, 2026 to Nov 30, 2026",
            us.dates(schedule(startDate = LocalDate.of(2026, 10, 20), endDate = LocalDate.of(2026, 11, 30)), today),
        )
    }
}
