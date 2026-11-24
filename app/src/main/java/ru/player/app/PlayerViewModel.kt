package ru.player.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(cover?.let { Uri.fromFile(File(it)) })
            .build()
    )
    .build()

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var settings by mutableStateOf(repo.loadSettings())
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentId by mutableStateOf<String?>(null)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var importing by mutableStateOf(false)
        private set

    val current: Track? get() = tracks.firstOrNull { it.id == currentId }

    private var controller: MediaController? = null
    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            updateTicker()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentId = mediaItem?.mediaId
            refreshPosition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshPosition()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffle = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(mode: Int) {
            repeatMode = mode
        }
    }

    init {
        tracks = repo.loadTracks()
        playlists = repo.loadPlaylists()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                isPlaying = c.isPlaying
                currentId = c.currentMediaItem?.mediaId
                shuffle = c.shuffleModeEnabled
                repeatMode = c.repeatMode
                updateTicker()
            } catch (e: Exception) {
            }
        }, MoreExecutors.directExecutor())
    }

    private fun refreshPosition() {
        val c = controller ?: return
        positionMs = c.currentPosition.coerceAtLeast(0L)
        durationMs = if (c.duration == C.TIME_UNSET) 0L else c.duration
    }

    private fun updateTicker() {
        ticker?.cancel()
        if (isPlaying) {
            ticker = viewModelScope.launch {
                while (isActive) {
                    refreshPosition()
                    delay(250)
                }
            }
        } else {
            refreshPosition()
        }
    }

    private fun persistPlaylists() {
        val snapshot = playlists
        viewModelScope.launch(Dispatchers.IO) { repo.savePlaylists(snapshot) }
    }

    // ---------- Библиотека ----------

    fun addTracks(uris: List<Uri>) {
        val fresh = uris.filter { u -> tracks.none { it.uri == u.toString() } }
        if (fresh.isEmpty()) return
        viewModelScope.launch {
            importing = true
            val added = withContext(Dispatchers.IO) { fresh.map { repo.importTrack(it) } }
            tracks = tracks + added
            val snapshot = tracks
            withContext(Dispatchers.IO) { repo.saveTracks(snapshot) }
            importing = false
        }
    }

    fun updateTrack(
        id: String,
        title: String,
        artist: String,
        newCover: Uri?,
        removeCover: Boolean,
        newBg: Uri?,
        removeBg: Boolean
    ) {
        viewModelScope.launch {
            val old = tracks.firstOrNull { it.id == id } ?: return@launch
            val coverPath: String? = withContext(Dispatchers.IO) {
                when {
                    newCover != null -> repo.saveCover(newCover) ?: old.cover
                    removeCover -> null
                    else -> old.cover
                }
            }
            val bgPath: String? = withContext(Dispatchers.IO) {
                when {
                    newBg != null -> repo.saveCover(newBg) ?: old.bg
                    removeBg -> null
                    else -> old.bg
                }
            }
            if (coverPath != old.cover) {
                withContext(Dispatchers.IO) { repo.deleteCover(old.cover) }
            }
            if (bgPath != old.bg) {
                withContext(Dispatchers.IO) { repo.deleteCover(old.bg) }
            }
            val updated = old.copy(
                title = title.ifBlank { old.title },
                artist = artist,
                cover = coverPath,
                bg = bgPath
            )
            tracks = tracks.map { if (it.id == id) updated else it }
            val snapshot = tracks
            withContext(Dispatchers.IO) { repo.saveTracks(snapshot) }
            controller?.let { c ->
                for (i in 0 until c.mediaItemCount) {
                    if (c.getMediaItemAt(i).mediaId == id) {
                        val wasCurrent = i == c.currentMediaItemIndex
                        val pos = c.currentPosition
                        c.replaceMediaItem(i, updated.toMediaItem())
                        if (wasCurrent) c.seekTo(i, pos)
                        break
                    }
                }
            }
        }
    }

    fun deleteTrack(id: String) {
        val old = tracks.firstOrNull { it.id == id } ?: return
        controller?.let { c ->
            for (i in c.mediaItemCount - 1 downTo 0) {
                if (c.getMediaItemAt(i).mediaId == id) c.removeMediaItem(i)
            }
        }
        tracks = tracks.filter { it.id != id }
        playlists = playlists.map { it.copy(trackIds = it.trackIds.filter { t -> t != id }) }
        val snapshot = tracks
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteCover(old.cover)
            repo.deleteCover(old.bg)
            repo.saveTracks(snapshot)
        }
        persistPlaylists()
    }

    // ---------- Плейлисты ----------

    fun createPlaylist(name: String, trackIds: List<String> = emptyList()): String {
        val p = Playlist(UUID.randomUUID().toString(), name, trackIds)
        playlists = playlists + p
        persistPlaylists()
        return p.id
    }

    fun renamePlaylist(id: String, name: String) {
        playlists = playlists.map { if (it.id == id) it.copy(name = name) else it }
        persistPlaylists()
    }

    fun deletePlaylist(id: String) {
        playlists = playlists.filter { it.id != id }
        persistPlaylists()
    }

    fun addToPlaylist(id: String, trackIds: List<String>) {
        playlists = playlists.map { p ->
            if (p.id == id) p.copy(trackIds = p.trackIds + trackIds.filter { it !in p.trackIds }) else p
        }
        persistPlaylists()
    }

    fun removeFromPlaylist(id: String, trackId: String) {
        playlists = playlists.map { p ->
            if (p.id == id) p.copy(trackIds = p.trackIds.filter { it != trackId }) else p
        }
        persistPlaylists()
    }

    // ---------- Воспроизведение ----------

    private fun queueMatches(c: MediaController, queue: List<Track>): Boolean {
        if (c.mediaItemCount != queue.size) return false
        for (i in queue.indices) {
            if (c.getMediaItemAt(i).mediaId != queue[i].id) return false
        }
        return true
    }

    fun play(id: String, queue: List<Track> = tracks) {
        val c = controller ?: return
        val index = queue.indexOfFirst { it.id == id }
        if (index < 0) return
        if (queueMatches(c, queue)) {
            if (id == currentId) {
                if (!c.isPlaying) c.play()
                return
            }
            c.seekTo(index, 0L)
        } else {
            c.setMediaItems(queue.map { it.toMediaItem() }, index, 0L)
            c.prepare()
        }
        c.play()
    }

    fun playQueue(queue: List<Track>) {
        val first = queue.firstOrNull() ?: return
        play(first.id, queue)
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // ---------- Настройки ----------

    fun updateSettings(s: AppSettings) {
        settings = s
        repo.saveSettings(s)
    }

    fun setGlobalBg(uri: Uri?) {
        viewModelScope.launch {
            val old = settings.globalBg
            val path: String? = if (uri == null) null else withContext(Dispatchers.IO) { repo.saveCover(uri) }
            if (uri != null && path == null) return@launch
            if (old != null) withContext(Dispatchers.IO) { repo.deleteCover(old) }
            updateSettings(settings.copy(globalBg = path))
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
