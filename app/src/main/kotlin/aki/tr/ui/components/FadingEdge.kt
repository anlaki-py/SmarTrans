package aki.tr.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.composed

fun Modifier.fadingEdge(
    scrollState: ScrollState,
    length: Dp = 24.dp
): Modifier = composed {
    val lengthPx = with(LocalDensity.current) { length.toPx() }

    graphicsLayer { alpha = 0.99f }
        .drawWithContent {
            drawContent()
            if (scrollState.value > 0) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = lengthPx
                    ),
                    blendMode = BlendMode.DstIn,
                    size = Size(size.width, lengthPx)
                )
            }
            if (scrollState.value < scrollState.maxValue) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = size.height - lengthPx,
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn,
                    topLeft = Offset(0f, size.height - lengthPx),
                    size = Size(size.width, lengthPx)
                )
            }
        }
}
