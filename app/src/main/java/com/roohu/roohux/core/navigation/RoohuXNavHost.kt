package com.roohu.roohux.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roohu.roohux.AppContainer
import com.roohu.roohux.PlayRequest
import com.roohu.roohux.core.design.AppBackground
import com.roohu.roohux.core.design.LocalReduceMotion
import com.roohu.roohux.core.design.findActivity
import com.roohu.roohux.features.browser.BrowserScreen
import com.roohu.roohux.features.downloads.ui.DownloadsScreen
import com.roohu.roohux.features.downloads.ui.DownloadsViewModel
import com.roohu.roohux.features.home.HomeScreen
import com.roohu.roohux.features.library.LibraryScreen
import com.roohu.roohux.features.library.LibraryViewModel
import com.roohu.roohux.features.player.PlayerScreen
import com.roohu.roohux.features.settings.SettingsScreen
import com.roohu.roohux.features.settings.SettingsViewModel

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Default.Home),
    Browser("browser", "Browse", Icons.Default.Search),
    Downloads("downloads", "Downloads", Icons.AutoMirrored.Filled.List),
    Library("library", "Library", Icons.Default.PlayArrow),
    Settings("settings", "Settings", Icons.Default.Settings),
}

private val BAR_ROUTES = setOf("home", "downloads", "library", "settings")

@Composable
fun RoohuXNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val downloadsVm: DownloadsViewModel = viewModel(factory = DownloadsViewModel.factory(container))
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val libraryVm: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val browser = container.browser
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val reduce = LocalReduceMotion.current
    val fullscreen = browser.fullscreenView

    fun go(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun play(uri: String, title: String) {
        container.playerRequest.current = PlayRequest(uri, title)
        nav.navigate("player") { launchSingleTop = true }
    }

    LaunchedEffect(Unit) { browser.onPlay = { uri, title -> play(uri, title) } }
    LaunchedEffect(Unit) { container.messages.collect { snackbar.showSnackbar(it) } }

    // Immersive mode for fullscreen web video and the player.
    val immersive = fullscreen != null || route == "player"
    DisposableEffect(immersive) {
        val window = context.findActivity()?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (immersive) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { }
    }
    BackHandler(enabled = fullscreen != null) { browser.exitFullscreen() }

    Box(Modifier.fillMaxSize()) {
        AppBackground {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (route in BAR_ROUTES) {
                        NavigationBar(containerColor = Color.Transparent) {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true,
                                    onClick = { go(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = Tab.Home.route,
                    modifier = Modifier.padding(padding),
                    enterTransition = { if (reduce) EnterTransition.None else fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.96f) },
                    exitTransition = { if (reduce) ExitTransition.None else fadeOut(tween(160)) },
                    popEnterTransition = { if (reduce) EnterTransition.None else fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.96f) },
                    popExitTransition = { if (reduce) ExitTransition.None else fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 1.04f) },
                ) {
                    composable("home") { HomeScreen(downloadsVm, browser, onNavigate = { go(it) }) }
                    composable("browser") { BrowserScreen(browser, onNavigate = { go(it) }) }
                    composable("downloads") { DownloadsScreen(downloadsVm) }
                    composable("library") { LibraryScreen(libraryVm, onPlay = { u, t -> play(u, t) }) }
                    composable("settings") { SettingsScreen(settingsVm) }
                    composable("player") {
                        container.playerRequest.current?.let { req ->
                            PlayerScreen(req.uri, req.title, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
        if (fullscreen != null) {
            AndroidView(
                factory = { _ ->
                    (fullscreen.parent as? ViewGroup)?.removeView(fullscreen)
                    fullscreen
                },
                modifier = Modifier.fillMaxSize().background(Color.Black),
            )
        }
    }
}
