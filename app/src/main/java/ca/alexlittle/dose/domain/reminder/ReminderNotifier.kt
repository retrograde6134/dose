package ca.alexlittle.dose.domain.reminder

import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.schedule.DueDose

/** Shows and clears dose reminder notifications, one per dose. */
internal interface ReminderNotifier {
    /** Shows or replaces the reminder for [dose]. Does nothing if the user turned notifications off. */
    fun show(dose: DueDose, medication: Medication)

    fun isShowing(key: DoseKey): Boolean

    fun cancel(key: DoseKey)

    fun cancelForSchedule(scheduleId: Long)

    fun cancelForMedication(medicationId: Long)
}
