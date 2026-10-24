package com.roohu.roohux.features.downloads.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roohu.roohux.core.database.DownloadEntity
import com.roohu.roohux.core.database.DownloadStatus
import androidx.compose.animation.core.animateFloatAsState
import com.roohu.roohux.core.design.GlassCard
import com.roohu.roohux.core.design.formatBytes
import com.roohu.roohux.core.design.formatEta
import com.roohu.roohux.core.design.formatSpeed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadsScreen(vm: DownloadsViewModel, modifier: Modifier = Modifier) {
    val list by vm.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<DownloadEntity?>(null) }

    if (list.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "No downloads yet.\nPaste a direct file link on the Home tab to start.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "download_center_summary") {
                val active = list.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                val completed = list.count { it.status == DownloadStatus.COMPLETED }
                val downloaded = list.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.downloadedBytes }
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ROOHUX DOWNLOAD CENTER", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Your media, organized.", style = MaterialTheme.typography.headlineSmall)
                        Text("Track active transfers, review completed files, and manage your queue from one place.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DownloadStat("Active", active.toString(), Modifier.weight(1f))
                            DownloadStat("Completed", completed.toString(), Modifier.weight(1f))
                            DownloadStat("Saved", formatBytes(downloaded), Modifier.weight(1f))
                        }
                    }
                }
            }
            items(list, key = { it.id }) { d ->
                var showDetails by remember(d.id) { mutableStateOf(false) }
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(d.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val mediaType = d.mimeType?.takeIf { it.isNotBlank() }
                            ?: d.fileName.substringAfterLast('.', "").uppercase().takeIf { it.isNotBlank() }?.let { ".$it file" }
                            ?: "File type unknown"
                        Text(mediaType, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        StatusBlock(d)
                        TextButton(onClick = { showDetails = !showDetails }) {
                            Text(if (showDetails) "Hide details" else "File details")
                        }
                        if (showDetails) {
                            val extension = d.fileName.substringAfterLast('.', "").uppercase().takeIf { it.isNotBlank() }
                                ?: "Unknown"
                            val sourceHost = runCatching {
                                android.net.Uri.parse(d.url).host ?: "Unknown source"
                            }.getOrDefault("Unknown source")
                            val resolutionHint = Regex("""(?i)(?:^|[^0-9])(2160|1440|1080|720|480|360|240)p?(?:[^0-9]|$)""")
                                .find(d.fileName)?.groupValues?.get(1)?.let { "${it}p (filename hint only)" }
                                ?: "Not provided by the filename"
                            HorizontalDivider()
                            DetailLine("File size", if (d.totalBytes > 0) formatBytes(d.totalBytes) else if (d.downloadedBytes > 0) "${formatBytes(d.downloadedBytes)} downloaded; total unknown" else "Unknown")
                            DetailLine("Format", extension)
                            DetailLine("Content type", d.mimeType?.takeIf { it.isNotBlank() } ?: "Not provided")
                            DetailLine("Source website", sourceHost)
                            if (d.fileName.contains(Regex("""(?i)\.(mp4|mkv|webm|mov|avi)$"""))) {
                                DetailLine("Resolution", resolutionHint)
                                Text("RoohuX keeps the original file quality. A quality selector is only possible when a website exposes separate versions; this download has no verified alternate-quality list.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (d.status == DownloadStatus.DOWNLOADING || d.status == DownloadStatus.PAUSED) {
                                DetailLine("Downloaded", formatBytes(d.downloadedBytes))
                                DetailLine("Remaining", if (d.totalBytes > 0) formatBytes((d.totalBytes - d.downloadedBytes).coerceAtLeast(0)) else "Unknown")
                                DetailLine("Resume support", when (d.resumable) {
                                    1 -> "Supported"
                                    0 -> "Not supported by server"
                                    else -> "Not checked yet"
                                })
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            when (d.status) {
                                DownloadStatus.DOWNLOADING -> {
                                    if (d.resumable == 1) TextButton({ vm.pause(d.id) }) { Text("Pause") }
                                    TextButton({ vm.cancel(d.id) }) { Text("Cancel") }
                                }
                                DownloadStatus.QUEUED -> TextButton({ vm.cancel(d.id) }) { Text("Cancel") }
                                DownloadStatus.PAUSED -> {
                                    TextButton({ vm.resume(d.id) }) { Text("Resume") }
                                    TextButton({ vm.cancel(d.id) }) { Text("Cancel") }
                                }
                                DownloadStatus.FAILED, DownloadStatus.CANCELED -> {
                                    TextButton({ vm.resume(d.id) }) { Text("Retry") }
                                    TextButton({ pendingDelete = d }) { Text("Delete") }
                                }
                                DownloadStatus.COMPLETED -> {
                                    TextButton({ DownloadActions.open(context, d) }) { Text("Open") }
                                    TextButton({ DownloadActions.share(context, d) }) { Text("Share") }
                                    TextButton({ DownloadActions.locate(context) }) { Text("Locate") }
                                    TextButton({ pendingDelete = d }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { d ->
        val completed = d.status == DownloadStatus.COMPLETED
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?") },
            text = {
                Text(
                    if (completed) "Delete the file from Downloads/RoohuX too, or only remove it from this list? Tap outside to cancel."
                    else "This removes it from the list and discards any partial data.",
                )
            },
            confirmButton = {
                TextButton({ vm.delete(d.id, deleteFile = true); pendingDelete = null }) { Text(if (completed) "Delete file" else "Delete") }
            },
            dismissButton = {
                if (completed) TextButton({ vm.delete(d.id, deleteFile = false); pendingDelete = null }) { Text("Remove from list") }
            },
        )
    }
}

@Composable
private fun StatusBlock(d: DownloadEntity) {
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    when (d.status) {
        DownloadStatus.QUEUED -> Text("Queued", color = sub)
        DownloadStatus.DOWNLOADING -> {
            if (d.totalBytes > 0) {
                val animated by animateFloatAsState((d.downloadedBytes.toFloat() / d.totalBytes).coerceIn(0f, 1f), label = "progress")
                LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
            } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            val parts = buildList {
                if (d.totalBytes > 0) add("${d.downloadedBytes * 100 / d.totalBytes}%")
                add("${formatBytes(d.downloadedBytes)} / ${formatBytes(d.totalBytes)}")
                if (d.speedBps > 0) add(formatSpeed(d.speedBps))
                if (d.speedBps > 0 && d.totalBytes > 0) add(formatEta((d.totalBytes - d.downloadedBytes) / d.speedBps))
            }
            Text(parts.joinToString(" · "), color = sub, style = MaterialTheme.typography.bodySmall)
            if (d.resumable == 0) Text("This server doesn't support pausing or resuming.", color = sub, style = MaterialTheme.typography.bodySmall)
        }
        DownloadStatus.PAUSED -> Text("Paused · ${formatBytes(d.downloadedBytes)} / ${formatBytes(d.totalBytes)}", color = sub)
        DownloadStatus.COMPLETED -> Text("${formatBytes(d.downloadedBytes)} · Saved to Downloads/RoohuX", color = sub)
        DownloadStatus.CANCELED -> Text("Canceled", color = sub)
        DownloadStatus.FAILED -> {
            Text(d.error?.userMessage ?: "Download failed.", color = MaterialTheme.colorScheme.error)
            d.errorDetail?.let { Text("Details: $it", color = sub, style = MaterialTheme.typography.bodySmall) }
            if (d.resumable == 1 && d.downloadedBytes > 0) Text("Retry will continue from ${formatBytes(d.downloadedBytes)}.", color = sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(108.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DownloadStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
