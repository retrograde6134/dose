package ca.alexlittle.dose.ui.today

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import ca.alexlittle.dose.R
import ca.alexlittle.dose.reminders.ReminderAccess

/** Re-checked each time the app comes back, so the banner goes away once the user fixes it in settings. */
@Composable
internal fun rememberReminderProblem(): ReminderProblem? {
    val context = LocalContext.current
    var problem by remember { mutableStateOf(reminderProblem(context)) }
    LifecycleResumeEffect(context) {
        problem = reminderProblem(context)
        onPauseOrDispose {}
    }
    return problem
}

private fun reminderProblem(context: Context): ReminderProblem? = when {
    !ReminderAccess.notificationsAllowed(context) -> ReminderProblem.NOTIFICATIONS_OFF
    !ReminderAccess.exactAlarmsAllowed(context) -> ReminderProblem.ALARMS_INEXACT
    else -> null
}

/** The system settings page that fixes [problem]. */
internal fun ReminderProblem.settingsIntent(context: Context) = when (this) {
    ReminderProblem.NOTIFICATIONS_OFF -> ReminderAccess.notificationSettings(context)
    ReminderProblem.ALARMS_INEXACT -> ReminderAccess.exactAlarmSettings(context)
}

/** Plain and dismissible: it says what is wrong and how to fix it, once per visit. */
@Composable
internal fun ReminderProblemBanner(
    problem: ReminderProblem,
    onFix: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, body, fix) = when (problem) {
        ReminderProblem.NOTIFICATIONS_OFF ->
            Triple(R.string.reminders_off_title, R.string.reminders_off_body, R.string.reminders_off_fix)

        ReminderProblem.ALARMS_INEXACT ->
            Triple(R.string.reminders_late_title, R.string.reminders_late_body, R.string.reminders_late_fix)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(body), style = MaterialTheme.typography.bodyLarge)
            // Wraps instead of clipping when large text makes the buttons too wide for one row.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminders_problem_dismiss)) }
                Button(onClick = onFix) { Text(stringResource(fix)) }
            }
        }
    }
}
