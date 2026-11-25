@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package ru.player.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(vm: PlayerViewModel, openPlayer: () -> Unit, openPlaylist: (String) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.addTracks(uris)
    }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Track?>(null) }
    var deleting by remember { mutableStateOf<Track?>(null) }
    var addingTo by remember { mutableStateOf<Track?>(null) }
    var creating by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    BackHandler(enabled = searching) {
        searching = false
        query = ""
    }
    LaunchedEffect(searching) {
        if (searching) {
            try {
                focus.requestFocus()
            } catch (e: Exception) {
            }
        }
    }

    val list = if (query.isBlank()) vm.tracks else vm.tracks.filter {
        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
    }
    val bottomPad = if (vm.current != null) 210.dp else 130.dp
    val scheme = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize()) {
        Crossfade(targetState = searching, animationSpec = tween(250), label = "header") { isSearching ->
            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        placeholder = { Text("Поиск по трекам") },
                        leadingIcon = {
                            IconButton(onClick = {
                                searching = false
                                query = ""
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Закрыть поиск")
                            }
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = scheme.surfaceContainerHigh,
                            unfocusedContainerColor = scheme.surfaceContainerHigh
                        )
                    )
                }
            } else {
                ScreenHeader(
                    title = "SAplay",
                    actions = {
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Поиск")
                        }
                        IconButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Добавить музыку")
                        }
                    }
                )
            }
        }

        if (vm.importing) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = bottomPad)
        ) {
            if (!searching) {
                item {
                    Column {
                        SectionLabel("Плейлисты")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(vm.playlists, key = { it.id }) { p ->
                                PlaylistCard(
                                    playlist = p,
                                    tracks = p.trackIds.mapNotNull { id -> vm.tracks.firstOrNull { it.id == id } },
                                    onClick = { openPlaylist(p.id) }
                                )
                            }
                            item { NewPlaylistCard { creating = true } }
                        }
                    }
                }
            }
            item {
                SectionLabel(
                    if (searching) "Результаты"
                    else "Все треки" + if (vm.tracks.isNotEmpty()) " · ${vm.tracks.size}" else ""
                )
            }
            if (vm.tracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Здесь пока пусто", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Нажми «+» сверху и выбери скачанную музыку из памяти телефона",
                            textAlign = TextAlign.Center,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            } else if (list.isEmpty()) {
                item {
                    Text(
                        "Ничего не найдено",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariant
                    )
                }
            } else {
                items(list, key = { it.id }) { t ->
                    TrackRow(
                        modifier = Modifier.animateItemPlacement(),
                        track = t,
                        playing = t.id == vm.currentId,
                        onClick = {
                            vm.play(t.id, list)
                            openPlayer()
                        },
                        actions = listOf(
                            "Изменить" to { editing = t },
                            "В плейлист" to { addingTo = t },
                            "Удалить" to { deleting = t }
                        )
                    )
                }
            }
        }
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

    addingTo?.let { t ->
        AddToPlaylistDialog(vm = vm, track = t, onDismiss = { addingTo = null })
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

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить трек?") },
            text = { Text("«${t.title}» уберётся из плеера. Сам файл останется на телефоне.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTrack(t.id)
                    deleting = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Отмена") }
            }
        )
    }
}
