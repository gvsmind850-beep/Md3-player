@file:OptIn(ExperimentalMaterial3Api::class)

package ru.player.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PlaylistsScreen(vm: PlayerViewModel, openPlaylist: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    val bottomPad = if (vm.current != null) 210.dp else 130.dp

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Плейлисты",
            actions = {
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Новый плейлист")
                }
            }
        )
        if (vm.playlists.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Плейлистов пока нет", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Нажми «+», чтобы создать первый",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = bottomPad)) {
                items(vm.playlists, key = { it.id }) { p ->
                    val tracks = p.trackIds.mapNotNull { id -> vm.tracks.firstOrNull { it.id == id } }
                    ListItem(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { openPlaylist(p.id) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            CoverImage(
                                tracks.firstOrNull()?.coverModel(),
                                Modifier.size(68.dp).clip(RoundedCornerShape(18.dp))
                            )
                        },
                        headlineContent = {
                            Text(p.name, fontWeight = FontWeight.Bold, maxLines = 1)
                        },
                        supportingContent = { Text(tracksText(tracks.size)) }
                    )
                }
            }
        }
    }

    if (creating) {
        CreatePlaylistDialog(
            onDismiss = { creating = false },
            onCreate = { name ->
                vm.createPlaylist(name)
                creating = false
            }
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    vm: PlayerViewModel,
    id: String,
    onBack: () -> Unit,
    openPlayer: () -> Unit
) {
    val pl = vm.playlists.firstOrNull { it.id == id }
    if (pl == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val tracks = pl.trackIds.mapNotNull { tid -> vm.tracks.firstOrNull { it.id == tid } }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Track?>(null) }
    val bottomPad = if (vm.current != null) 210.dp else 130.dp

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = pl.name,
            startContent = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Добавить треки")
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Переименовать") },
                            onClick = {
                                menu = false
                                renaming = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить плейлист") },
                            onClick = {
                                menu = false
                                confirmDelete = true
                            }
                        )
                    }
                }
            }
        )
        LazyColumn(contentPadding = PaddingValues(bottom = bottomPad)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CoverImage(
                        tracks.firstOrNull()?.coverModel(),
                        Modifier.size(200.dp).clip(RoundedCornerShape(32.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tracksText(tracks.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        enabled = tracks.isNotEmpty(),
                        onClick = {
                            vm.playQueue(tracks)
                            openPlayer()
                        }
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Воспроизвести")
                    }
                }
            }
            if (tracks.isEmpty()) {
                item {
                    Text(
                        "В плейлисте пока пусто. Нажми «+» сверху, чтобы добавить треки.",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(tracks, key = { it.id }) { t ->
                TrackRow(
                    track = t,
                    playing = t.id == vm.currentId,
                    onClick = {
                        vm.play(t.id, tracks)
                        openPlayer()
                    },
                    actions = listOf(
                        "Убрать из плейлиста" to { vm.removeFromPlaylist(pl.id, t.id) },
                        "Изменить" to { editing = t }
                    )
                )
            }
        }
    }

    if (adding) {
        PickTracksDialog(
            candidates = vm.tracks.filter { it.id !in pl.trackIds },
            onDismiss = { adding = false },
            onConfirm = { ids ->
                vm.addToPlaylist(pl.id, ids)
                adding = false
            }
        )
    }

    if (renaming) {
        CreatePlaylistDialog(
            title = "Переименовать",
            initial = pl.name,
            onDismiss = { renaming = false },
            onCreate = { name ->
                vm.renamePlaylist(pl.id, name)
                renaming = false
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить плейлист?") },
            text = { Text("Сами треки останутся в библиотеке.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deletePlaylist(pl.id)
                    onBack()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            }
        )
    }

    editing?.let { t ->
        EditTrackSheet(
            track = t,
            onDismiss = { editing = null },
            onSave = { title, artist, cover, removeCover, bg, removeBg ->
                vm.updateTrack(t.id, title, artist, cover, removeCover, bg, removeBg)
                editing = null
            }
        )
    }
}
