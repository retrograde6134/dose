package ca.alexlittle.dose.ui.theme

import androidx.annotation.StringRes
import ca.alexlittle.dose.R

internal data class MedicationColor(val argb: Int, @StringRes val nameRes: Int)

/** Colors offered when adding a medication. Each has a name so it is never conveyed by color alone. */
internal object MedicationPalette {
    val colors: List<MedicationColor> = listOf(
        MedicationColor(0xFFC62828.toInt(), R.string.medication_color_red),
        MedicationColor(0xFFEF6C00.toInt(), R.string.medication_color_orange),
        MedicationColor(0xFFF9A825.toInt(), R.string.medication_color_yellow),
        MedicationColor(0xFF2E7D32.toInt(), R.string.medication_color_green),
        MedicationColor(0xFF00838F.toInt(), R.string.medication_color_teal),
        MedicationColor(0xFF1565C0.toInt(), R.string.medication_color_blue),
        MedicationColor(0xFF3949AB.toInt(), R.string.medication_color_indigo),
        MedicationColor(0xFF6A1B9A.toInt(), R.string.medication_color_purple),
        MedicationColor(0xFFAD1457.toInt(), R.string.medication_color_pink),
        MedicationColor(0xFF6D4C41.toInt(), R.string.medication_color_brown),
        MedicationColor(0xFF546E7A.toInt(), R.string.medication_color_grey),
    )

    val default: MedicationColor = colors.first { it.nameRes == R.string.medication_color_blue }

    /** Name for a stored color; colors from an imported file may not be in the palette. */
    @StringRes
    fun nameRes(argb: Int): Int = colors.firstOrNull { it.argb == argb }?.nameRes ?: R.string.medication_color_custom
}
