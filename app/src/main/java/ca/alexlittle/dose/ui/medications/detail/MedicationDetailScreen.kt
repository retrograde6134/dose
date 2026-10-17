package ca.alexlittle.dose.ui.medications.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.alexlittle.dose.R
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.ui.components.BackButton
import ca.alexlittle.dose.ui.components.MedicationColorSwatch
import ca.alexlittle.dose.ui.medications.labelRes
import ca.alexlittle.dose.ui.medications.strengthText
import ca.alexlittle.dose.ui.theme.MedicationPalette

@Composable
internal fun MedicationDetailRoute(
    medicationId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<MedicationDetailViewModel, MedicationDetailViewModel.Factory> {
        it.create(medicationId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    val archivedMessage = stringResource(R.string.medication_archived_snackbar)
    val undoLabel = stringResource(R.string.medication_undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MedicationDetailEvent.Archived -> {
                    val result = snackbarHostState.showSnackbar(archivedMessage, actionLabel = undoLabel)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUnarchive()
                }
            }
        }
    }
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(uiState is MedicationDetailUiState.Missing) {
        if (uiState is MedicationDetailUiState.Missing) currentOnBack()
    }

    MedicationDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onArchive = viewModel::onArchive,
        onUnarchive = viewModel::onUnarchive,
        onDelete = { confirmingDelete = true },
        modifier = modifier,
    )

    val content = uiState as? MedicationDetailUiState.Content
    if (confirmingDelete && content != null) {
        DeleteDialog(
            name = content.medication.name,
            onConfirm = {
                confirmingDelete = false
                viewModel.onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MedicationDetailScreen(
    uiState: MedicationDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val medication = (uiState as? MedicationDetailUiState.Content)?.medication
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    if (medication != null) {
                        TextButton(onClick = onEdit) { Text(stringResource(R.string.medication_edit)) }
                    }
                },
            )
        },
    ) { innerPadding ->
        // Loading is near-instant from a local database; a spinner would only flicker.
        if (medication == null) return@Scaffold
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(medication)
            Details(medication)
            Actions(
                archived = medication.archived,
                onArchive = onArchive,
                onUnarchive = onUnarchive,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun Header(medication: Medication) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MedicationColorSwatch(colorArgb = medication.colorArgb, size = 32.dp)
        Column {
            Text(
                text = medication.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (medication.archived) {
                Text(
                    text = stringResource(R.string.medication_archived_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Details(medication: Medication) {
    medication.strengthText()?.let { Field(R.string.medication_field_strength, it) }
    Field(R.string.medication_field_form, stringResource(medication.form.labelRes()))
    Field(R.string.medication_field_color, stringResource(MedicationPalette.nameRes(medication.colorArgb)))
    medication.instructions?.let { Field(R.string.medication_field_instructions, it) }
    medication.notes?.let { Field(R.string.medication_field_notes, it) }
}

@Composable
private fun Field(@StringRes labelRes: Int, value: String) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Actions(archived: Boolean, onArchive: () -> Unit, onUnarchive: () -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val buttonModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
        OutlinedButton(onClick = if (archived) onUnarchive else onArchive, modifier = buttonModifier) {
            Text(stringResource(if (archived) R.string.medication_unarchive else R.string.medication_archive))
        }
        TextButton(onClick = onDelete, modifier = buttonModifier) {
            Text(stringResource(R.string.medication_delete), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medication_delete_title, name)) },
        text = { Text(stringResource(R.string.medication_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.medication_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.medication_delete_cancel)) }
        },
    )
}
