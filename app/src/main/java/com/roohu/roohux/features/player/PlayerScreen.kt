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
import androidx.compose.runtime.*
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
                audioOnly = tracks.groups.isNotEmpty() && !tracks.isTypeSelected(C.TRACK_TYPE_VIDEO)
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
                if (c.currentTracks.isTypeSelected(C.TRACK_TYPE_VIDEO)) c.pause() // video doesn't continue in the background
            }
            controller = null
            MediaController.releaseFuture(future)
        }
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
        errorText?.let {
            Text(it, Modifier.align(Alignment.Center).padding(32.dp), color = Color.White, textAlign = TextAlign.Center)
        }
    }
}
