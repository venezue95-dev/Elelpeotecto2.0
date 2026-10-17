package cu.todus.download.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.download.data.DownloadItem
import cu.todus.download.data.formatSize
import cu.todus.download.data.formatSpeed
import cu.todus.download.ui.components.DownloadCard
import cu.todus.download.ui.components.EmptyState
import cu.todus.download.ui.components.FancyProgress
import cu.todus.download.ui.components.GlassCard
import cu.todus.download.ui.components.GradientButton
import cu.todus.download.ui.components.StatusPill
import cu.todus.download.ui.components.bouncyClickable
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AppTheme

@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    maxConcurrent: Int,
    onOpenAddDialog: () -> Unit,
    onPauseResume: (DownloadItem) -> Unit,
    onCancel: (DownloadItem) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val queue = remember(downloads) { downloads.filter { it.status != DownloadItem.Status.COMPLETED } }
    val running = remember(queue) {
        queue.filter { it.status == DownloadItem.Status.DOWNLOADING || it.status == DownloadItem.Status.CONNECTING }
    }
    val waiting = remember(queue) { queue.count { it.status == DownloadItem.Status.QUEUED } }
    val stopped = remember(queue) {
        queue.count { it.status == DownloadItem.Status.PAUSED || it.status == DownloadItem.Status.ERROR }
    }
    val totalSpeed = remember(running) { running.sumOf { it.speedBps } }

    // Progreso global de lo que tiene tamaño conocido
    val sized = remember(queue) { queue.filter { it.totalBytes > 0L } }
    val sumTotal = remember(sized) { sized.sumOf { it.totalBytes } }
    val sumDone = remember(sized) { sized.sumOf { it.downloadedBytes.coerceAtMost(it.totalBytes) } }
    val overall = if (sumTotal > 0L) sumDone.toFloat() / sumTotal.toFloat() else 0f

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "toDus Download",
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(c.accent, c.accent2)),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text("Descargas rápidas y en paralelo", color = c.textSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))

        // Resumen
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.Speed, contentDescription = null, tint = c.accent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (running.isEmpty()) "Sin actividad" else formatSpeed(totalSpeed),
                            color = c.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            buildString {
                                append("${running.size} de $maxConcurrent activas")
                                if (waiting > 0) append("  ·  $waiting en cola")
                                if (stopped > 0) append("  ·  $stopped en pausa")
                            },
                            color = c.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    StatusPill(
                        if (running.isNotEmpty()) "Descargando" else "Listo",
                        active = true
                    )
                }
                if (sumTotal > 0L) {
                    Spacer(Modifier.height(14.dp))
                    FancyProgress(
                        progress = overall,
                        color = c.accent,
                        track = c.cardAlt,
                        shimmer = running.isNotEmpty()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${formatSize(sumDone)} de ${formatSize(sumTotal)}",
                        color = c.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GradientButton("Añadir enlaces", AppIcons.Plus, onOpenAddDialog, Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Cola de descarga", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(queue.size.toString(), color = c.onAccent, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            if (running.isNotEmpty() || waiting > 0) {
                QueueAction("Pausar todo", AppIcons.Pause, onPauseAll)
            } else if (stopped > 0) {
                QueueAction("Reanudar todo", AppIcons.Play, onResumeAll)
            }
        }
        Spacer(Modifier.height(10.dp))

        if (queue.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Tu cola está vacía",
                    message = "Añade uno o varios enlaces de toDus para empezar a descargar.",
                    icon = AppIcons.Download,
                    action = { GradientButton("Pegar enlaces", AppIcons.Clipboard, onOpenAddDialog) }
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(queue, key = { it.id }) { item ->
                    DownloadCard(
                        item = item,
                        onPauseResume = onPauseResume,
                        onRemove = onCancel,
                        onOpen = onOpen,
                        onShare = onShare,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.accent.copy(alpha = 0.12f))
            .border(1.dp, c.accent.copy(alpha = 0.25f), RoundedCornerShape(50))
            .bouncyClickable(onClick, pressedScale = 0.93f)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = c.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
