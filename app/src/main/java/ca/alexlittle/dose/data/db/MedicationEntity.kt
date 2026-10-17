package ca.alexlittle.dose.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.alexlittle.dose.domain.model.MedicationForm
import java.math.BigDecimal
import java.time.Instant

// schedules (and the future dose_logs) reference id with ON DELETE CASCADE: deleting a
// medication removes its history, and archiving is how the user keeps it.
@Entity(tableName = "medications")
internal data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val strengthValue: BigDecimal?,
    val strengthUnit: String?,
    val form: MedicationForm,
    val colorArgb: Int,
    val notes: String?,
    val instructions: String?,
    @ColumnInfo(defaultValue = "0") val archived: Boolean,
    val createdAt: Instant,
)
