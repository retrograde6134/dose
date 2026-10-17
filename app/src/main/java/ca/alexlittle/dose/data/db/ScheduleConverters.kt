package ca.alexlittle.dose.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

internal class ScheduleConverters {
    // ISO text keeps dates readable and sortable, and independent of any zone.
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    // Times are never queried individually, so a comma-separated list avoids a join table.
    @TypeConverter
    fun timesToString(times: List<LocalTime>): String = times.joinToString(",")

    @TypeConverter
    fun stringToTimes(value: String): List<LocalTime> =
        if (value.isEmpty()) emptyList() else value.split(',').map(LocalTime::parse)

    @TypeConverter
    fun scheduleTypeToString(type: ScheduleType): String = type.name

    @TypeConverter
    fun stringToScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)
}
