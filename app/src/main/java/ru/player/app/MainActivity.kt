package ru.player.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App(vm: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    PlayerTheme(vm.settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var tab by rememberSaveable { mutableStateOf("home") }
            var playlistId by rememberSaveable { mutableStateOf<String?>(null) }
            var showPlayer by rememberSaveable { mutableStateOf(false) }

            BackHandler(enabled = playlistId != null) { playlistId = null }
            BackHandler(enabled = playlistId == null && tab != "home") { tab = "home" }

            Box(Modifier.fillMaxSize()) {
                Crossfade(targetState = tab, label = "tab") { t ->
                    when (t) {
                        "playlists" -> {
                            val pid = playlistId
                            if (pid != null) {
                                PlaylistDetailScreen(
                                    vm = vm,
                                    id = pid,
                                    onBack = { playlistId = null },
                                    openPlayer = { showPlayer = true }
                                )
                            } else {
                                PlaylistsScreen(vm, openPlaylist = { playlistId = it })
                            }
                        }
                        "settings" -> SettingsScreen(vm)
                        else -> HomeScreen(
                            vm = vm,
                            openPlayer = { showPlayer = true },
                            openPlaylist = {
                                tab = "playlists"
                                playlistId = it
                            }
                        )
                    }
                }

                Column(Modifier.align(Alignment.BottomCenter)) {
                    MiniPlayer(vm) { showPlayer = true }
                    BottomNavBar(selected = tab) { route ->
                        tab = route
                        playlistId = null
                    }
                }

                AnimatedVisibility(
                    visible = showPlayer,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    PlayerScreen(vm, onBack = { showPlayer = false })
                }

                // зарегистрирован последним, поэтому закрывает плеер первым
                BackHandler(enabled = showPlayer) { showPlayer = false }
            }
        }
    }
}
