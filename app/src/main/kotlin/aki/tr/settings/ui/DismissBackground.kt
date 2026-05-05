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
import androidx.compose.material3.Material3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Red background revealed when swiping a language item to dismiss.
 * Shows a scaling delete icon.
 */
@Material3ExpressiveApi
@Composable
fun DismissBackground(dismissValue: SwipeToDismissBoxValue) {
    val isSettledDismissed = dismissValue == SwipeToDismissBoxValue.EndToStart
    val color by animateColorAsState(
        targetValue = if (isSettledDismissed)
            MaterialTheme.colorScheme.errorContainer else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "bgColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSettledDismissed) 1.2f else 0.8f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "iconScale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isSettledDismissed) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.scale(scale),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
