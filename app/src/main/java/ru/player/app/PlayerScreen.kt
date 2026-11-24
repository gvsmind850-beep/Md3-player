@file:OptIn(ExperimentalMaterial3Api::class)

package ru.player.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.max

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
    val corner by animateDpAsState(if (vm.isPlaying) 28.dp else 42.dp, label = "corner")

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (bgModel != null) {
            AsyncImage(
                model = bgModel,
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
                    onClick = onBack,
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
            CoverImage(
                track.coverModel(),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(20.dp, RoundedCornerShape(36.dp))
                    .clip(RoundedCornerShape(36.dp))
            )
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = onBg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist.ifBlank { "Неизвестный исполнитель" },
                    style = MaterialTheme.typography.titleMedium,
                    color = onBgMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Перемешать",
                        tint = if (vm.shuffle) accent else onBgMuted
                    )
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
                    Icon(
                        if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (vm.isPlaying) "Пауза" else "Играть",
                        modifier = Modifier.size(44.dp)
                    )
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
                        tint = if (vm.repeatMode != Player.REPEAT_MODE_OFF) accent else onBgMuted
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
