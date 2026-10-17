package ca.alexlittle.dose.ui.medications.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.alexlittle.dose.R
import ca.alexlittle.dose.domain.model.MedicationForm
import ca.alexlittle.dose.ui.components.BackButton
import ca.alexlittle.dose.ui.components.MedicationColorSwatch
import ca.alexlittle.dose.ui.medications.labelRes
import ca.alexlittle.dose.ui.theme.MedicationPalette

@Composable
internal fun MedicationEditRoute(
    onDone: () -> Unit,
    onAdded: (Long) -> Unit,
    viewModel: MedicationEditViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnAdded by rememberUpdatedState(onAdded)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MedicationEditEvent.Added -> currentOnAdded(event.id)
                MedicationEditEvent.Done -> currentOnDone()
            }
        }
    }
    MedicationEditScreen(
        uiState = uiState,
        actions = MedicationEditActions(
            onNameChange = viewModel::onNameChange,
            onStrengthChange = viewModel::onStrengthChange,
            onUnitChange = viewModel::onUnitChange,
            onFormChange = viewModel::onFormChange,
            onColorChange = viewModel::onColorChange,
            onNotesChange = viewModel::onNotesChange,
            onInstructionsChange = viewModel::onInstructionsChange,
            onSave = viewModel::onSave,
        ),
        onBack = onDone,
        modifier = modifier,
    )
}

@Composable
internal fun MedicationEditRoute(
    medicationId: Long,
    onDone: () -> Unit,
    onAdded: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<MedicationEditViewModel, MedicationEditViewModel.Factory> { it.create(medicationId) }
    MedicationEditRoute(onDone = onDone, onAdded = onAdded, viewModel = viewModel, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MedicationEditScreen(
    uiState: MedicationEditUiState,
    actions: MedicationEditActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState.isNew) R.string.medication_add_title else R.string.medication_edit_title
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    TextButton(onClick = actions.onSave, enabled = !uiState.loading && !uiState.saving) {
                        Text(stringResource(R.string.medication_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        // Loading is near-instant from a local database; a spinner would only flicker.
        if (uiState.loading) {
            Box(Modifier.padding(innerPadding).fillMaxSize())
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NameField(uiState, actions)
            StrengthFields(uiState, actions)
            FormPicker(selected = uiState.form, onSelect = actions.onFormChange)
            ColorPicker(selected = uiState.colorArgb, onSelect = actions.onColorChange)
            NotesFields(uiState, actions)
            Button(
                onClick = actions.onSave,
                enabled = !uiState.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.medication_save))
            }
        }
    }
}

@Composable
private fun NameField(uiState: MedicationEditUiState, actions: MedicationEditActions) {
    val error = MedicationEditError.NAME_BLANK in uiState.errors
    OutlinedTextField(
        value = uiState.name,
        onValueChange = actions.onNameChange,
        label = { Text(stringResource(R.string.medication_name_label)) },
        isError = error,
        supportingText = if (error) {
            { Text(stringResource(R.string.medication_error_name_blank)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StrengthFields(uiState: MedicationEditUiState, actions: MedicationEditActions) {
    val strengthError = when {
        MedicationEditError.STRENGTH_INVALID in uiState.errors -> R.string.medication_error_strength_invalid
        MedicationEditError.STRENGTH_NOT_POSITIVE in uiState.errors -> R.string.medication_error_strength_not_positive
        else -> null
    }
    val unitError = MedicationEditError.UNIT_MISSING in uiState.errors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Stacked rather than side by side so labels stay readable at 200% font scale.
        OutlinedTextField(
            value = uiState.strength,
            onValueChange = actions.onStrengthChange,
            label = { Text(stringResource(R.string.medication_strength_label)) },
            isError = strengthError != null,
            supportingText = strengthError?.let { { Text(stringResource(it)) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.unit,
            onValueChange = actions.onUnitChange,
            label = { Text(stringResource(R.string.medication_unit_label)) },
            isError = unitError,
            supportingText = if (unitError) {
                { Text(stringResource(R.string.medication_error_unit_missing)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stringArrayResource(R.array.medication_unit_suggestions).forEach { unit ->
                SuggestionChip(onClick = { actions.onUnitChange(unit) }, label = { Text(unit) })
            }
        }
    }
}

@Composable
private fun FormPicker(selected: MedicationForm, onSelect: (MedicationForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(R.string.medication_field_form)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            MedicationForm.entries.forEach { form ->
                FilterChip(
                    selected = form == selected,
                    onClick = { onSelect(form) },
                    label = { Text(stringResource(form.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(R.string.medication_field_color)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            MedicationPalette.colors.forEach { color ->
                val name = stringResource(color.nameRes)
                val isSelected = color.argb == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(color.argb) })
                        .semantics { contentDescription = name },
                ) {
                    MedicationColorSwatch(colorArgb = color.argb, size = 40.dp)
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = if (Color(color.argb).luminance() > LIGHT_SWATCH) Color.Black else Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesFields(uiState: MedicationEditUiState, actions: MedicationEditActions) {
    OutlinedTextField(
        value = uiState.instructions,
        onValueChange = actions.onInstructionsChange,
        label = { Text(stringResource(R.string.medication_instructions_label)) },
        placeholder = { Text(stringResource(R.string.medication_instructions_hint)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = uiState.notes,
        onValueChange = actions.onNotesChange,
        label = { Text(stringResource(R.string.medication_notes_label)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionLabel(@StringRes textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.titleSmall)
}

private const val LIGHT_SWATCH = 0.5f
