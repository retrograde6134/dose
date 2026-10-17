package ca.alexlittle.dose.ui.medications

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class StrengthFormatTest {
    private val english = Locale.CANADA
    private val french = Locale.CANADA_FRENCH

    @Test
    fun `parses plain numbers with either separator in comma locales`() {
        val cases = listOf(
            Triple("500", english, "500"),
            Triple(" 0.5 ", english, "0.5"),
            Triple(".5", english, "0.5"),
            Triple("5.", english, "5"),
            Triple("0,5", french, "0.5"),
            Triple("0.5", french, "0.5"),
        )
        for ((text, locale, expected) in cases) {
            assertEquals("$text in $locale", 0, BigDecimal(expected).compareTo(parseStrength(text, locale)))
        }
    }

    @Test
    fun `rejects anything that is not a plain number`() {
        val cases = listOf("", "abc", "1,000", "1.2.3", "-1", "1e3", "5 mg", ".")
        for (text in cases) {
            assertEquals(text, null, parseStrength(text, english))
        }
    }

    @Test
    fun `formats with the locale decimal separator`() {
        assertEquals("0.5", formatStrength(BigDecimal("0.5"), english))
        assertEquals("0,5", formatStrength(BigDecimal("0.5"), french))
        assertEquals("500", formatStrength(BigDecimal("500"), french))
    }
}
