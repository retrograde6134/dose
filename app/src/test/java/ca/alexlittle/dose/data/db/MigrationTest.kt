package ca.alexlittle.dose.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DoseDatabase::class.java)

    @Test
    fun `1 to 2 keeps medications and adds an empty schedules table`() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO medications (id, name, strengthValue, strengthUnit, form, colorArgb, archived, createdAt)
                VALUES (7, 'Metformin', '500', 'mg', 'TABLET', 0, 0, 1760616000000)
                """,
            )
        }

        // Validates the resulting schema against 2.json, including the foreign key and index.
        helper.runMigrationsAndValidate(DB_NAME, 2, true).use { db ->
            db.query("SELECT name FROM medications WHERE id = 7").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Metformin", cursor.getString(0))
            }
            db.query("SELECT COUNT(*) FROM schedules").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
