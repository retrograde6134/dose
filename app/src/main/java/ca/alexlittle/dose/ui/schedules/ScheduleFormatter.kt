package ca.alexlittle.dose.ui.schedules

import android.content.res.Resources
import android.icu.text.MessageFormat
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import ca.alexlittle.dose.R
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Plain-language text for schedules, in the user's locale and 12/24-hour setting. */
internal class ScheduleFormatter(private val resources: Resources, private val locale: Locale, is24Hour: Boolean) {
    private val timeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    /** Days of the week starting on the locale's first day, e.g. Sunday in the US and Monday in France. */
    val weekOrder: List<DayOfWeek> = WeekFields.of(locale).firstDayOfWeek.let { first ->
        List(DayOfWeek.entries.size) { first.plus(it.toLong()) }
    }

    /** "Every day · 8:00 AM, 8:00 PM · 1 tablet" or "As needed · at least 4 hours apart · 2 puffs". */
    fun summary(schedule: Schedule, form: MedicationForm): String {
        val recurrence = schedule.recurrence
        val whenText = if (recurrence is Recurrence.AsNeeded) {
            recurrence.minIntervalMinutes?.let { resources.getString(R.string.schedule_min_interval, duration(it)) }
        } else {
            schedule.timesOfDay.joinToString(listSeparator, transform = ::time)
        }
        return listOfNotNull(recurrence(recurrence), whenText, quantity(schedule.doseQuantity, form))
            .joinToString(resources.getString(R.string.schedule_summary_separator))
    }

    /** "Starts Oct 20, 2026", "Until Nov 30, 2026", both, or null for an open-ended schedule already running. */
    fun dates(schedule: Schedule, today: LocalDate): String? {
        val start = schedule.startDate.takeIf { it > today }
        val end = schedule.endDate
        return when {
            start != null && end != null -> resources.getString(R.string.schedule_date_range, date(start), date(end))
            start != null -> resources.getString(R.string.schedule_starts, date(start))
            end != null -> resources.getString(R.string.schedule_until, date(end))
            else -> null
        }
    }

    fun recurrence(recurrence: Recurrence): String = when (recurrence) {
        Recurrence.Daily -> resources.getString(R.string.schedule_every_day)

        is Recurrence.Weekly -> if (recurrence.days.size == DayOfWeek.entries.size && recurrence.everyNWeeks == 1) {
            resources.getString(R.string.schedule_every_day)
        } else {
            val days = weekOrder.filter { it in recurrence.days }.joinToString(listSeparator, transform = ::shortDay)
            val n = recurrence.everyNWeeks
            resources.getQuantityString(R.plurals.schedule_weekdays_every_n_weeks, n, n, days)
        }

        is Recurrence.EveryNDays -> resources.getQuantityString(
            R.plurals.schedule_every_n_days,
            recurrence.n,
            recurrence.n,
        )

        is Recurrence.Cyclic -> resources.getString(
            R.string.schedule_cycle,
            days(recurrence.onDays),
            days(recurrence.offDays),
        )

        is Recurrence.AsNeeded -> resources.getString(R.string.schedule_as_needed)
    }

    fun time(time: LocalTime): String = timeFormatter.format(time)

    fun date(date: LocalDate): String = dateFormatter.format(date)

    /** "1 tablet", "0.5 tablets", "2 puffs", with plural rules that handle fractions. */
    fun quantity(quantity: BigDecimal, form: MedicationForm): String =
        MessageFormat(resources.getString(form.quantityRes()), locale).format(arrayOf(quantity))

    fun shortDay(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, locale)

    fun fullDay(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, locale)

    fun days(count: Int): String = resources.getQuantityString(R.plurals.schedule_days, count, count)

    fun duration(minutes: Int): String = if (minutes % MINUTES_PER_HOUR == 0) {
        val hours = minutes / MINUTES_PER_HOUR
        resources.getQuantityString(R.plurals.schedule_hours, hours, hours)
    } else {
        resources.getQuantityString(R.plurals.schedule_minutes, minutes, minutes)
    }

    private val listSeparator: String get() = resources.getString(R.string.schedule_list_separator)

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}

@StringRes
private fun MedicationForm.quantityRes(): Int = when (this) {
    MedicationForm.TABLET -> R.string.dose_quantity_tablet
    MedicationForm.CAPSULE -> R.string.dose_quantity_capsule
    MedicationForm.LIQUID -> R.string.dose_quantity_liquid
    MedicationForm.INJECTION -> R.string.dose_quantity_injection
    MedicationForm.DROPS -> R.string.dose_quantity_drops
    MedicationForm.INHALER -> R.string.dose_quantity_inhaler
    MedicationForm.OTHER -> R.string.dose_quantity_other
}

@Composable
internal fun rememberScheduleFormatter(): ScheduleFormatter {
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    // Read on each composition: the 12/24-hour setting is not part of the configuration.
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(resources, locale, is24Hour) { ScheduleFormatter(resources, locale, is24Hour) }
}
