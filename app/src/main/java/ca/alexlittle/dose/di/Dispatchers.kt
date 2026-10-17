package ca.alexlittle.dose.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultDispatcher
