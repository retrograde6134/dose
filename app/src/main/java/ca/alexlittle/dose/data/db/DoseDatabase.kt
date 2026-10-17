package ca.alexlittle.dose.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MedicationEntity::class, ScheduleEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // 2: adds the schedules table.
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(Converters::class, ScheduleConverters::class)
internal abstract class DoseDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    abstract fun scheduleDao(): ScheduleDao

    companion object {
        const val NAME = "dose.db"
    }
}
