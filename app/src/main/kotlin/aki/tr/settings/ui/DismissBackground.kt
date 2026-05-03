package aki.tr.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Red background revealed when swiping a language item to dismiss.
 * Shows a scaling delete icon.
 */
@Composable
fun DismissBackground(targetValue: SwipeToDismissBoxValue) {
    val color by animateColorAsState(
        targetValue = if (targetValue == SwipeToDismissBoxValue.EndToStart)
            MaterialTheme.colorScheme.errorContainer else Color.Transparent,
        label = "bgColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 0.8f,
        label = "iconScale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (targetValue == SwipeToDismissBoxValue.EndToStart) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.scale(scale),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
