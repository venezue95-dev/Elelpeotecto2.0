package cu.todus.download.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.download.data.DownloadItem
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AppTheme
import java.text.DateFormat
import java.util.Date

/**
 * Tarjeta de descarga.
 * [onRemove] cancela la descarga si está en curso, o elimina la entrada si ya terminó.
 */
@Composable
fun DownloadCard(
    item: DownloadItem,
    onPauseResume: (DownloadItem) -> Unit,
    onRemove: (DownloadItem) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val completed = item.status == DownloadItem.Status.COMPLETED
    val running = item.status == DownloadItem.Status.DOWNLOADING || item.status == DownloadItem.Status.CONNECTING
    val queued = item.status == DownloadItem.Status.QUEUED

    val statusColor by animateColorAsState(
        targetValue = when (item.status) {
            DownloadItem.Status.COMPLETED -> c.success
            DownloadItem.Status.ERROR -> c.error
            DownloadItem.Status.PAUSED -> c.warn
            DownloadItem.Status.QUEUED -> c.textMuted
            else -> c.accent
        },
        animationSpec = tween(300),
        label = "status-color"
    )

    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.animateContentSize().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FileTile(item, completed, statusColor)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        color = c.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    if (completed) {
                        Text(completedSummary(item), color = c.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        StatusChip(item.status, statusColor)
                    }
                }
                if (!completed) {
                    Spacer(Modifier.width(8.dp))
                    val (pauseIcon, pauseLabel) = when {
                        running || queued -> AppIcons.Pause to "Pausar"
                        item.status == DownloadItem.Status.ERROR -> AppIcons.Retry to "Reintentar"
                        else -> AppIcons.Play to "Reanudar"
                    }
                    RoundIconButton(pauseIcon, pauseLabel, { onPauseResume(item) }, tint = c.accent, filled = true)
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(AppIcons.Close, "Cancelar descarga", { onRemove(item) }, tint = c.textMuted)
                }
            }

            if (!completed) {
                Spacer(Modifier.height(14.dp))
                val known = item.totalBytes > 0L
                FancyProgress(
                    progress = when {
                        known -> (item.percentage / 100f)
                        running -> null
                        else -> 0f
                    },
                    color = statusColor,
                    track = c.cardAlt,
                    shimmer = item.status == DownloadItem.Status.DOWNLOADING && known
                )
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (known) "${item.humanSize(item.downloadedBytes)} de ${item.humanSize()}" else item.humanSize(item.downloadedBytes),
                        color = c.textSecondary,
                        fontSize = 11.sp
                    )
                    if (known) {
                        Text("${item.percentage}%", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (item.status == DownloadItem.Status.DOWNLOADING) {
                    Spacer(Modifier.height(2.dp))
                    val eta = item.etaString()
                    Text(
                        if (eta.isNotBlank()) "${item.humanSpeed()}  ·  quedan $eta" else item.humanSpeed(),
                        color = c.textMuted,
                        fontSize = 11.sp
                    )
                }
                if (!item.errorMsg.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.error.copy(alpha = 0.10f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Warning, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(item.errorMsg ?: "", color = c.error, fontSize = 11.sp)
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.accent)
                            .bouncyClickable({ onOpen(item) }, pressedScale = 0.97f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Open, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir", color = c.onAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    RoundIconButton(AppIcons.Share, "Compartir", { onShare(item) }, tint = c.accent)
                    RoundIconButton(AppIcons.Trash, "Eliminar", { onRemove(item) }, tint = c.error)
                }
            }
        }
    }
}

@Composable
private fun FileTile(item: DownloadItem, completed: Boolean, color: Color) {
    val icon: ImageVector = when {
        completed -> AppIcons.Check
        item.isVideo -> AppIcons.Film
        item.isAudio -> AppIcons.Music
        item.isArchive -> AppIcons.Archive
        else -> AppIcons.File
    }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun StatusChip(status: DownloadItem.Status, color: Color) {
    val label = when (status) {
        DownloadItem.Status.DOWNLOADING -> "Descargando"
        DownloadItem.Status.CONNECTING -> "Conectando…"
        DownloadItem.Status.PAUSED -> "En pausa"
        DownloadItem.Status.QUEUED -> "En cola"
        DownloadItem.Status.COMPLETED -> "Completado"
        DownloadItem.Status.ERROR -> "Error"
        DownloadItem.Status.CANCELLED -> "Cancelado"
    }
    val icon = when (status) {
        DownloadItem.Status.QUEUED -> AppIcons.Queue
        DownloadItem.Status.PAUSED -> AppIcons.Pause
        DownloadItem.Status.ERROR -> AppIcons.Warning
        else -> AppIcons.Download
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun completedSummary(item: DownloadItem): String {
    val size = item.humanSize()
    if (item.completedTimestamp <= 0L) return size
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.completedTimestamp))
    return "$size  ·  $date"
}
