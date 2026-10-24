package com.roohu.roohux.features.player

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.media3.ui.AspectRatioFrameLayout
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
    var controlsVisible by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var seekFeedbackCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(seekFeedbackCount) {
        if (seekFeedbackCount > 0) { delay(650); seekFeedback = null }
    }
    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(if (backPressed) 0.92f else 1f, label = "backScale")
    LaunchedEffect(backPressed) { if (backPressed) { delay(120); backPressed = false } }
    val positionPrefs = remember(context) { context.getSharedPreferences("roohux_playback_positions", android.content.Context.MODE_PRIVATE) }
    val positionKey = remember(uri) { "position_${uri.hashCode()}" }

    DisposableEffect(uri) {
        errorText = null
        buffering = false
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        var attached: MediaController? = null
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorText = "Can't play this media (${error.errorCodeName}). The file may be missing, protected, or in an unsupported format."
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }

            override fun onTracksChanged(tracks: Tracks) {
                audioOnly = tracks.groups.isNotEmpty() && tracks.isTypeSelected(C.TRACK_TYPE_AUDIO) && !tracks.isTypeSelected(C.TRACK_TYPE_VIDEO)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) positionPrefs.edit().remove(positionKey).apply()
            }
        }
        future.addListener({
            try {
                val c = future.get()
                attached = c
                c.addListener(listener)
                playing = c.isPlaying
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
            buffering = false
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
                    controllerShowTimeoutMs = 4000
                    controllerHideOnTouch = true
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        controlsVisible = visibility == android.view.View.VISIBLE
                    })
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setFullscreenButtonClickListener { fullscreen ->
                        ctx.findActivity()?.requestedOrientation =
                            if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            },
            update = { view -> view.player = controller },
            onRelease = { view -> view.player = null },
        )
        // Edge-only gesture zones keep the middle of Media3 PlayerView touchable.
        // Single taps are deliberately not consumed, so native controller toggling still works.
        if (!audioOnly && controller != null) {
            Row(Modifier.fillMaxWidth().fillMaxHeight(0.7f).align(Alignment.Center)) {
                Box(
                    Modifier
                        .fillMaxHeight(0.42f)
                        .weight(1f)
                        .pointerInput(controller) {
                            detectTapGestures(onDoubleTap = {
                                controller?.let { player ->
                                    player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                                    seekFeedback = "−10s"
                                    seekFeedbackCount++
                                }
                            })
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AnimatedVisibility(
                        visible = seekFeedback == "−10s",
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            "↶ 10s",
                            Modifier.padding(start = 28.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxHeight(0.42f)
                        .weight(1f)
                        .pointerInput(controller) {
                            detectTapGestures(onDoubleTap = {
                                controller?.let { player ->
                                    val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                    player.seekTo((player.currentPosition + 10_000L).coerceAtMost(end))
                                    seekFeedback = "+10s"
                                    seekFeedbackCount++
                                }
                            })
                        },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    AnimatedVisibility(
                        visible = seekFeedback == "+10s",
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            "10s ↷",
                            Modifier.padding(end = 28.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = buffering && errorText == null && !audioOnly,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(180)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = Color.White.copy(alpha = 0.92f),
                strokeWidth = 3.dp,
                trackColor = Color.White.copy(alpha = 0.14f),
            )
        }
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(180)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
        Row(
            Modifier.fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black.copy(alpha = 0.48f),
                            Color.Transparent,
                        ),
                    ),
                )
                .statusBarsPadding()
                .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .scale(backScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { backPressed = true; onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "ROOHUX  /  NOW PLAYING",
                    color = Color(0xFF80E7F2),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    title,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            // Compact transport shortcuts. Media3's original timeline and advanced controls remain available.
            if (!audioOnly && controller != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("−10", if (playing) "Ⅱ" else "▶", "+10").forEachIndexed { index, label ->
                        TextButton(
                            onClick = {
                                controller?.let { player ->
                                    when (index) {
                                        0 -> player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                                        1 -> if (player.isPlaying) player.pause() else player.play()
                                        else -> {
                                            val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                            player.seekTo((player.currentPosition + 10_000L).coerceAtMost(end))
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(width = 43.dp, height = 40.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (index == 1) Color(0xFF80E7F2).copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
        }
        errorText?.let {
            Text(it, Modifier.align(Alignment.Center).padding(32.dp), color = Color.White, textAlign = TextAlign.Center)
        }
    }
}
