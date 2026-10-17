package ca.alexlittle.dose.ui.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.alexlittle.dose.R
import ca.alexlittle.dose.ui.theme.DoseTheme
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
            DoseTheme { TodayScreen(uiState = TodayUiState.Empty, onOpenMedications = {}) }
        }

        composeRule.onNodeWithText(context.getString(R.string.today_empty)).assertIsDisplayed()
    }
}
