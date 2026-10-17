package cu.todus.download.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Barra de progreso con degradado. [progress] entre 0 y 1; null = indeterminada (segmento que viaja).
 * Con [shimmer] pasa un destello por la parte rellena mientras se descarga.
 */
@Composable
fun FancyProgress(
    progress: Float?,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    shimmer: Boolean = false
) {
    val animated by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val moving = shimmer || progress == null
    val phase: Float = if (moving) {
        val transition = rememberInfiniteTransition(label = "progress-phase")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
            label = "phase"
        ).value
    } else {
        0f
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = radius)

        if (progress == null) {
            val segment = size.width * 0.32f
            val x = (size.width + segment) * phase - segment
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color), startX = x, endX = x + segment),
                topLeft = Offset(x, 0f),
                size = Size(segment, size.height),
                cornerRadius = radius
            )
            return@Canvas
        }

        val filled = size.width * animated
        if (filled > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.72f), color), startX = 0f, endX = filled.coerceAtLeast(1f)),
                size = Size(filled, size.height),
                cornerRadius = radius
            )
            if (shimmer) {
                val width = size.width * 0.25f
                val x = (filled + width) * phase - width
                clipRect(left = 0f, top = 0f, right = filled, bottom = size.height) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent),
                            startX = x,
                            endX = x + width
                        ),
                        topLeft = Offset(x, 0f),
                        size = Size(width, size.height)
                    )
                }
            }
        }
    }
}
