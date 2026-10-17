package ca.alexlittle.dose.data.db

import androidx.room.TypeConverter
import ca.alexlittle.dose.domain.model.DoseStatus
import ca.alexlittle.dose.domain.model.MedicationForm
import java.math.BigDecimal
import java.time.Instant

internal class Converters {
    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    // Stored as text so quantities like 0.5 survive exactly; REAL would round-trip through a double.
    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun formToString(form: MedicationForm): String = form.name

    @TypeConverter
    fun stringToForm(value: String): MedicationForm = MedicationForm.valueOf(value)

    @TypeConverter
    fun doseStatusToString(status: DoseStatus): String = status.name

    @TypeConverter
    fun stringToDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)
}
