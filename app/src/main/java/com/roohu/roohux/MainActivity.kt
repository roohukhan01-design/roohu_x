package com.roohu.roohux

import androidx.compose.foundation.layout.*
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
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.background,
                                                Violet.copy(alpha = 0.16f),
                                                Cyan.copy(alpha = 0.08f),
                                                MaterialTheme.colorScheme.background,
                                            ),
                                        ),
                                    ),
                                ) {
                                    HoloOrb(
                                        Modifier.align(Alignment.Center).size(260.dp),
                                    )
                                    Column(
                                        Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            "ROOHU",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Cyan,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "RoohuX",
                                            style = MaterialTheme.typography.displayLarge,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "YOUR MEDIA. YOUR UNIVERSE.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(36.dp))
                                        if (!settings.reduceMotion && !systemReduce) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = Cyan,
                                                strokeWidth = 2.dp,
                                                trackColor = Violet.copy(alpha = 0.22f),
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "PREPARING YOUR EXPERIENCE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        "BUILT FOR YOUR MEDIA",
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
