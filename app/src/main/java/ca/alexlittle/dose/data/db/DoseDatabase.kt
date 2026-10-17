package ca.alexlittle.dose.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MedicationEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
internal abstract class DoseDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        const val NAME = "dose.db"
    }
}
