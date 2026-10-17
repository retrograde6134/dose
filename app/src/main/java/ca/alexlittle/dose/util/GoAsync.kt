package ca.alexlittle.dose.util

import android.content.BroadcastReceiver
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// A receiver that runs past about 10 seconds gets an ANR; stop well before that.
private val RECEIVER_BUDGET = 8.seconds

/** Runs [block] off the main thread while keeping the broadcast alive, bounded so it cannot ANR. */
internal fun BroadcastReceiver.goAsync(scope: CoroutineScope, block: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch {
        try {
            withTimeout(RECEIVER_BUDGET) { block() }
        } finally {
            pending.finish()
        }
    }
}
