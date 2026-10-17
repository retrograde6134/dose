package ca.alexlittle.dose.ui.schedules.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.alexlittle.dose.R
import ca.alexlittle.dose.ui.components.Stepper
import ca.alexlittle.dose.ui.medications.parseStrength
import ca.alexlittle.dose.ui.schedules.ScheduleFormatter
import java.time.LocalTime

@Composable
internal fun KindPicker(selected: ScheduleKind, onAction: (ScheduleEditAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(R.string.schedule_repeats_label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            ScheduleKind.entries.forEach { kind ->
                FilterChip(
                    selected = kind == selected,
                    onClick = { onAction(ScheduleEditAction.KindChange(kind)) },
                    label = { Text(stringResource(kind.labelRes())) },
                )
            }
        }
    }
}

/** The fields that only apply to the selected kind. */
@Composable
internal fun KindFields(
    uiState: ScheduleEditUiState,
    formatter: ScheduleFormatter,
    onAction: (ScheduleEditAction) -> Unit,
) {
    when (uiState.kind) {
        ScheduleKind.DAILY -> Unit

        ScheduleKind.WEEKDAYS -> WeekdayFields(uiState, formatter, onAction)

        ScheduleKind.INTERVAL -> Stepper(
            text = pluralStringResource(R.plurals.schedule_every_n_days, uiState.everyNDays, uiState.everyNDays),
            value = uiState.everyNDays,
            range = ScheduleLimits.DAYS,
            onValueChange = { onAction(ScheduleEditAction.EveryNDaysChange(it)) },
        )

        ScheduleKind.CYCLIC -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper(
                text = stringResource(R.string.schedule_cycle_on, formatter.days(uiState.cycleOnDays)),
                value = uiState.cycleOnDays,
                range = ScheduleLimits.DAYS,
                onValueChange = { onAction(ScheduleEditAction.CycleOnDaysChange(it)) },
            )
            Stepper(
                text = stringResource(R.string.schedule_cycle_off, formatter.days(uiState.cycleOffDays)),
                value = uiState.cycleOffDays,
                range = ScheduleLimits.DAYS,
                onValueChange = { onAction(ScheduleEditAction.CycleOffDaysChange(it)) },
            )
        }

        ScheduleKind.AS_NEEDED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(R.string.schedule_min_interval_label)
            val minutes = uiState.minIntervalMinutes
            Stepper(
                text = if (minutes == 0) {
                    stringResource(R.string.schedule_min_interval_none)
                } else {
                    formatter.duration(minutes)
                },
                value = minutes,
                range = ScheduleLimits.MIN_INTERVAL_MINUTES,
                step = ScheduleLimits.MIN_INTERVAL_STEP,
                onValueChange = { onAction(ScheduleEditAction.MinIntervalChange(it)) },
            )
        }
    }
}

@Composable
private fun WeekdayFields(
    uiState: ScheduleEditUiState,
    formatter: ScheduleFormatter,
    onAction: (ScheduleEditAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(R.string.schedule_weekdays_label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            formatter.weekOrder.forEach { day ->
                val fullName = formatter.fullDay(day)
                FilterChip(
                    selected = day in uiState.weekdays,
                    onClick = { onAction(ScheduleEditAction.WeekdayToggle(day)) },
                    label = { Text(formatter.shortDay(day)) },
                    modifier = Modifier.semantics { contentDescription = fullName },
                )
            }
        }
        if (ScheduleEditError.NO_WEEKDAYS in uiState.errors) ErrorText(R.string.schedule_error_no_weekdays)
        Stepper(
            text = pluralStringResource(R.plurals.schedule_every_n_weeks, uiState.everyNWeeks, uiState.everyNWeeks),
            value = uiState.everyNWeeks,
            range = ScheduleLimits.WEEKS,
            onValueChange = { onAction(ScheduleEditAction.EveryNWeeksChange(it)) },
        )
    }
}

@Composable
internal fun TimesSection(
    times: List<LocalTime>,
    showError: Boolean,
    formatter: ScheduleFormatter,
    onEditTime: (LocalTime) -> Unit,
    onRemoveTime: (LocalTime) -> Unit,
    onAddTime: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(R.string.schedule_times_label)
        times.forEach { time ->
            val text = formatter.time(time)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClickLabel = stringResource(R.string.schedule_change_time)) { onEditTime(time) }
                        .heightIn(min = 56.dp)
                        .padding(vertical = 12.dp),
                )
                IconButton(onClick = { onRemoveTime(time) }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.schedule_remove_time, text),
                    )
                }
            }
        }
        if (showError) ErrorText(R.string.schedule_error_no_times)
        OutlinedButton(
            onClick = onAddTime,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                stringResource(R.string.schedule_add_time),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
    }
}

@Composable
internal fun QuantityField(
    uiState: ScheduleEditUiState,
    formatter: ScheduleFormatter,
    onAction: (ScheduleEditAction) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val error = when {
        ScheduleEditError.QUANTITY_INVALID in uiState.errors -> R.string.schedule_error_quantity_invalid
        ScheduleEditError.QUANTITY_NOT_POSITIVE in uiState.errors -> R.string.schedule_error_quantity_not_positive
        else -> null
    }
    // Echo the amount back with its unit ("0.5 tablets") so the user sees what will be shown in reminders.
    val preview = parseStrength(uiState.quantity, locale)?.takeIf {
        it.signum() > 0
    }?.let { formatter.quantity(it, uiState.form) }
    OutlinedTextField(
        value = uiState.quantity,
        onValueChange = { onAction(ScheduleEditAction.QuantityChange(it)) },
        label = { Text(stringResource(R.string.schedule_quantity_label)) },
        isError = error != null,
        supportingText = when {
            error != null -> {
                { Text(stringResource(error)) }
            }

            preview != null -> {
                { Text(preview) }
            }

            else -> null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun DatesSection(
    uiState: ScheduleEditUiState,
    formatter: ScheduleFormatter,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onAction: (ScheduleEditAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DateRow(R.string.schedule_start_label, formatter.date(uiState.startDate), onClick = onEditStart)
        val end = uiState.endDate
        DateRow(
            labelRes = R.string.schedule_end_label,
            value = end?.let(formatter::date) ?: stringResource(R.string.schedule_end_never),
            onClick = onEditEnd,
            onClear = end?.let { { onAction(ScheduleEditAction.EndDateChange(null)) } },
        )
        if (ScheduleEditError.END_BEFORE_START in uiState.errors) ErrorText(R.string.schedule_error_end_before_start)
    }
}

@Composable
private fun DateRow(@StringRes labelRes: Int, value: String, onClick: () -> Unit, onClear: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = stringResource(R.string.schedule_change_date), onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        if (onClear != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(56.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.schedule_remove_end),
                )
            }
        }
    }
}

@Composable
internal fun SectionLabel(@StringRes textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun ErrorText(@StringRes textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@StringRes
private fun ScheduleKind.labelRes(): Int = when (this) {
    ScheduleKind.DAILY -> R.string.schedule_kind_daily
    ScheduleKind.WEEKDAYS -> R.string.schedule_kind_weekdays
    ScheduleKind.INTERVAL -> R.string.schedule_kind_interval
    ScheduleKind.CYCLIC -> R.string.schedule_kind_cyclic
    ScheduleKind.AS_NEEDED -> R.string.schedule_kind_as_needed
}
