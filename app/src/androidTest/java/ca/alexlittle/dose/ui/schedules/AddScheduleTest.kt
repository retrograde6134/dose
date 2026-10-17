package ca.alexlittle.dose.ui.schedules

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.alexlittle.dose.R
import ca.alexlittle.dose.data.db.DoseDatabase
import ca.alexlittle.dose.data.repository.RoomDoseLogRepository
import ca.alexlittle.dose.data.repository.RoomMedicationRepository
import ca.alexlittle.dose.data.repository.RoomScheduleRepository
import ca.alexlittle.dose.domain.model.DoseKey
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.domain.model.Recurrence
import ca.alexlittle.dose.domain.model.Schedule
import ca.alexlittle.dose.domain.reminder.ReminderAlarms
import ca.alexlittle.dose.domain.reminder.ReminderNotifier
import ca.alexlittle.dose.domain.schedule.DueDose
import ca.alexlittle.dose.domain.usecase.DeleteMedication
import ca.alexlittle.dose.domain.usecase.DeleteSchedule
import ca.alexlittle.dose.domain.usecase.ObserveMedication
import ca.alexlittle.dose.domain.usecase.ObserveSchedule
import ca.alexlittle.dose.domain.usecase.ObserveSchedules
import ca.alexlittle.dose.domain.usecase.RescheduleReminders
import ca.alexlittle.dose.domain.usecase.SaveSchedule
import ca.alexlittle.dose.domain.usecase.SetMedicationArchived
import ca.alexlittle.dose.ui.medications.detail.MedicationDetailScreen
import ca.alexlittle.dose.ui.medications.detail.MedicationDetailViewModel
import ca.alexlittle.dose.ui.schedules.edit.ScheduleEditRoute
import ca.alexlittle.dose.ui.schedules.edit.ScheduleEditViewModel
import ca.alexlittle.dose.ui.theme.DoseTheme
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddScheduleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, DoseDatabase::class.java).build()
    private val medications = RoomMedicationRepository(database.medicationDao())
    private val schedules = RoomScheduleRepository(database.scheduleDao())
    private val clock = Clock.systemDefaultZone()

    // Real alarms and notifications are out of scope here; the reminder pipeline has its own tests.
    private val notifier = object : ReminderNotifier {
        override fun show(dose: DueDose, medication: Medication) = Unit

        override fun isShowing(key: DoseKey) = false

        override fun cancel(key: DoseKey) = Unit

        override fun cancelForSchedule(scheduleId: Long) = Unit

        override fun cancelForMedication(medicationId: Long) = Unit
    }
    private val reschedule = RescheduleReminders(
        schedules,
        RoomDoseLogRepository(database.doseLogDao()),
        medications,
        object : ReminderAlarms {
            override fun arm(at: Instant) = Unit

            override fun cancel() = Unit
        },
        notifier,
        clock,
    )

    // Otherwise saving a schedule with reminders stops at the system notification-permission dialog.
    @Before
    fun grantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addingADailyScheduleWithTwoTimesShowsItsSummaryOnDetail() {
        val medicationId = runBlocking { medications.insert(metformin()) }
        val editViewModel = ScheduleEditViewModel(
            medicationId = medicationId,
            scheduleId = Schedule.NEW_ID,
            observeMedication = ObserveMedication(medications),
            observeSchedule = ObserveSchedule(schedules),
            saveSchedule = SaveSchedule(schedules, medications, reschedule),
            deleteSchedule = DeleteSchedule(schedules, notifier, reschedule),
            locale = context.resources.configuration.locales[0],
            clock = clock,
        )
        val detailViewModel = MedicationDetailViewModel(
            medicationId = medicationId,
            observeMedication = ObserveMedication(medications),
            observeSchedules = ObserveSchedules(schedules),
            clock = clock,
            setMedicationArchived = SetMedicationArchived(medications, notifier, reschedule),
            deleteMedication = DeleteMedication(medications, notifier, reschedule),
        )
        var showingDetail by mutableStateOf(false)
        composeRule.setContent {
            DoseTheme {
                if (showingDetail) {
                    val uiState by detailViewModel.uiState.collectAsStateWithLifecycle()
                    MedicationDetailScreen(uiState, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {})
                } else {
                    ScheduleEditRoute(onDone = { showingDetail = true }, viewModel = editViewModel)
                }
            }
        }

        // The form starts as every day at 8:00; a second time defaults to four hours later.
        composeRule.onNodeWithText(string(R.string.schedule_add_time)).performScrollTo().performClick()
        composeRule.onNode(hasText(string(R.string.schedule_picker_ok)) and hasAnyAncestor(isDialog())).performClick()
        // The bottom Save, inside the scrolling form; the top bar has a second one.
        composeRule
            .onNode(hasText(string(R.string.schedule_save)) and hasAnyAncestor(hasScrollAction()))
            .performScrollTo()
            .performClick()

        composeRule.waitUntil { showingDetail }
        val saved = runBlocking { schedules.get(1) }
        assertEquals(Recurrence.Daily, saved?.recurrence)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0)), saved?.timesOfDay)
        assertEquals(BigDecimal.ONE, saved?.doseQuantity)

        val formatter = ScheduleFormatter(
            context.resources,
            context.resources.configuration.locales[0],
            DateFormat.is24HourFormat(context),
        )
        val summary = formatter.summary(checkNotNull(saved), MedicationForm.TABLET)
        composeRule.waitUntil { composeRule.onAllNodes(hasText(summary)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun string(res: Int) = context.getString(res)

    private fun metformin() = Medication(
        name = "Metformin",
        strengthValue = BigDecimal("500"),
        strengthUnit = "mg",
        form = MedicationForm.TABLET,
        colorArgb = 0xFF1565C0.toInt(),
        notes = null,
        instructions = null,
        archived = false,
        createdAt = Instant.parse("2026-10-16T12:00:00Z"),
    )
}
