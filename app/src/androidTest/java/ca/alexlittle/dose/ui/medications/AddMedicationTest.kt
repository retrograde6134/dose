package ca.alexlittle.dose.ui.medications

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.alexlittle.dose.R
import ca.alexlittle.dose.data.db.DoseDatabase
import ca.alexlittle.dose.data.repository.RoomMedicationRepository
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.usecase.ObserveMedication
import ca.alexlittle.dose.domain.usecase.SaveMedication
import ca.alexlittle.dose.ui.medications.edit.MedicationEditRoute
import ca.alexlittle.dose.ui.medications.edit.MedicationEditViewModel
import ca.alexlittle.dose.ui.theme.DoseTheme
import java.math.BigDecimal
import java.time.Clock
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddMedicationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, DoseDatabase::class.java).build()
    private val repository = RoomMedicationRepository(database.medicationDao())

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addingAMedicationSavesItAndMovesOnToIt() {
        val viewModel = MedicationEditViewModel(
            medicationId = Medication.NEW_ID,
            observeMedication = ObserveMedication(repository),
            saveMedication = SaveMedication(repository, Clock.systemUTC()),
            locale = Locale.US,
        )
        var addedId: Long? = null
        composeRule.setContent {
            DoseTheme { MedicationEditRoute(onDone = {}, onAdded = { addedId = it }, viewModel = viewModel) }
        }

        textField(R.string.medication_name_label).performTextInput("Metformin")
        textField(R.string.medication_strength_label).performTextInput("0.5")
        composeRule.onNodeWithText("mg").performClick()
        composeRule.onNodeWithText(context.getString(R.string.medication_form_capsule)).performScrollTo().performClick()
        // The bottom Save, inside the scrolling form; the top bar has a second one.
        composeRule
            .onNode(hasText(context.getString(R.string.medication_save)) and hasAnyAncestor(hasScrollAction()))
            .performScrollTo()
            .performClick()

        composeRule.waitUntil { addedId != null }
        val saved = runBlocking { repository.get(checkNotNull(addedId)) }
        assertEquals("Metformin", saved?.name)
        assertEquals(BigDecimal("0.5"), saved?.strengthValue)
        assertEquals("mg", saved?.strengthUnit)
        assertEquals(MedicationForm.CAPSULE, saved?.form)
    }

    private fun textField(labelRes: Int) =
        composeRule.onNode(hasSetTextAction() and hasText(context.getString(labelRes)))
}
