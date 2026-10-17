package ca.alexlittle.dose.di

import javax.inject.Qualifier

/** Outlives any screen: for work started by receivers and app start that must not be cancelled early. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope
