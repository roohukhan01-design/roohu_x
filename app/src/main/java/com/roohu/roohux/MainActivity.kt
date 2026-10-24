package com.roohu.roohux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roohu.roohux.core.design.LocalReduceMotion
import com.roohu.roohux.core.design.HoloOrb
import com.roohu.roohux.core.design.Cyan
import com.roohu.roohux.core.design.Violet
import com.roohu.roohux.core.design.RoohuXTheme
import com.roohu.roohux.core.design.isDark
import com.roohu.roohux.core.navigation.RoohuXNavHost
import com.roohu.roohux.core.settings.AppSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as RoohuXApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
            val dark = isDark(settings.theme)
            val systemReduce = remember {
                Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            CompositionLocalProvider(LocalReduceMotion provides (settings.reduceMotion || systemReduce)) {
                RoohuXTheme(dark) {
                    var intro by remember { mutableStateOf(savedInstanceState == null) }
                    LaunchedEffect(intro) {
                        if (intro) {
                            delay(if (settings.reduceMotion || systemReduce) 250L else 1200L)
                            intro = false
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        RoohuXNavHost(container)
                        AnimatedVisibility(visible = intro, enter = fadeIn(), exit = fadeOut()) {
                            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                Box(
                                    Modifier.fillMaxSize().background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(Color(0xFF080C16), Color(0xFF15162B), Color(0xFF0C1821)),
                                        ),
                                    ),
                                ) {
                                    Box(
                                        Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 58.dp)
                                            .size(width = 46.dp, height = 4.dp)
                                            .background(Cyan, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                    )
                                    Text(
                                        "PERSONAL MEDIA PLAYER",
                                        modifier = Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 76.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Column(
                                        Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                                            HoloOrb(Modifier.fillMaxSize())
                                            Surface(
                                                modifier = Modifier.size(92.dp),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(alpha = 0.45f)),
                                                shadowElevation = 0.dp,
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "R",
                                                        style = MaterialTheme.typography.displayMedium,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(18.dp))
                                        Text("ROOHU", style = MaterialTheme.typography.labelLarge, color = Cyan, letterSpacing = androidx.compose.ui.unit.sp(4))
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "RoohuX",
                                            style = MaterialTheme.typography.displayLarge,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text("Your media, beautifully in sync.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(34.dp))
                                        if (!settings.reduceMotion && !systemReduce) {
                                            LinearProgressIndicator(
                                                modifier = Modifier.fillMaxWidth(0.48f).height(3.dp),
                                                color = Cyan,
                                                trackColor = Violet.copy(alpha = 0.24f),
                                            )
                                        } else {
                                            Box(Modifier.width(48.dp).height(3.dp).background(Cyan, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
                                        }
                                        Spacer(Modifier.height(14.dp))
                                        Text(
                                            "GETTING THINGS READY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = androidx.compose.ui.unit.sp(1.5),
                                        )
                                    }
                                    Text(
                                        "MADE FOR YOUR OWN UNIVERSE",
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                        letterSpacing = androidx.compose.ui.unit.sp(1.2),
                                    )
                                } }
                }
            }
        }
    }
}
