package ca.alexlittle.dose.domain.schedule

import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.testing.schedule
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleEngineTest {
    private data class DayCase(
        val name: String,
        val start: String,
        val recurrence: Recurrence,
        val expected: Map<String, Boolean>,
        val end: String? = null,
    )

    @Test
    fun `occursOn follows each recurrence`() {
        for (case in dayCases) {
            val schedule = schedule(
                recurrence = case.recurrence,
                startDate = LocalDate.parse(case.start),
                endDate = case.end?.let(LocalDate::parse),
            )
            for ((date, expected) in case.expected) {
                assertEquals("${case.name}: $date", expected, ScheduleEngine.occursOn(schedule, LocalDate.parse(date)))
            }
        }
    }

    private data class TimesCase(
        val name: String,
        val times: List<String>,
        val from: String,
        val until: String,
        val expected: List<String>,
        val zone: ZoneId = TORONTO,
        val start: String = "2026-01-01",
        val end: String? = null,
    )

    @Test
    fun `doseTimes resolves wall-clock times in the given zone`() {
        for (case in timesCases) {
            val schedule = schedule(
                startDate = LocalDate.parse(case.start),
                endDate = case.end?.let(LocalDate::parse),
                timesOfDay = case.times.map(LocalTime::parse),
            )
            val actual = ScheduleEngine.doseTimes(
                schedule,
                Instant.parse(case.from),
                Instant.parse(case.until),
                case.zone,
            )
            assertEquals(case.name, case.expected.map(Instant::parse), actual)
        }
    }

    @Test
    fun `doseTimes only uses days the recurrence occurs on`() {
        val schedule = schedule(
            recurrence = Recurrence.Weekly(setOf(MONDAY, THURSDAY), everyNWeeks = 2),
            startDate = LocalDate.parse("2026-10-16"),
            timesOfDay = listOf(LocalTime.of(9, 0)),
        )

        val actual = ScheduleEngine.doseTimes(
            schedule,
            Instant.parse("2026-10-12T00:00:00Z"),
            Instant.parse("2026-11-10T00:00:00Z"),
            TORONTO,
        )

        // Oct 26 and 29 (EDT), then Nov 9 (EST).
        val expected = listOf("2026-10-26T13:00:00Z", "2026-10-29T13:00:00Z", "2026-11-09T14:00:00Z")
        assertEquals(expected.map(Instant::parse), actual)
    }

    @Test
    fun `as-needed schedules have no dose times`() {
        val schedule = schedule(recurrence = Recurrence.AsNeeded(240), timesOfDay = emptyList())

        val actual = ScheduleEngine.doseTimes(
            schedule,
            Instant.parse("2026-10-16T00:00:00Z"),
            Instant.parse("2026-10-30T00:00:00Z"),
            TORONTO,
        )

        assertEquals(emptyList<Instant>(), actual)
    }

    private companion object {
        val TORONTO: ZoneId = ZoneId.of("America/Toronto")
    }

    // 2026-10-16 is a Friday; its Monday-based week starts 2026-10-12.
    private val dayCases = listOf(
        DayCase(
            "daily, bounded by start and inclusive end",
            start = "2026-10-16",
            end = "2026-10-20",
            recurrence = Recurrence.Daily,
            expected = mapOf(
                "2026-10-15" to false,
                "2026-10-16" to true,
                "2026-10-20" to true,
                "2026-10-21" to false,
            ),
        ),
        DayCase(
            "daily across month and year end",
            start = "2026-12-31",
            recurrence = Recurrence.Daily,
            expected = mapOf("2026-12-31" to true, "2027-01-01" to true, "2027-02-28" to true),
        ),
        DayCase(
            "daily from a leap day",
            start = "2028-02-29",
            recurrence = Recurrence.Daily,
            expected = mapOf("2028-02-28" to false, "2028-02-29" to true, "2028-03-01" to true),
        ),
        DayCase(
            "Mon and Thu every 2 weeks, started mid-week: anchored to the start date's week",
            start = "2026-10-16",
            recurrence = Recurrence.Weekly(setOf(MONDAY, THURSDAY), everyNWeeks = 2),
            expected = mapOf(
                "2026-10-12" to false, // anchor week, but before the start date
                "2026-10-15" to false,
                "2026-10-19" to false, // off week
                "2026-10-22" to false,
                "2026-10-26" to true,
                "2026-10-29" to true,
                "2026-10-30" to false, // Friday, not a chosen day
                "2026-11-02" to false,
                "2026-11-09" to true,
            ),
        ),
        DayCase(
            "Sunday weekly: Sunday ends the Monday-based week",
            start = "2026-10-16",
            recurrence = Recurrence.Weekly(setOf(SUNDAY)),
            expected = mapOf("2026-10-18" to true, "2026-10-25" to true, "2026-10-24" to false),
        ),
        DayCase(
            "Sunday every 2 weeks: the first Sunday is in the anchor week",
            start = "2026-10-16",
            recurrence = Recurrence.Weekly(setOf(SUNDAY), everyNWeeks = 2),
            expected = mapOf("2026-10-18" to true, "2026-10-25" to false, "2026-11-01" to true),
        ),
        DayCase(
            "weekly across year end",
            start = "2026-12-28",
            recurrence = Recurrence.Weekly(setOf(MONDAY), everyNWeeks = 2),
            expected = mapOf("2026-12-28" to true, "2027-01-04" to false, "2027-01-11" to true),
        ),
        DayCase(
            "every 3 days",
            start = "2026-10-16",
            recurrence = Recurrence.EveryNDays(3),
            expected = mapOf(
                "2026-10-16" to true,
                "2026-10-17" to false,
                "2026-10-18" to false,
                "2026-10-19" to true,
            ),
        ),
        DayCase(
            "every 2 days across a leap February",
            start = "2028-02-27",
            recurrence = Recurrence.EveryNDays(2),
            expected = mapOf("2028-02-29" to true, "2028-03-01" to false, "2028-03-02" to true),
        ),
        DayCase(
            "every 2 days across a common February",
            start = "2027-02-27",
            recurrence = Recurrence.EveryNDays(2),
            expected = mapOf("2027-02-28" to false, "2027-03-01" to true),
        ),
        DayCase(
            "every 365 days from a leap day",
            start = "2028-02-29",
            recurrence = Recurrence.EveryNDays(365),
            expected = mapOf("2029-02-28" to true, "2029-03-01" to false),
        ),
        DayCase(
            "21 on, 7 off: boundaries of the cycle",
            start = "2026-10-16",
            recurrence = Recurrence.Cyclic(onDays = 21, offDays = 7),
            expected = mapOf(
                "2026-10-16" to true, // day 0
                "2026-11-05" to true, // day 20, last on day
                "2026-11-06" to false, // day 21, first off day
                "2026-11-12" to false, // day 27, last off day
                "2026-11-13" to true, // day 28, next cycle
            ),
        ),
        DayCase(
            "1 on, 1 off alternates",
            start = "2026-10-16",
            recurrence = Recurrence.Cyclic(onDays = 1, offDays = 1),
            expected = mapOf("2026-10-16" to true, "2026-10-17" to false, "2026-10-18" to true),
        ),
        DayCase(
            "as needed never occurs",
            start = "2026-10-16",
            recurrence = Recurrence.AsNeeded(null),
            expected = mapOf("2026-10-16" to false, "2026-10-17" to false),
        ),
    )

    private val timesCases = listOf(
        TimesCase(
            "several times per day, EDT",
            times = listOf("08:00", "20:30"),
            from = "2026-10-16T04:00:00Z",
            until = "2026-10-17T04:00:00Z",
            expected = listOf("2026-10-16T12:00:00Z", "2026-10-17T00:30:00Z"),
        ),
        TimesCase(
            "window includes from and excludes until",
            times = listOf("08:00"),
            from = "2026-10-16T12:00:00Z",
            until = "2026-10-17T12:00:00Z",
            expected = listOf("2026-10-16T12:00:00Z"),
        ),
        TimesCase(
            "empty window",
            times = listOf("08:00"),
            from = "2026-10-16T12:00:00Z",
            until = "2026-10-16T12:00:00Z",
            expected = emptyList(),
        ),
        TimesCase(
            "8:00 stays 8:00 across fall back",
            times = listOf("08:00"),
            from = "2026-10-31T00:00:00Z",
            until = "2026-11-03T00:00:00Z",
            expected = listOf("2026-10-31T12:00:00Z", "2026-11-01T13:00:00Z", "2026-11-02T13:00:00Z"),
        ),
        TimesCase(
            "8:00 stays 8:00 across spring forward",
            times = listOf("08:00"),
            from = "2026-03-07T00:00:00Z",
            until = "2026-03-10T00:00:00Z",
            expected = listOf("2026-03-07T13:00:00Z", "2026-03-08T12:00:00Z", "2026-03-09T12:00:00Z"),
        ),
        TimesCase(
            "time in the spring-forward gap moves later by the gap",
            times = listOf("02:30"),
            from = "2026-03-07T00:00:00Z",
            until = "2026-03-10T00:00:00Z",
            // 02:30 EST, then 03:30 EDT on the transition day, then 02:30 EDT.
            expected = listOf("2026-03-07T07:30:00Z", "2026-03-08T07:30:00Z", "2026-03-09T06:30:00Z"),
        ),
        TimesCase(
            "two times that collapse onto one instant in the gap are one dose",
            times = listOf("02:00", "03:00"),
            from = "2026-03-08T05:00:00Z",
            until = "2026-03-09T04:00:00Z",
            expected = listOf("2026-03-08T07:00:00Z"),
        ),
        TimesCase(
            "time in the fall-back overlap happens once, at the earlier offset",
            times = listOf("01:30"),
            from = "2026-11-01T04:00:00Z",
            until = "2026-11-02T05:00:00Z",
            expected = listOf("2026-11-01T05:30:00Z"),
        ),
        TimesCase(
            "same schedule resolves in another zone after travel",
            times = listOf("08:00"),
            from = "2026-10-15T22:00:00Z",
            until = "2026-10-16T22:00:00Z",
            zone = ZoneId.of("Europe/Paris"),
            expected = listOf("2026-10-16T06:00:00Z"),
        ),
        TimesCase(
            "start and end dates bound the window",
            times = listOf("08:00"),
            from = "2026-10-10T00:00:00Z",
            until = "2026-10-25T00:00:00Z",
            start = "2026-10-16",
            end = "2026-10-17",
            expected = listOf("2026-10-16T12:00:00Z", "2026-10-17T12:00:00Z"),
        ),
        TimesCase(
            "window starting late in a local day still finds the evening dose",
            times = listOf("23:30"),
            from = "2026-10-17T03:00:00Z", // 23:00 on Oct 16 in Toronto
            until = "2026-10-17T04:00:00Z",
            expected = listOf("2026-10-17T03:30:00Z"),
        ),
    )
}
