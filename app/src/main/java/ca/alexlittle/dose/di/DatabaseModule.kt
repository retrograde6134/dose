package ca.alexlittle.dose.di

import android.content.Context
import androidx.room.Room
import ca.alexlittle.dose.data.db.DoseDatabase
import ca.alexlittle.dose.data.db.MedicationDao
import ca.alexlittle.dose.data.repository.RoomMedicationRepository
import ca.alexlittle.dose.domain.repository.MedicationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    // No destructive fallback: every schema change ships with a migration.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DoseDatabase =
        Room.databaseBuilder(context, DoseDatabase::class.java, DoseDatabase.NAME).build()

    @Provides
    fun provideMedicationDao(database: DoseDatabase): MedicationDao = database.medicationDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {
    @Binds
    fun bindMedicationRepository(impl: RoomMedicationRepository): MedicationRepository
}
