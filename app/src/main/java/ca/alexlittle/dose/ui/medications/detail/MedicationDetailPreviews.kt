package ca.alexlittle.dose.ui.medications.detail

import android.content.res.Configuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ca.alexlittle.dose.ui.medications.MedicationPreviewData
import ca.alexlittle.dose.ui.theme.DoseTheme

@Preview(name = "Active")
@Preview(name = "Active, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Active, 200% font", fontScale = 2f)
@Composable
private fun MedicationDetailActivePreview() {
    DoseTheme {
        MedicationDetailScreen(
            uiState = MedicationDetailUiState.Content(MedicationPreviewData.metformin),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onEdit = {},
            onArchive = {},
            onUnarchive = {},
            onDelete = {},
        )
    }
}

@Preview(name = "Archived")
@Composable
private fun MedicationDetailArchivedPreview() {
    DoseTheme {
        MedicationDetailScreen(
            uiState = MedicationDetailUiState.Content(MedicationPreviewData.amoxicillin),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onEdit = {},
            onArchive = {},
            onUnarchive = {},
            onDelete = {},
        )
    }
}
