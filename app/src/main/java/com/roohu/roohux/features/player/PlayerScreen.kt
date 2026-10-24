package com.roohu.roohux.features.player

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.roohu.roohux.core.design.HoloOrb
import com.roohu.roohux.core.design.findActivity

/**
 * Full-screen player. Playback runs in [PlaybackService] (via MediaController) so audio continues in the background.
 * Video pauses when this screen is left; audio keeps playing and is controlled from the media notification.
 * Seek bar, fullscreen/landscape toggle, playback speed, audio-track and subtitle selection come from Media3's PlayerView.
 */
@Composable
fun PlayerScreen(uri: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var audioOnly by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerMessage by remember { mutableStateOf<String?>(null) }
    val positionPrefs = remember(context) { context.getSharedPreferences("roohux_playback_positions", android.content.Context.MODE_PRIVATE) }
    val positionKey = remember(uri) { "position_${uri.hashCode()}" }

    DisposableEffect(uri) {
        errorText = null
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        var attached: MediaController? = null
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorText = "Can't play this media (${error.errorCodeName}). The file may be missing, protected, or in an unsupported format."
            }

            override fun onTracksChanged(tracks: Tracks) {
                audioOnly = tracks.groups.isNotEmpty() && tracks.isTypeSelected(C.TRACK_TYPE_AUDIO) && !tracks.isTypeSelected(C.TRACK_TYPE_VIDEO)
                if (!audioOnly) { sleepTimerMinutes = null; showSleepTimer = false }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) positionPrefs.edit().remove(positionKey).apply()
            }
        }
        future.addListener({
            try {
                val c = future.get()
                attached = c
                c.addListener(listener)
                c.setMediaItem(
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                        .build(),
                )
                // Restore the last saved position before playback starts.
                val savedPosition = positionPrefs.getLong(positionKey, 0L)
                if (savedPosition > 0L) c.seekTo(savedPosition)
                positionPrefs.edit().putLong("recent_${uri.hashCode()}", System.currentTimeMillis()).apply()
                c.prepare()
                c.playWhenReady = true
                controller = c
            } catch (e: Exception) {
                errorText = "Couldn't start the player."
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            attached?.let { c ->
                c.removeListener(listener)
                // Save the current position when leaving the player. Keep audio playback in background.
                if (c.currentPosition > 0L && c.playbackState != Player.STATE_ENDED) {
                    positionPrefs.edit().putLong(positionKey, c.currentPosition).apply()
                }
                if (c.currentTracks.isTypeSelected(C.TRACK_TYPE_VIDEO)) c.pause() // video doesn't continue in the background
            }
            controller = null
            MediaController.releaseFuture(future)
        }
    }

    // Save progress periodically so it survives app closure, not only screen navigation.
    LaunchedEffect(controller, uri) {
        val c = controller ?: return@LaunchedEffect
        while (true) {
            if (c.currentMediaItem != null && c.currentPosition > 0L && c.playbackState != Player.STATE_ENDED) {
                positionPrefs.edit().putLong(positionKey, c.currentPosition).apply()
            }
            delay(2_000)
        }
    }

    LaunchedEffect(controller, sleepTimerMinutes) {
        val minutes = sleepTimerMinutes ?: return@LaunchedEffect
        delay(minutes * 60_000L)
        controller?.pause()
        sleepTimerMinutes = null
        sleepTimerMessage = "Playback paused by sleep timer"
        delay(2500)
        sleepTimerMessage = null
    }
    // Leaving the screen restores portrait/auto rotation.
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (audioOnly) HoloOrb(Modifier.align(Alignment.Center).size(260.dp))
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    controllerShowTimeoutMs = 3500
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setFullscreenButtonClickListener { fullscreen ->
                        ctx.findActivity()?.requestedOrientation =
                            if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            },
            update = { view -> view.player = controller },
            onRelease = { view -> view.player = null },
        )
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Text(title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        if (audioOnly) Surface(
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        ) {
            TextButton(onClick = { showSleepTimer = true }) {
                Text(if (sleepTimerMinutes == null) "Sleep timer" else "Timer: " + sleepTimerMinutes + "m")
            }
        }
        sleepTimerMessage?.let {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp),
                color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                Text(it, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        errorText?.let {
            Text(it, Modifier.align(Alignment.Center).padding(32.dp), color = Color.White, textAlign = TextAlign.Center)
        }
    }
    if (showSleepTimer && audioOnly) {
        AlertDialog(
            onDismissRequest = { showSleepTimer = false },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    Text("Pause playback automatically after:")
                    listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
                        TextButton(onClick = { sleepTimerMinutes = minutes; showSleepTimer = false }, modifier = Modifier.fillMaxWidth()) { Text(minutes.toString() + " minutes") }
                    }
                    if (sleepTimerMinutes != null) TextButton(onClick = { sleepTimerMinutes = null; showSleepTimer = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel active timer") }
                }
            },
            confirmButton = { TextButton(onClick = { showSleepTimer = false }) { Text("Close") } },
        )
    }
}
