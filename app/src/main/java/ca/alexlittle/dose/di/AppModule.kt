package ca.alexlittle.dose.di

import ca.alexlittle.dose.util.SystemZoneClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    @Provides
    fun provideClock(): Clock = SystemZoneClock

    // Unscoped so each ViewModel picks up the locale current at its creation.
    @Provides
    fun provideLocale(): Locale = Locale.getDefault()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
