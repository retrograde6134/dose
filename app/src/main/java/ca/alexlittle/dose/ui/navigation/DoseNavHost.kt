package ca.alexlittle.dose.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ca.alexlittle.dose.ui.medications.detail.MedicationDetailRoute
import ca.alexlittle.dose.ui.medications.edit.MedicationEditRoute
import ca.alexlittle.dose.ui.medications.list.MedicationListRoute
import ca.alexlittle.dose.ui.schedules.edit.ScheduleEditRoute
import ca.alexlittle.dose.ui.today.TodayRoute

// Navigation callbacks are wrapped in dropUnlessResumed so a double tap during a transition
// cannot navigate twice or pop past the start destination.
@Composable
internal fun DoseNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TodayDestination,
        modifier = modifier,
    ) {
        composable<TodayDestination> {
            TodayRoute(onOpenMedications = dropUnlessResumed { navController.navigate(MedicationsDestination) })
        }
        composable<MedicationsDestination> { entry ->
            MedicationListRoute(
                onBack = dropUnlessResumed { navController.popBackStack() },
                // dropUnlessResumed only wraps no-argument lambdas, so check the entry directly.
                onOpenMedication = { id ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(MedicationDetailDestination(id))
                    }
                },
                onAddMedication = dropUnlessResumed { navController.navigate(MedicationEditDestination()) },
            )
        }
        composable<MedicationDetailDestination> { entry ->
            val id = entry.toRoute<MedicationDetailDestination>().id
            MedicationDetailRoute(
                medicationId = id,
                onBack = dropUnlessResumed { navController.popBackStack() },
                onEdit = dropUnlessResumed { navController.navigate(MedicationEditDestination(id)) },
                onOpenSchedule = { scheduleId ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(ScheduleEditDestination(id, scheduleId))
                    }
                },
                onAddSchedule = dropUnlessResumed { navController.navigate(ScheduleEditDestination(id)) },
            )
        }
        composable<MedicationEditDestination> { entry ->
            MedicationEditRoute(
                medicationId = entry.toRoute<MedicationEditDestination>().id,
                onDone = dropUnlessResumed { navController.popBackStack() },
                // Replace the add screen with the new medication's detail, so Back returns to the list.
                onAdded = { id ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(MedicationDetailDestination(id)) {
                            popUpTo<MedicationEditDestination> { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<ScheduleEditDestination> { entry ->
            val destination = entry.toRoute<ScheduleEditDestination>()
            ScheduleEditRoute(
                medicationId = destination.medicationId,
                scheduleId = destination.scheduleId,
                onDone = dropUnlessResumed { navController.popBackStack() },
            )
        }
    }
}
