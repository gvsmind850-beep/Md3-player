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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

private val tabs = listOf("home", "playlists", "settings")

private fun tabIndex(t: String): Int = tabs.indexOf(t).coerceAtLeast(0)

private fun slideSpec(forward: Boolean): ContentTransform =
    (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> if (forward) w / 3 else -w / 3 } +
        fadeIn(tween(340))) togetherWith
        (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 3 else w / 3 } +
            fadeOut(tween(200)))

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
            var showGestures by rememberSaveable { mutableStateOf(false) }
            var showPlayer by rememberSaveable { mutableStateOf(false) }
            val tabSwipe = with(LocalDensity.current) { 100.dp.toPx() }

            BackHandler(enabled = playlistId != null) { playlistId = null }
            BackHandler(enabled = showGestures) { showGestures = false }
            BackHandler(enabled = tab != "home" && playlistId == null && !showGestures) { tab = "home" }

            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(tab, playlistId, showGestures) {
                            // свайп по экрану переключает вкладки
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = {
                                    if (playlistId == null && !showGestures) {
                                        val i = tabIndex(tab)
                                        if (total < -tabSwipe && i < tabs.size - 1) tab = tabs[i + 1]
                                        else if (total > tabSwipe && i > 0) tab = tabs[i - 1]
                                    }
                                },
                                onDragCancel = { },
                                onHorizontalDrag = { _, dx -> total += dx }
                            )
                        },
                    transitionSpec = { slideSpec(tabIndex(targetState) > tabIndex(initialState)) },
                    label = "tab"
                ) { t ->
                    when (t) {
                        "playlists" -> AnimatedContent(
                            targetState = playlistId,
                            transitionSpec = { slideSpec(targetState != null) },
                            label = "playlist"
                        ) { pid ->
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
                        "settings" -> AnimatedContent(
                            targetState = showGestures,
                            transitionSpec = { slideSpec(targetState) },
                            label = "settings"
                        ) { g ->
                            if (g) {
                                GesturesScreen(onBack = { showGestures = false })
                            } else {
                                SettingsScreen(vm, openGestures = { showGestures = true })
                            }
                        }
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
                    AnimatedVisibility(
                        visible = vm.current != null,
                        enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 2 } +
                            fadeIn(tween(320)),
                        exit = slideOutVertically(tween(240)) { it / 2 } + fadeOut(tween(200))
                    ) {
                        MiniPlayer(vm) { showPlayer = true }
                    }
                    BottomNavBar(selected = tab) { route ->
                        tab = route
                        playlistId = null
                        showGestures = false
                    }
                }

                // закрытие и анимацию «вниз» плеер делает сам (свайп, кнопка, жест назад)
                AnimatedVisibility(
                    visible = showPlayer,
                    enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it } +
                        fadeIn(tween(220)),
                    exit = ExitTransition.None
                ) {
                    PlayerScreen(vm, onBack = { showPlayer = false })
                }
            }
        }
    }
}
