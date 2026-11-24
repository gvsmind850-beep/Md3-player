package ru.player.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Волнистая полоса времени: пока играет - волна бежит, на паузе выравнивается в прямую.
 */
@Composable
fun WavySeekBar(
    fraction: Float,
    playing: Boolean,
    color: Color,
    trackColor: Color,
    onPreview: (Float) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val amplitude by animateFloatAsState(if (playing) 1f else 0f, tween(600), label = "amp")
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val currentPreview by rememberUpdatedState(onPreview)
    val currentSeek by rememberUpdatedState(onSeek)
    var dragFraction by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    currentSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        currentPreview(dragFraction)
                    },
                    onDragEnd = { currentSeek(dragFraction) },
                    onDragCancel = { currentSeek(dragFraction) },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        currentPreview(dragFraction)
                    }
                )
            }
    ) {
        val w = size.width
        val cy = size.height / 2f
        val stroke = 4.dp.toPx()
        val progressX = fraction.coerceIn(0f, 1f) * w
        val amp = amplitude * 5.dp.toPx()
        val waveLen = 30.dp.toPx()
        val twoPi = 2f * PI.toFloat()

        // непройденная часть - прямая линия
        drawLine(
            color = trackColor,
            start = Offset(progressX, cy),
            end = Offset(w, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // пройденная часть - волна
        val path = Path()
        path.moveTo(0f, cy + amp * sin(-phase))
        var x = 0f
        while (x < progressX) {
            path.lineTo(x, cy + amp * sin(twoPi * x / waveLen - phase))
            x += 3f
        }
        val endY = cy + amp * sin(twoPi * progressX / waveLen - phase)
        path.lineTo(progressX, endY)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // ползунок
        val thumbW = 5.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(progressX - thumbW / 2f, endY - 14.dp.toPx()),
            size = Size(thumbW, 28.dp.toPx()),
            cornerRadius = CornerRadius(thumbW / 2f)
        )
    }
}
