@file:OptIn(ExperimentalMaterial3Api::class)

package ru.player.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class GestureInfo(val id: String, val title: String, val hint: String)

private val gestureList = listOf(
    GestureInfo(
        "swipe_left", "Свайп влево",
        "По обложке или мини-плееру: следующий трек. По экрану: следующая вкладка."
    ),
    GestureInfo(
        "swipe_right", "Свайп вправо",
        "По обложке или мини-плееру: предыдущий трек. По экрану: предыдущая вкладка."
    ),
    GestureInfo("swipe_up", "Свайп вверх", "По мини-плееру: открыть полный плеер."),
    GestureInfo("swipe_down", "Свайп вниз", "В плеере: потянуть вниз и свернуть его."),
    GestureInfo("double_tap", "Двойной тап", "По обложке в плеере: пауза или воспроизведение."),
    GestureInfo("long_press", "Долгое нажатие", "По треку в списке: открыть меню действий.")
)

@Composable
fun GesturesScreen(onBack: () -> Unit) {
    var done by remember { mutableStateOf(setOf<String>()) }
    var last by remember { mutableStateOf<String?>(null) }
    val progress by animateFloatAsState(
        done.size / gestureList.size.toFloat(),
        tween(500),
        label = "progress"
    )

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Жесты",
            startContent = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            }
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Потренируйся на карточке ниже. Каждый жест, который получится, отметится в списке.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            GesturePad(
                label = gestureList.firstOrNull { it.id == last }?.title,
                onGesture = { id ->
                    last = id
                    done = done + id
                }
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Освоено: ${done.size} из ${gestureList.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(8.dp))
            gestureList.forEach { g -> GestureRow(g, g.id in done) }
            Spacer(Modifier.height(150.dp))
        }
    }
}

@Composable
private fun GestureRow(g: GestureInfo, learned: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tint by animateColorAsState(if (learned) scheme.primary else scheme.outline, tween(300), label = "tint")
    val scale by animateFloatAsState(
        if (learned) 1f else 0.85f,
        spring(dampingRatio = 0.4f, stiffness = 350f),
        label = "scale"
    )
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(
                if (learned) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
        },
        headlineContent = { Text(g.title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(g.hint) }
    )
}

@Composable
private fun GesturePad(label: String?, onGesture: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val threshold = with(density) { 60.dp.toPx() }
    val limit = with(density) { 40.dp.toPx() }
    val padX = remember { Animatable(0f) }
    val padY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun springBack() {
        scope.launch { padX.animateTo(0f, spring(dampingRatio = 0.5f)) }
        scope.launch { padY.animateTo(0f, spring(dampingRatio = 0.5f)) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(scheme.primaryContainer)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onGesture("double_tap") },
                    onLongPress = { onGesture("long_press") }
                )
            }
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = {
                        dx = 0f
                        dy = 0f
                    },
                    onDragEnd = {
                        val g = when {
                            abs(dx) < threshold && abs(dy) < threshold -> null
                            abs(dx) > abs(dy) -> if (dx < 0) "swipe_left" else "swipe_right"
                            else -> if (dy < 0) "swipe_up" else "swipe_down"
                        }
                        if (g != null) onGesture(g)
                        springBack()
                    },
                    onDragCancel = { springBack() },
                    onDrag = { change, d ->
                        change.consume()
                        dx += d.x
                        dy += d.y
                        scope.launch { padX.snapTo((padX.value + d.x * 0.5f).coerceIn(-limit, limit)) }
                        scope.launch { padY.snapTo((padY.value + d.y * 0.5f).coerceIn(-limit, limit)) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = padX.value
                    translationY = padY.value
                }
                .size(96.dp)
                .background(scheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.TouchApp,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
        AnimatedContent(
            targetState = label ?: "Попробуй любой жест здесь",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            transitionSpec = {
                (fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 }) togetherWith
                    (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 2 })
            },
            label = "padLabel"
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}
