@file:OptIn(ExperimentalMaterial3Api::class)

package ru.player.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(vm: PlayerViewModel, onBack: () -> Unit) {
    val track = vm.current
    if (track == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val settings = vm.settings
    var editing by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val dur = max(vm.durationMs, track.durationMs).coerceAtLeast(1L)
    val fraction = if (dragging) dragValue else (vm.positionMs.toFloat() / dur).coerceIn(0f, 1f)
    val corner by animateDpAsState(
        if (vm.isPlaying) 28.dp else 42.dp,
        spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "corner"
    )
    val coverScale by animateFloatAsState(
        if (vm.isPlaying) 1f else 0.9f,
        spring(dampingRatio = 0.6f, stiffness = 260f),
        label = "coverScale"
    )

    // какой фон показывать: свой у трека > настройка плеера
    val bgModel: Any? = when {
        track.bg != null -> File(track.bg)
        settings.bgStyle == 0 -> track.coverModel()
        settings.bgStyle == 1 -> settings.globalBg?.let { File(it) }
        else -> null
    }
    val hasBg = bgModel != null
    val scheme = MaterialTheme.colorScheme
    val onBg = if (hasBg) Color.White else scheme.onSurface
    val onBgMuted = if (hasBg) Color.White.copy(alpha = 0.75f) else scheme.onSurfaceVariant
    val accent = if (hasBg) Color.White else scheme.primary
    val shuffleTint by animateColorAsState(if (vm.shuffle) accent else onBgMuted, tween(300), label = "shuffleTint")
    val repeatTint by animateColorAsState(
        if (vm.repeatMode != Player.REPEAT_MODE_OFF) accent else onBgMuted,
        tween(300),
        label = "repeatTint"
    )

    // закрытие свайпом вниз и кнопкой «назад»
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val heightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val swipePx = with(density) { 90.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    val coverX = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }
    fun dismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            offsetY.animateTo(heightPx, tween(280, easing = FastOutLinearInEasing))
            onBack()
        }
    }
    BackHandler { dismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .graphicsLayer { alpha = 1f - (offsetY.value / heightPx).coerceIn(0f, 1f) * 0.5f }
            .background(scheme.background)
            .pointerInput(Unit) { detectTapGestures { } }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY.value > heightPx * 0.18f) dismiss()
                        else scope.launch { offsetY.animateTo(0f, spring(dampingRatio = 0.8f)) }
                    },
                    onDragCancel = { scope.launch { offsetY.animateTo(0f, spring()) } },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        scope.launch { offsetY.snapTo((offsetY.value + dy).coerceAtLeast(0f)) }
                    }
                )
            }
    ) {
        Crossfade(targetState = bgModel, animationSpec = tween(600), label = "bg") { m ->
            if (m != null) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = m,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.2f
                                scaleY = 1.2f
                            }
                            .blur(settings.blurDp.dp)
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                IconButton(
                    onClick = { dismiss() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Свернуть", tint = onBg)
                }
                Text(
                    "Сейчас играет",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onBg
                )
                IconButton(
                    onClick = { editing = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Изменить", tint = onBg)
                }
            }
            Spacer(Modifier.height(8.dp))

            // обложка: двойной тап - пауза, свайп влево/вправо - смена трека
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                        translationX = coverX.value
                        rotationZ = coverX.value / 40f
                    }
                    .shadow(20.dp, RoundedCornerShape(36.dp))
                    .clip(RoundedCornerShape(36.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { vm.togglePlay() })
                    }
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total < -swipePx) vm.next()
                                else if (total > swipePx) vm.previous()
                                scope.launch { coverX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                            },
                            onDragCancel = { scope.launch { coverX.animateTo(0f, spring()) } },
                            onHorizontalDrag = { change, dx ->
                                change.consume()
                                total += dx
                                scope.launch { coverX.snapTo(coverX.value + dx * 0.6f) }
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(380)) },
                    label = "cover"
                ) { t ->
                    CoverImage(t.coverModel(), Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.height(24.dp))
            AnimatedContent(
                targetState = track,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 4 }) togetherWith fadeOut(tween(180))
                },
                label = "titles"
            ) { t ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        t.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = onBg,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        t.artist.ifBlank { "Неизвестный исполнитель" },
                        style = MaterialTheme.typography.titleMedium,
                        color = onBgMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            WavySeekBar(
                fraction = fraction,
                playing = vm.isPlaying,
                color = accent,
                trackColor = accent.copy(alpha = 0.25f),
                onPreview = {
                    dragging = true
                    dragValue = it
                },
                onSeek = { f ->
                    vm.seekTo((f * dur).toLong())
                    dragging = false
                }
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(if (dragging) (dragValue * dur).toLong() else vm.positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = onBgMuted
                )
                Text(formatTime(dur), style = MaterialTheme.typography.labelMedium, color = onBgMuted)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Перемешать", tint = shuffleTint)
                }
                IconButton(onClick = { vm.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Предыдущий",
                        tint = onBg,
                        modifier = Modifier.size(36.dp)
                    )
                }
                FilledIconButton(
                    onClick = { vm.togglePlay() },
                    modifier = Modifier.size(84.dp),
                    shape = RoundedCornerShape(corner),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasBg) Color.White else scheme.primary,
                        contentColor = if (hasBg) Color.Black else scheme.onPrimary
                    )
                ) {
                    Crossfade(targetState = vm.isPlaying, animationSpec = tween(200), label = "playIcon") { playing ->
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) "Пауза" else "Играть",
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Следующий",
                        tint = onBg,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Повтор",
                        tint = repeatTint
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (editing) {
        EditTrackSheet(
            track = track,
            onDismiss = { editing = false },
            onSave = { title, artist, cover, removeCover, bg, removeBg ->
                vm.updateTrack(track.id, title, artist, cover, removeCover, bg, removeBg)
                editing = false
            }
        )
    }
}
