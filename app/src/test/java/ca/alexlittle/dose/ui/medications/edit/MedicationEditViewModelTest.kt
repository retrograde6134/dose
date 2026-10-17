package ca.alexlittle.dose.ui.medications.edit

import app.cash.turbine.test
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.usecase.ObserveMedication
import ca.alexlittle.dose.domain.usecase.SaveMedication
import ca.alexlittle.dose.testing.FakeMedicationRepository
import ca.alexlittle.dose.testing.fixedClock
import ca.alexlittle.dose.testing.medication
import ca.alexlittle.dose.util.MainDispatcherRule
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MedicationEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMedicationRepository()

    private fun viewModel(id: Long = Medication.NEW_ID, locale: Locale = Locale.CANADA) =
        MedicationEditViewModel(id, ObserveMedication(repository), SaveMedication(repository, fixedClock()), locale)

    @Test
    fun `new medication starts empty and not loading`() {
        val state = viewModel().uiState.value

        assertTrue(state.isNew)
        assertFalse(state.loading)
        assertEquals("", state.name)
    }

    @Test
    fun `existing medication loads into the form using the locale separator`() = runTest {
        val id = repository.insert(medication(name = "Metformin").copy(strengthValue = BigDecimal("0.5")))

        val state = viewModel(id, Locale.CANADA_FRENCH).uiState.value

        assertFalse(state.isNew)
        assertFalse(state.loading)
        assertEquals("Metformin", state.name)
        assertEquals("0,5", state.strength)
        assertEquals("mg", state.unit)
    }

    @Test
    fun `missing medication closes the screen`() = runTest {
        viewModel(id = 42).events.test {
            assertEquals(MedicationEditEvent.Done, awaitItem())
        }
    }

    @Test
    fun `valid save inserts and closes the screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onNameChange("Metformin")
        viewModel.onStrengthChange("500")
        viewModel.onUnitChange("mg")
        viewModel.onFormChange(MedicationForm.CAPSULE)

        viewModel.events.test {
            viewModel.onSave()
            assertEquals(MedicationEditEvent.Done, awaitItem())
        }
        val saved = repository.all.single()
        assertEquals("Metformin", saved.name)
        assertEquals(BigDecimal("500"), saved.strengthValue)
        assertEquals(MedicationForm.CAPSULE, saved.form)
    }

    @Test
    fun `second save tap while saving does not insert twice`() = runTest {
        val viewModel = viewModel()
        viewModel.onNameChange("Metformin")

        viewModel.onSave()
        viewModel.onSave()

        assertEquals(1, repository.all.size)
    }

    @Test
    fun `domain errors show on the form and nothing is saved`() = runTest {
        val viewModel = viewModel()
        viewModel.onStrengthChange("0")

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals(
            setOf(
                MedicationEditError.NAME_BLANK,
                MedicationEditError.STRENGTH_NOT_POSITIVE,
                MedicationEditError.UNIT_MISSING,
            ),
            state.errors,
        )
        assertFalse(state.saving)
        assertTrue(repository.all.isEmpty())
    }

    @Test
    fun `unparseable strength is reported with a blank name`() = runTest {
        val viewModel = viewModel()
        viewModel.onStrengthChange("1,000")

        viewModel.onSave()

        assertEquals(
            setOf(MedicationEditError.STRENGTH_INVALID, MedicationEditError.NAME_BLANK),
            viewModel.uiState.value.errors,
        )
        assertTrue(repository.all.isEmpty())
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val viewModel = viewModel()
        viewModel.onStrengthChange("abc")
        viewModel.onSave()

        viewModel.onNameChange("M")
        viewModel.onStrengthChange("5")

        assertEquals(emptySet<MedicationEditError>(), viewModel.uiState.value.errors)
    }
}
