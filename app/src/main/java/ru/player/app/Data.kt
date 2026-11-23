package ru.player.app

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val cover: String?,
    val durationMs: Long
)

data class AppSettings(
    val themeMode: Int = 0, // 0 - системная, 1 - светлая, 2 - тёмная
    val dynamicColor: Boolean = true,
    val hue: Float = 265f,
    val amoled: Boolean = false
)

class Repository(private val context: Context) {
    private val libFile = File(context.filesDir, "library.json")
    private val coversDir = File(context.filesDir, "covers").also { it.mkdirs() }
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun loadTracks(): List<Track> {
        if (!libFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(libFile.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Track(
                    id = o.getString("id"),
                    uri = o.getString("uri"),
                    title = o.getString("title"),
                    artist = o.getString("artist"),
                    cover = if (o.isNull("cover")) null else o.getString("cover"),
                    durationMs = o.optLong("duration", 0L)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTracks(list: List<Track>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("uri", t.uri)
            o.put("title", t.title)
            o.put("artist", t.artist)
            o.put("cover", t.cover ?: JSONObject.NULL)
            o.put("duration", t.durationMs)
            arr.put(o)
        }
        libFile.writeText(arr.toString())
    }

    fun loadSettings(): AppSettings = AppSettings(
        themeMode = prefs.getInt("themeMode", 0),
        dynamicColor = prefs.getBoolean("dynamic", true),
        hue = prefs.getFloat("hue", 265f),
        amoled = prefs.getBoolean("amoled", false)
    )

    fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putInt("themeMode", s.themeMode)
            .putBoolean("dynamic", s.dynamicColor)
            .putFloat("hue", s.hue)
            .putBoolean("amoled", s.amoled)
            .apply()
    }

    fun importTrack(uri: Uri): Track {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
        }
        var title: String? = null
        var artist: String? = null
        var duration = 0L
        var coverPath: String? = null
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val pic = mmr.embeddedPicture
            if (pic != null) {
                val f = File(coversDir, UUID.randomUUID().toString() + ".img")
                f.writeBytes(pic)
                coverPath = f.absolutePath
            }
        } catch (e: Exception) {
        } finally {
            try {
                mmr.release()
            } catch (e: Exception) {
            }
        }
        return Track(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            title = title?.takeIf { it.isNotBlank() } ?: displayName(uri),
            artist = artist?.takeIf { it.isNotBlank() } ?: "",
            cover = coverPath,
            durationMs = duration
        )
    }

    fun saveCover(uri: Uri): String? {
        return try {
            val f = File(coversDir, UUID.randomUUID().toString() + ".img")
            context.contentResolver.openInputStream(uri)?.use { input ->
                f.outputStream().use { out -> input.copyTo(out) }
            }
            if (f.exists() && f.length() > 0) f.absolutePath else null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteCover(path: String?) {
        if (path == null) return
        try {
            File(path).delete()
        } catch (e: Exception) {
        }
    }

    private fun displayName(uri: Uri): String {
        try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getString(0)
                    if (!n.isNullOrBlank()) return n.substringBeforeLast('.')
                }
            }
        } catch (e: Exception) {
        }
        return "Без названия"
    }
}
