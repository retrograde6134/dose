package ca.alexlittle.dose.reminders

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import ca.alexlittle.dose.domain.model.DoseKey
import java.time.Instant

/**
 * A dose's identity as an intent data URI. Pending intents compare by data, so every dose gets its own
 * distinct notification actions without having to squeeze the identity into an int request code.
 */
internal object DoseIntents {
    private const val SCHEME = "dose"
    private const val AUTHORITY = "dose"

    fun uri(key: DoseKey): Uri = Uri.Builder()
        .scheme(SCHEME)
        .authority(AUTHORITY)
        .appendPath(key.scheduleId.toString())
        .appendPath(key.scheduledAt.toEpochMilli().toString())
        .build()

    fun key(intent: Intent): DoseKey? = intent.data?.let(::key)

    /** Notification tag for a dose. The notification id holds the medication id. */
    fun tag(key: DoseKey): String = uri(key).toString()

    fun key(tag: String?): DoseKey? = tag?.let { key(it.toUri()) }

    private fun key(uri: Uri): DoseKey? {
        val segments = uri.pathSegments
        val scheduleId = segments.getOrNull(0)?.toLongOrNull()
        val millis = segments.getOrNull(1)?.toLongOrNull()
        val ours = uri.scheme == SCHEME && uri.authority == AUTHORITY && segments.size == 2
        return if (ours && scheduleId != null && millis != null) {
            DoseKey(scheduleId, Instant.ofEpochMilli(millis))
        } else {
            null
        }
    }
}
