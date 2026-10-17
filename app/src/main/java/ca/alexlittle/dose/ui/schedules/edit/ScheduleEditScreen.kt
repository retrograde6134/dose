package ca.alexlittle.dose.ui.schedules.edit

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.alexlittle.dose.R
import ca.alexlittle.dose.reminders.ReminderAccess
import ca.alexlittle.dose.ui.components.BackButton
import ca.alexlittle.dose.ui.schedules.rememberScheduleFormatter
import java.time.LocalTime

@Composable
internal fun ScheduleEditRoute(onDone: () -> Unit, viewModel: ScheduleEditViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    val context = LocalContext.current
    // Asked right after the user sets up a reminder, when the reason is obvious. The screen closes
    // whatever the answer; Today explains how to turn notifications on later.
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentOnDone()
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ScheduleEditEvent.Done -> currentOnDone()

                ScheduleEditEvent.SavedWithReminders -> if (ReminderAccess.needsNotificationPermission(context)) {
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    currentOnDone()
                }
            }
        }
    }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    ScheduleEditScreen(
        uiState = uiState,
        onAction = { action ->
            if (action == ScheduleEditAction.Delete) confirmingDelete = true else viewModel.onAction(action)
        },
        onBack = onDone,
        modifier = modifier,
    )
    if (confirmingDelete) {
        DeleteScheduleDialog(
            onConfirm = {
                confirmingDelete = false
                viewModel.onAction(ScheduleEditAction.Delete)
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
internal fun ScheduleEditRoute(
    medicationId: Long,
    scheduleId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ScheduleEditViewModel, ScheduleEditViewModel.Factory> {
        it.create(medicationId, scheduleId)
    }
    ScheduleEditRoute(onDone = onDone, viewModel = viewModel, modifier = modifier)
}

private enum class DateField { START, END }

private const val ADDING_TIME = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleEditScreen(
    uiState: ScheduleEditUiState,
    onAction: (ScheduleEditAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Minute of day of the time being changed, or ADDING_TIME. Plain types so they survive rotation.
    var editingTime by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingDate by rememberSaveable { mutableStateOf<DateField?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TopBar(uiState, onAction, onBack) },
    ) { innerPadding ->
        // Loading is near-instant from a local database; a spinner would only flicker.
        if (uiState.loading) {
            Box(Modifier.padding(innerPadding).fillMaxSize())
        } else {
            Form(
                uiState = uiState,
                onAction = onAction,
                onEditTime = {
                    editingTime =
                        it?.let { time -> time.hour * MINUTES_PER_HOUR + time.minute } ?: ADDING_TIME
                },
                onEditDate = { editingDate = it },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    editingTime?.let { minuteOfDay ->
        val old = minuteOfDay.takeIf { it != ADDING_TIME }?.let {
            LocalTime.of(
                it / MINUTES_PER_HOUR,
                it % MINUTES_PER_HOUR,
            )
        }
        TimeDialog(
            initial = old ?: nextTime(uiState.times),
            onConfirm = { time ->
                onAction(old?.let { ScheduleEditAction.ChangeTime(it, time) } ?: ScheduleEditAction.AddTime(time))
                editingTime = null
            },
            onDismiss = { editingTime = null },
        )
    }
    editingDate?.let { field ->
        val isStart = field == DateField.START
        DateDialog(
            initial = if (isStart) uiState.startDate else uiState.endDate ?: uiState.startDate,
            onConfirm = { date ->
                onAction(
                    if (isStart) ScheduleEditAction.StartDateChange(date) else ScheduleEditAction.EndDateChange(date),
                )
                editingDate = null
            },
            onDismiss = { editingDate = null },
        )
    }
}

/** [onEditTime] gets the time to change, or null to add one. */
@Composable
private fun Form(
    uiState: ScheduleEditUiState,
    onAction: (ScheduleEditAction) -> Unit,
    onEditTime: (LocalTime?) -> Unit,
    onEditDate: (DateField) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = rememberScheduleFormatter()
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        KindPicker(selected = uiState.kind, onAction = onAction)
        KindFields(uiState, formatter, onAction)
        if (uiState.kind != ScheduleKind.AS_NEEDED) {
            TimesSection(
                times = uiState.times,
                showError = ScheduleEditError.NO_TIMES in uiState.errors,
                formatter = formatter,
                onEditTime = onEditTime,
                onRemoveTime = { onAction(ScheduleEditAction.RemoveTime(it)) },
                onAddTime = { onEditTime(null) },
            )
        }
        QuantityField(uiState, formatter, onAction)
        DatesSection(
            uiState = uiState,
            formatter = formatter,
            onEditStart = { onEditDate(DateField.START) },
            onEditEnd = { onEditDate(DateField.END) },
            onAction = onAction,
        )
        Actions(isNew = uiState.isNew, saving = uiState.saving, onAction = onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(uiState: ScheduleEditUiState, onAction: (ScheduleEditAction) -> Unit, onBack: () -> Unit) {
    TopAppBar(
        title = { Title(uiState) },
        navigationIcon = { BackButton(onClick = onBack) },
        actions = {
            val enabled = !uiState.loading && !uiState.saving
            TextButton(onClick = { onAction(ScheduleEditAction.Save) }, enabled = enabled) {
                Text(stringResource(R.string.schedule_save))
            }
        },
    )
}

@Composable
private fun Title(uiState: ScheduleEditUiState) {
    Column {
        Text(stringResource(if (uiState.isNew) R.string.schedule_add_title else R.string.schedule_edit_title))
        if (uiState.medicationName.isNotEmpty()) {
            Text(
                text = uiState.medicationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Actions(isNew: Boolean, saving: Boolean, onAction: (ScheduleEditAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val buttonModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
        Button(onClick = { onAction(ScheduleEditAction.Save) }, enabled = !saving, modifier = buttonModifier) {
            Text(stringResource(R.string.schedule_save))
        }
        if (!isNew) {
            TextButton(onClick = { onAction(ScheduleEditAction.Delete) }, modifier = buttonModifier) {
                Text(stringResource(R.string.schedule_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// A sensible starting point for a new time: 8:00 for the first, otherwise four hours after the last.
private fun nextTime(times: List<LocalTime>): LocalTime =
    times.lastOrNull()?.plusHours(NEW_TIME_GAP_HOURS) ?: DEFAULT_FIRST_TIME

private const val MINUTES_PER_HOUR = 60
private const val NEW_TIME_GAP_HOURS = 4L
