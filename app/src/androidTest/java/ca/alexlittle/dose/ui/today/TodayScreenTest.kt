package ca.alexlittle.dose.ui.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.alexlittle.dose.R
import ca.alexlittle.dose.ui.theme.DoseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyStateIsShown() {
        composeRule.setContent {
            DoseTheme {
                TodayScreen(
                    uiState = TodayUiState(loading = false),
                    reminderProblem = null,
                    onFixReminderProblem = {},
                    onDismissReminderProblem = {},
                    onOpenMedications = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.today_empty)).assertIsDisplayed()
    }

    @Test
    fun notificationsOffBannerOffersAFixAndCanBeDismissed() {
        var fixes = 0
        var dismissals = 0
        composeRule.setContent {
            DoseTheme {
                TodayScreen(
                    uiState = TodayUiState(loading = false, hasReminders = true),
                    reminderProblem = ReminderProblem.NOTIFICATIONS_OFF,
                    onFixReminderProblem = { fixes++ },
                    onDismissReminderProblem = { dismissals++ },
                    onOpenMedications = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.reminders_off_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.reminders_off_fix)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.reminders_problem_dismiss)).performClick()

        assertEquals(1, fixes)
        assertEquals(1, dismissals)
    }
}
