package ca.alexlittle.dose.ui.medications

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import ca.alexlittle.dose.R
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm

@StringRes
internal fun MedicationForm.labelRes(): Int = when (this) {
    MedicationForm.TABLET -> R.string.medication_form_tablet
    MedicationForm.CAPSULE -> R.string.medication_form_capsule
    MedicationForm.LIQUID -> R.string.medication_form_liquid
    MedicationForm.INJECTION -> R.string.medication_form_injection
    MedicationForm.DROPS -> R.string.medication_form_drops
    MedicationForm.INHALER -> R.string.medication_form_inhaler
    MedicationForm.OTHER -> R.string.medication_form_other
}

/** "500 mg", or null when no strength was entered. */
@Composable
internal fun Medication.strengthText(): String? {
    val locale = LocalConfiguration.current.locales[0]
    val value = strengthValue ?: return null
    return stringResource(R.string.medication_strength, formatStrength(value, locale), strengthUnit.orEmpty())
}

/** "500 mg · Tablet", or just "Tablet" when no strength was entered. */
@Composable
internal fun Medication.summaryText(): String {
    val form = stringResource(form.labelRes())
    val strength = strengthText() ?: return form
    return stringResource(R.string.medication_summary, strength, form)
}
