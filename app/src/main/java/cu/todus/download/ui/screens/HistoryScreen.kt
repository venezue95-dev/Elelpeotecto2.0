package cu.todus.download.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.download.data.DownloadItem
import cu.todus.download.data.formatSize
import cu.todus.download.ui.components.DownloadCard
import cu.todus.download.ui.components.EmptyState
import cu.todus.download.ui.components.RoundIconButton
import cu.todus.download.ui.components.bouncyClickable
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AppTheme

@Composable
fun HistoryScreen(
    downloads: List<DownloadItem>,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onDelete: (DownloadItem) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Todo") }
    val completed = remember(downloads) {
        downloads.filter { it.status == DownloadItem.Status.COMPLETED }.sortedByDescending { it.completedTimestamp }
    }
    val totalSize = remember(completed) { completed.sumOf { it.totalBytes } }
    val filters = listOf("Todo", "Vídeo", "Audio", "Otros")
    val visible = remember(completed, search, filter) {
        completed.filter { item ->
            val typeMatches = when (filter) {
                "Vídeo" -> item.isVideo
                "Audio" -> item.isAudio
                "Otros" -> !item.isVideo && !item.isAudio
                else -> true
            }
            typeMatches && item.name.contains(search, ignoreCase = true)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Historial", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (completed.isEmpty()) "Aún no hay archivos" else "${completed.size} archivos  ·  ${formatSize(totalSize)}",
                    color = c.textSecondary,
                    fontSize = 12.sp
                )
            }
            if (completed.isNotEmpty()) {
                RoundIconButton(AppIcons.Sweep, "Limpiar historial", onClearAll, tint = c.textSecondary)
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            singleLine = true,
            leadingIcon = { Icon(AppIcons.Search, "Buscar", tint = c.accent, modifier = Modifier.size(20.dp)) },
            placeholder = { Text("Buscar archivos…", color = c.textMuted) },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.textPrimary,
                unfocusedTextColor = c.textPrimary,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.border,
                cursorColor = c.accent,
                focusedContainerColor = c.card,
                unfocusedContainerColor = c.card
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            filters.forEach { option -> FilterPill(option, filter == option) { filter = option } }
        }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    if (search.isBlank() && filter == "Todo") "Aún no hay descargas" else "Sin coincidencias",
                    "Tus archivos completados aparecerán aquí.",
                    AppIcons.FolderOpen
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visible, key = { it.id }) { item ->
                    DownloadCard(
                        item = item,
                        onPauseResume = {},
                        onRemove = onDelete,
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
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val bg by animateColorAsState(if (selected) c.accent else c.card, tween(250), label = "pill-bg")
    val fg by animateColorAsState(if (selected) c.onAccent else c.textSecondary, tween(250), label = "pill-fg")
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) c.accent else c.border, RoundedCornerShape(50))
            .bouncyClickable(onClick, pressedScale = 0.92f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
