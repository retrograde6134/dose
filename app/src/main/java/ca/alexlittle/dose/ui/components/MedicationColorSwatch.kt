package ca.alexlittle.dose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Decorative color dot. Callers always show the medication's name next to it. */
@Composable
internal fun MedicationColorSwatch(colorArgb: Int, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(colorArgb))
            // The outline keeps dark swatches visible on a dark background.
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}
