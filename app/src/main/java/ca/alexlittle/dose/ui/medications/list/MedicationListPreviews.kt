package ca.alexlittle.dose.ui.medications.list

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ca.alexlittle.dose.ui.medications.MedicationPreviewData
import ca.alexlittle.dose.ui.theme.DoseTheme

@Preview(name = "Content")
@Preview(name = "Content, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Content, 200% font", fontScale = 2f)
@Composable
private fun MedicationListContentPreview() {
    DoseTheme {
        MedicationListScreen(
            uiState = MedicationListUiState.Content(
                active = listOf(MedicationPreviewData.eyeDrops, MedicationPreviewData.metformin),
                archived = listOf(MedicationPreviewData.amoxicillin),
            ),
            archivedExpanded = true,
            onToggleArchived = {},
            onBack = {},
            onOpenMedication = {},
            onAddMedication = {},
        )
    }
}

@Preview(name = "Empty")
@Preview(name = "Empty, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MedicationListEmptyPreview() {
    DoseTheme {
        MedicationListScreen(
            uiState = MedicationListUiState.Content(active = emptyList(), archived = emptyList()),
            archivedExpanded = false,
            onToggleArchived = {},
            onBack = {},
            onOpenMedication = {},
            onAddMedication = {},
        )
    }
}
