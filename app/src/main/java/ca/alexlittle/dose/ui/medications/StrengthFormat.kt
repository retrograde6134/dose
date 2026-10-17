package ca.alexlittle.dose.ui.medications

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

private val DECIMAL = Regex("""\d*\.?\d+|\d+\.""")

/**
 * Parses a strength typed by the user, accepting the locale's decimal separator or a period
 * (many decimal keyboards only offer a period). Grouping separators are not accepted, so "1,000"
 * in English is rejected rather than misread. Returns null if the text is not a plain number.
 */
internal fun parseStrength(text: String, locale: Locale): BigDecimal? {
    val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val normalized = text.trim().replace(separator, '.')
    return if (DECIMAL.matches(normalized)) BigDecimal(normalized) else null
}

internal fun formatStrength(value: BigDecimal, locale: Locale): String =
    value.toPlainString().replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
