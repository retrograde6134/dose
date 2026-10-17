package ca.alexlittle.dose.ui.medications.edit

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.ui.theme.DoseTheme

private val noActions = MedicationEditActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(name = "Add")
@Preview(name = "Add, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Add, 200% font", fontScale = 2f)
@Composable
private fun MedicationEditAddPreview() {
    DoseTheme {
        MedicationEditScreen(uiState = MedicationEditUiState(isNew = true), actions = noActions, onBack = {})
    }
}

@Preview(name = "Edit with errors")
@Preview(name = "Edit with errors, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MedicationEditErrorsPreview() {
    DoseTheme {
        MedicationEditScreen(
            uiState = MedicationEditUiState(
                isNew = false,
                name = "",
                strength = "abc",
                form = MedicationForm.CAPSULE,
                errors = setOf(MedicationEditError.NAME_BLANK, MedicationEditError.STRENGTH_INVALID),
            ),
            actions = noActions,
            onBack = {},
        )
    }
}
