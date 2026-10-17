package ca.alexlittle.dose.ui.schedules.edit

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.ui.medications.MedicationPreviewData
import ca.alexlittle.dose.ui.theme.DoseTheme
import java.time.DayOfWeek
import java.time.LocalTime

private val daily = ScheduleEditUiState(
    isNew = true,
    startDate = MedicationPreviewData.today,
    loading = false,
    medicationName = "Metformin",
    times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
)

@Preview(name = "Add, daily")
@Preview(name = "Add, daily, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Add, daily, 200% font", fontScale = 2f)
@Composable
private fun ScheduleEditDailyPreview() {
    DoseTheme { ScheduleEditScreen(uiState = daily, onAction = {}, onBack = {}) }
}

@Preview(name = "Edit, weekdays with errors")
@Preview(name = "Edit, weekdays with errors, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScheduleEditWeekdaysPreview() {
    DoseTheme {
        ScheduleEditScreen(
            uiState = daily.copy(
                isNew = false,
                kind = ScheduleKind.WEEKDAYS,
                weekdays = emptySet(),
                everyNWeeks = 2,
                times = emptyList(),
                quantity = "0",
                endDate = MedicationPreviewData.today.minusDays(1),
                errors = setOf(
                    ScheduleEditError.NO_WEEKDAYS,
                    ScheduleEditError.NO_TIMES,
                    ScheduleEditError.QUANTITY_NOT_POSITIVE,
                    ScheduleEditError.END_BEFORE_START,
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Cycle")
@Composable
private fun ScheduleEditCyclicPreview() {
    DoseTheme {
        ScheduleEditScreen(
            uiState = daily.copy(kind = ScheduleKind.CYCLIC, weekdays = setOf(DayOfWeek.MONDAY), quantity = "0.5"),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "As needed")
@Preview(name = "As needed, 200% font", fontScale = 2f)
@Composable
private fun ScheduleEditAsNeededPreview() {
    DoseTheme {
        ScheduleEditScreen(
            uiState = daily.copy(
                kind = ScheduleKind.AS_NEEDED,
                form = MedicationForm.INHALER,
                minIntervalMinutes = 240,
                quantity = "2",
            ),
            onAction = {},
            onBack = {},
        )
    }
}
