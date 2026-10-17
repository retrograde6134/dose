package ca.alexlittle.dose.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.alexlittle.dose.R

@Composable
internal fun TodayRoute(
    onOpenMedications: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val problem = rememberReminderProblem()
    // Dismissed for this visit only: a reminder that cannot arrive is too important to hide for good.
    var dismissed by rememberSaveable { mutableStateOf(false) }
    TodayScreen(
        uiState = uiState,
        reminderProblem = problem.takeIf { uiState.hasReminders && !dismissed },
        onFixReminderProblem = { problem?.let { context.startActivity(it.settingsIntent(context)) } },
        onDismissReminderProblem = { dismissed = true },
        onOpenMedications = onOpenMedications,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    uiState: TodayUiState,
    reminderProblem: ReminderProblem?,
    onFixReminderProblem: () -> Unit,
    onDismissReminderProblem: () -> Unit,
    onOpenMedications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.today_title)) },
                actions = {
                    // A text label is clearer than an icon alone for many of this app's users.
                    TextButton(onClick = onOpenMedications) {
                        Text(stringResource(R.string.today_open_medications))
                    }
                },
            )
        },
    ) { innerPadding ->
        // Loading is near-instant from a local database; a spinner would only flicker.
        if (uiState.loading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            reminderProblem?.let {
                ReminderProblemBanner(problem = it, onFix = onFixReminderProblem, onDismiss = onDismissReminderProblem)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.today_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
