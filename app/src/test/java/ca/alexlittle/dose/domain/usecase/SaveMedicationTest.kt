package ca.alexlittle.dose.domain.usecase

import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.testing.FIXED_INSTANT
import ca.alexlittle.dose.testing.FakeMedicationRepository
import ca.alexlittle.dose.testing.draft
import ca.alexlittle.dose.testing.fixedClock
import ca.alexlittle.dose.testing.medication
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveMedicationTest {
    private val repository = FakeMedicationRepository()
    private val saveMedication = SaveMedication(repository, fixedClock())

    @Test
    fun `rejects invalid drafts with every applicable error`() = runTest {
        data class Case(val draft: MedicationDraft, val expected: Set<MedicationError>)

        val cases = listOf(
            Case(draft(name = ""), setOf(MedicationError.NAME_BLANK)),
            Case(draft(name = "   "), setOf(MedicationError.NAME_BLANK)),
            Case(draft(strengthValue = "0"), setOf(MedicationError.STRENGTH_NOT_POSITIVE)),
            Case(draft(strengthValue = "-1"), setOf(MedicationError.STRENGTH_NOT_POSITIVE)),
            Case(draft(strengthUnit = null), setOf(MedicationError.UNIT_MISSING)),
            Case(draft(strengthUnit = "  "), setOf(MedicationError.UNIT_MISSING)),
            Case(
                draft(name = "", strengthValue = "0", strengthUnit = ""),
                setOf(MedicationError.NAME_BLANK, MedicationError.STRENGTH_NOT_POSITIVE, MedicationError.UNIT_MISSING),
            ),
        )

        for (case in cases) {
            assertEquals(case.toString(), SaveMedicationResult.Invalid(case.expected), saveMedication(case.draft))
        }
        assertTrue(repository.all.isEmpty())
    }

    @Test
    fun `inserts a new medication with createdAt from the clock`() = runTest {
        val result = saveMedication(draft(form = MedicationForm.LIQUID))

        val saved = repository.all.single()
        assertEquals(SaveMedicationResult.Saved(saved.id), result)
        assertEquals("Metformin", saved.name)
        assertEquals(MedicationForm.LIQUID, saved.form)
        assertEquals(FIXED_INSTANT, saved.createdAt)
        assertEquals(false, saved.archived)
    }

    @Test
    fun `trims text and turns blank optional fields into null`() = runTest {
        saveMedication(draft(name = "  Metformin ", strengthUnit = " mg ", notes = "  ", instructions = " with food "))

        val saved = repository.all.single()
        assertEquals("Metformin", saved.name)
        assertEquals("mg", saved.strengthUnit)
        assertNull(saved.notes)
        assertEquals("with food", saved.instructions)
    }

    @Test
    fun `drops a unit that has no strength`() = runTest {
        saveMedication(draft(strengthValue = null, strengthUnit = "mg"))

        val saved = repository.all.single()
        assertNull(saved.strengthValue)
        assertNull(saved.strengthUnit)
    }

    @Test
    fun `normalizes trailing zeros and keeps fractions exact`() = runTest {
        val cases = mapOf("2.50" to "2.5", "0.5" to "0.5", "500.0" to "500", "750" to "750", "0.125" to "0.125")

        for ((input, expected) in cases) {
            val id = (saveMedication(draft(strengthValue = input)) as SaveMedicationResult.Saved).id
            // Equality with BigDecimal(expected) also checks scale, so this matches what Room reads back.
            assertEquals(input, BigDecimal(expected), repository.get(id)?.strengthValue)
        }
    }

    @Test
    fun `update keeps createdAt and archived state`() = runTest {
        val created = Instant.parse("2025-01-01T00:00:00Z")
        val id = repository.insert(medication(archived = true, createdAt = created))

        val result = saveMedication(draft(id = id, name = "Metformin XR", strengthValue = "750"))

        assertEquals(SaveMedicationResult.Saved(id), result)
        val updated = repository.get(id)
        assertEquals("Metformin XR", updated?.name)
        assertEquals(BigDecimal("750"), updated?.strengthValue)
        assertEquals(created, updated?.createdAt)
        assertEquals(true, updated?.archived)
    }

    @Test
    fun `update of a deleted medication reports not found and inserts nothing`() = runTest {
        assertEquals(SaveMedicationResult.NotFound, saveMedication(draft(id = 42)))
        assertTrue(repository.all.isEmpty())
    }
}
