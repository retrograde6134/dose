package ca.alexlittle.dose.ui.today

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ca.alexlittle.dose.ui.theme.DoseTheme

@Preview(name = "Empty")
@Preview(name = "Empty, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Empty, 200% font", fontScale = 2f)
@Composable
private fun TodayScreenEmptyPreview() {
    DoseTheme {
        TodayScreen(uiState = TodayUiState.Empty, onOpenMedications = {})
    }
}
