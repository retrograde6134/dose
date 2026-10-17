package ca.alexlittle.dose.ui.navigation

import ca.alexlittle.dose.domain.model.Medication
import kotlinx.serialization.Serializable

@Serializable
internal data object MedicationsDestination

@Serializable
internal data class MedicationDetailDestination(val id: Long)

/** [id] is [Medication.NEW_ID] when adding a medication. */
@Serializable
internal data class MedicationEditDestination(val id: Long = Medication.NEW_ID)
