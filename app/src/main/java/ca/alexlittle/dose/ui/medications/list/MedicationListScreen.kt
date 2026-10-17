package ca.alexlittle.dose.ui.medications.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.alexlittle.dose.R
import ca.alexlittle.dose.domain.model.Medication
import ca.alexlittle.dose.ui.components.BackButton
import ca.alexlittle.dose.ui.components.MedicationColorSwatch
import ca.alexlittle.dose.ui.medications.summaryText

@Composable
internal fun MedicationListRoute(
    onBack: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    MedicationListScreen(
        uiState = uiState,
        archivedExpanded = archivedExpanded,
        onToggleArchived = { archivedExpanded = !archivedExpanded },
        onBack = onBack,
        onOpenMedication = onOpenMedication,
        onAddMedication = onAddMedication,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MedicationListScreen(
    uiState: MedicationListUiState,
    archivedExpanded: Boolean,
    onToggleArchived: () -> Unit,
    onBack: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.medications_title)) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMedication,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.medications_add)) },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            // Loading is near-instant from a local database; a spinner would only flicker.
            MedicationListUiState.Loading -> Unit

            is MedicationListUiState.Content -> if (uiState.active.isEmpty() && uiState.archived.isEmpty()) {
                EmptyState(Modifier.padding(innerPadding))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    // Room for the FAB so it never covers the last row.
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    medicationRows(uiState.active, onOpenMedication)
                    if (uiState.archived.isNotEmpty()) {
                        archivedSection(
                            archived = uiState.archived,
                            showDivider = uiState.active.isNotEmpty(),
                            expanded = archivedExpanded,
                            onToggle = onToggleArchived,
                            onOpenMedication = onOpenMedication,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.medicationRows(medications: List<Medication>, onOpenMedication: (Long) -> Unit) {
    items(medications, key = { it.id }) { medication ->
        MedicationRow(medication = medication, onClick = { onOpenMedication(medication.id) })
    }
}

private fun LazyListScope.archivedSection(
    archived: List<Medication>,
    showDivider: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenMedication: (Long) -> Unit,
) {
    item(key = "archived-header") {
        if (showDivider) HorizontalDivider()
        val clickLabel = stringResource(
            if (expanded) R.string.medications_archived_hide else R.string.medications_archived_show,
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.medications_archived_header, archived.size)) },
            trailingContent = {
                Icon(
                    painter = painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable(onClickLabel = clickLabel, onClick = onToggle),
        )
    }
    if (expanded) medicationRows(archived, onOpenMedication)
}

@Composable
private fun MedicationRow(medication: Medication, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(medication.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(medication.summaryText()) },
        leadingContent = { MedicationColorSwatch(colorArgb = medication.colorArgb) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.medications_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
