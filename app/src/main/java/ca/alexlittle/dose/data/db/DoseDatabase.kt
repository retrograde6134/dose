package ca.alexlittle.dose.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MedicationEntity::class, ScheduleEntity::class, DoseLogEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // 2: adds the schedules table.
        AutoMigration(from = 1, to = 2),
        // 3: adds the dose_logs table.
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class, ScheduleConverters::class)
internal abstract class DoseDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    abstract fun scheduleDao(): ScheduleDao

    abstract fun doseLogDao(): DoseLogDao

    companion object {
        const val NAME = "dose.db"
    }
}
