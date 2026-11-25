@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package ru.player.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.io.File
import kotlin.math.max

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun tracksText(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    val word = if (m10 == 1 && m100 != 11) "трек"
    else if (m10 in 2..4 && m100 !in 12..14) "трека"
    else "треков"
    return "$n $word"
}

fun Track.coverModel(): Any? = cover?.let { File(it) }

@Composable
fun CoverImage(model: Any?, modifier: Modifier = Modifier) {
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp)
            .height(64.dp)
    ) {
        Box(Modifier.align(Alignment.CenterStart)) { startContent() }
        Text(
            title,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun BottomNavBar(selected: String, onSelect: (String) -> Unit) {
    val items: List<Triple<String, String, ImageVector>> = listOf(
        Triple("home", "Главная", Icons.Rounded.Home),
        Triple("playlists", "Плейлисты", Icons.Rounded.LibraryMusic),
        Triple("settings", "Настройки", Icons.Rounded.Settings)
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { (route, label, icon) ->
                NavItem(label, icon, route == selected) { onSelect(route) }
            }
        }
    }
}

@Composable
fun MiniPlayer(vm: PlayerViewModel, onClick: () -> Unit) {
    val t = vm.current ?: return
    val dur = max(vm.durationMs, t.durationMs).coerceAtLeast(1L)
    val fg = MaterialTheme.colorScheme.onPrimaryContainer
    val swipe = with(LocalDensity.current) { 48.dp.toPx() }
    val dragX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .graphicsLayer { translationX = dragX.value }
            .bounceClick(RoundedCornerShape(28.dp), onClick = onClick)
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = {
                        dx = 0f
                        dy = 0f
                    },
                    onDragEnd = {
                        if (abs(dy) > abs(dx) && dy < -swipe) onClick()
                        else if (abs(dx) > abs(dy) && dx < -swipe) vm.next()
                        else if (abs(dx) > abs(dy) && dx > swipe) vm.previous()
                        scope.launch { dragX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    },
                    onDragCancel = { scope.launch { dragX.animateTo(0f, spring()) } },
                    onDrag = { change, d ->
                        change.consume()
                        dx += d.x
                        dy += d.y
                        scope.launch { dragX.snapTo(dragX.value + d.x * 0.5f) }
                    }
                )
            },
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(t.coverModel(), Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = fg
                    )
                    Text(
                        t.artist.ifBlank { "Неизвестный исполнитель" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = fg
                    )
                }
                IconButton(onClick = { vm.togglePlay() }) {
                    Icon(
                        if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (vm.isPlaying) "Пауза" else "Играть",
                        tint = fg
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Следующий", tint = fg)
                }
            }
            LinearProgressIndicator(
                progress = { (vm.positionMs.toFloat() / dur).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    playing: Boolean,
    onClick: () -> Unit,
    actions: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val artist = track.artist.ifBlank { "Неизвестный исполнитель" }
    val highlight by animateColorAsState(
        if (playing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
        tween(350),
        label = "highlight"
    )
    ListItem(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .bounceClick(
                RoundedCornerShape(22.dp),
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu = true
                },
                onClick = onClick
            )
            .background(highlight),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            CoverImage(track.coverModel(), Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)))
        },
        headlineContent = {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (playing) FontWeight.Bold else FontWeight.SemiBold,
                color = if (playing) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        },
        supportingContent = {
            Text(
                if (track.durationMs > 0) "$artist • ${formatTime(track.durationMs)}" else artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Меню")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    actions.forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                menu = false
                                action()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PlaylistCard(playlist: Playlist, tracks: List<Track>, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(172.dp)
            .bounceClick(RoundedCornerShape(24.dp), onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(184.dp)) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 26.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp)
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(scheme.primary.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            )
            CoverImage(
                tracks.firstOrNull()?.coverModel(),
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(26.dp))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            playlist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        if (tracks.isEmpty()) {
            Text(
                "Пока пусто",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
        tracks.take(3).forEach { t ->
            Text(
                t.title,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
fun NewPlaylistCard(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(172.dp)
            .height(184.dp)
            .padding(top = 22.dp)
            .bounceClick(RoundedCornerShape(26.dp), onClick = onClick)
            .background(scheme.primaryContainer.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
            Text("Создать", color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    title: String = "Новый плейлист",
    initial: String = ""
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name.trim()) }) {
                Text("Готово")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun AddToPlaylistDialog(vm: PlayerViewModel, track: Track, onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreatePlaylistDialog(
            onDismiss = { creating = false },
            onCreate = { name ->
                vm.createPlaylist(name, listOf(track.id))
                onDismiss()
            }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в плейлист") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (vm.playlists.isEmpty()) Text("Плейлистов пока нет")
                vm.playlists.forEach { p ->
                    val has = track.id in p.trackIds
                    ListItem(
                        modifier = Modifier.clickable(enabled = !has) {
                            vm.addToPlaylist(p.id, listOf(track.id))
                            onDismiss()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(p.name) },
                        supportingContent = {
                            Text(if (has) "Уже в плейлисте" else tracksText(p.trackIds.size))
                        }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { creating = true }) { Text("Новый плейлист") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun PickTracksDialog(
    candidates: List<Track>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить треки") },
        text = {
            if (candidates.isEmpty()) {
                Text("Все треки уже в плейлисте")
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(candidates, key = { it.id }) { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (t.id in selected) selected - t.id else selected + t.id
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = t.id in selected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    t.artist.ifBlank { "Неизвестный исполнитель" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = { onConfirm(candidates.filter { it.id in selected }.map { it.id }) }
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun EditTrackSheet(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        artist: String,
        cover: Uri?,
        removeCover: Boolean,
        bg: Uri?,
        removeBg: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var pickedCover by remember { mutableStateOf<Uri?>(null) }
    var removeCover by remember { mutableStateOf(false) }
    var pickedBg by remember { mutableStateOf<Uri?>(null) }
    var removeBg by remember { mutableStateOf(false) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedCover = uri
            removeCover = false
        }
    }
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedBg = uri
            removeBg = false
        }
    }
    val coverModel: Any? = when {
        pickedCover != null -> pickedCover
        removeCover -> null
        else -> track.coverModel()
    }
    val bgModel: Any? = when {
        pickedBg != null -> pickedBg
        removeBg -> null
        else -> track.bg?.let { File(it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Редактирование", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            CoverImage(
                coverModel,
                Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { coverPicker.launch(arrayOf("image/*")) }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { coverPicker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Rounded.Image, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Обложка")
                }
                if (coverModel != null) {
                    TextButton(onClick = {
                        pickedCover = null
                        removeCover = true
                    }) { Text("Убрать") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Фон плеера для этого трека",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            CoverImage(
                bgModel,
                Modifier
                    .width(200.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { bgPicker.launch(arrayOf("image/*")) }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { bgPicker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Rounded.Image, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Фон")
                }
                if (bgModel != null) {
                    TextButton(onClick = {
                        pickedBg = null
                        removeBg = true
                    }) { Text("Убрать") }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Исполнитель") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    onSave(title.trim(), artist.trim(), pickedCover, removeCover, pickedBg, removeBg)
                }) { Text("Сохранить") }
            }
        }
    }
}
