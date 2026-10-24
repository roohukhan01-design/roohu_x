package com.roohu.roohux.features.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roohu.roohux.core.database.DownloadEntity
import com.roohu.roohux.core.database.DownloadStatus
import com.roohu.roohux.core.design.Cyan
import com.roohu.roohux.core.design.GlassCard
import com.roohu.roohux.core.design.HoloOrb
import com.roohu.roohux.core.design.Violet
import com.roohu.roohux.features.browser.BrowserManager
import com.roohu.roohux.features.browser.BrowserPanel
import com.roohu.roohux.features.downloads.ui.DownloadsViewModel
import java.util.Calendar

@Composable
fun HomeScreen(vm: DownloadsViewModel, browser: BrowserManager, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val list by vm.downloads.collectAsStateWithLifecycle()
    val history by browser.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }

    fun startDownload() {
        error = vm.add(input)
        if (error == null) input = ""
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { startDownload() }

    fun onDownloadClicked() {
        val needsAsk = Build.VERSION.SDK_INT >= 33 && !askedForNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk && input.isNotBlank()) {
            askedForNotifications = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startDownload()
    }

    fun openBrowser() {
        if (input.isNotBlank()) { browser.go(input); input = "" }
        error = null
        onNavigate("browser")
    }

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier.fillMaxWidth().height(204.dp).clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Violet.copy(alpha = 0.34f),
                            Color(0xFF17264A).copy(alpha = 0.92f),
                            Cyan.copy(alpha = 0.12f),
                        ),
                    ),
                ),
        ) {
            HoloOrb(Modifier.align(Alignment.CenterEnd).size(190.dp))
            Box(
                Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 18.dp)
                    .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "PERSONAL MEDIA HUB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 22.dp, end = 10.dp, top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    "Your media.\nYour universe.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = MaterialTheme.typography.headlineMedium.lineHeight,
                )
                Text(
                    "$greeting. Find it, save it, enjoy it.",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(0.66f),
                )
            }
            Box(
                Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("BROWSE  •  DOWNLOAD  •  PLAY", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.86f))
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What do you want to open?") },
            isError = error != null,
            supportingText = { Text(error ?: "Paste a website or direct media link. Open to browse, Download for a file link.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { openBrowser() }),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { openBrowser() }, modifier = Modifier.weight(1f)) { Text("Open") }
            OutlinedButton(onClick = { onDownloadClicked() }, modifier = Modifier.weight(1f)) { Text("Download") }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Quick access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Everything you need, one tap away.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Browser", "Explore websites", Icons.Default.Search, Modifier.weight(1f)) { onNavigate("browser") }
            QuickAction("Downloads", "Track your files", Icons.AutoMirrored.Filled.List, Modifier.weight(1f)) { onNavigate("downloads") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Library", "Play saved media", Icons.Default.PlayArrow, Modifier.weight(1f)) { onNavigate("library") }
            QuickAction("Bookmarks", "Saved websites", Icons.Default.Home, Modifier.weight(1f)) { browser.pendingPanel = BrowserPanel.BOOKMARKS; onNavigate("browser") }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Your downloads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("A quick status overview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onNavigate("downloads") }) { Text("View all") }
            }
            val active = list.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Stat("Active", active.size, Modifier.weight(1f))
                Stat("Completed", list.count { it.status == DownloadStatus.COMPLETED }, Modifier.weight(1f))
                Stat("Failed", list.count { it.status == DownloadStatus.FAILED }, Modifier.weight(1f))
            }
            if (active.isEmpty()) {
                Text(
                    "Nothing downloading right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                active.take(3).forEach { ActiveRow(it) }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent sites", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (history.isNotEmpty()) TextButton({ browser.clearHistory() }) { Text("Clear") }
            }
            if (history.isEmpty()) Text("Nothing yet. Visited sites appear here; you can clear them any time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            history.take(5).forEach { h ->
                Text(
                    h.title, Modifier.fillMaxWidth().clickable { browser.go(h.url); onNavigate("browser") }.padding(vertical = 6.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    GlassCard(modifier, onClick = onClick) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Violet.copy(alpha = 0.22f), Cyan.copy(alpha = 0.14f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActiveRow(d: DownloadEntity) {
    val target = if (d.totalBytes > 0) (d.downloadedBytes.toFloat() / d.totalBytes).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(target, label = "progress")
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(d.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        if (d.totalBytes > 0) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
