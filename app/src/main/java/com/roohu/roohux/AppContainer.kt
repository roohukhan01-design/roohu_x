package com.roohu.roohux

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import com.roohu.roohux.core.database.AppDatabase
import com.roohu.roohux.core.network.WebViewCookieJar
import com.roohu.roohux.core.settings.SettingsRepository
import com.roohu.roohux.features.browser.BrowserManager
import com.roohu.roohux.features.downloads.data.DownloadCoordinator
import com.roohu.roohux.features.downloads.data.DownloadEngine
import com.roohu.roohux.services.download.DownloadNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlayRequest(val uri: String, val title: String)

/** What the player screen should play; lives in the container so rotation/navigation can't lose it. */
class PlayerRequestHolder { var current by mutableStateOf<PlayRequest?>(null) }

/** Manual dependency graph (no Hilt, to keep the build simple and low-risk). */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** One-shot user-facing messages (shown as snackbars). */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val playerRequest = PlayerRequestHolder()

    // No destructive-migration fallback: user data must survive updates.
    val database: AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, "roohux.db").build()
    val settings = SettingsRepository(appContext)
    val notifier = DownloadNotifier(appContext)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .cookieJar(WebViewCookieJar)
        .build()

    private val engine = DownloadEngine(appContext, database.downloadDao(), httpClient, settings, notifier)
    val downloads = DownloadCoordinator(appContext, database.downloadDao(), engine, settings, scope)
    val browser = BrowserManager(appContext, database, downloads, httpClient, settings) { messages.tryEmit(it) }

    init { scope.launch { downloads.recoverInterrupted() } }
}
