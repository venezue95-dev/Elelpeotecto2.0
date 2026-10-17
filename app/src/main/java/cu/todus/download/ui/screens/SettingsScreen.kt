package cu.todus.download.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.download.data.AppPreferences
import cu.todus.download.ui.components.SegmentedChoice
import cu.todus.download.ui.components.SettingsGroup
import cu.todus.download.ui.components.SettingsRow
import cu.todus.download.ui.components.appSliderColors
import cu.todus.download.ui.components.appSwitchColors
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AccentPalette
import cu.todus.download.ui.theme.AppTheme
import kotlin.math.roundToInt

private val threadOptions = listOf(1, 2, 4, 8, 16)

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onMaxConcurrentChange: (Int) -> Unit,
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit,
    accentIndex: Int,
    onChooseAccent: () -> Unit,
    downloadDirText: String,
    isDefaultDir: Boolean,
    onPickFolder: () -> Unit,
    onResetFolder: () -> Unit,
    storageGranted: Boolean,
    onFixStorage: () -> Unit,
    versionName: String,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    var threadIndex by remember {
        mutableIntStateOf(threadOptions.indexOf(preferences.defaultThreads).let { if (it < 0) 2 else it })
    }
    var concurrent by remember { mutableIntStateOf(preferences.maxConcurrent) }
    var resume by remember { mutableStateOf(preferences.autoResume) }
    var notify by remember { mutableStateOf(preferences.showNotifications) }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text("Ajustes", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Personaliza cómo descargas y cómo se ve la app", color = c.textSecondary, fontSize = 12.sp)
        }

        SettingsGroup("Descargas", AppIcons.Download) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Descargas simultáneas", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Las demás esperan en cola", color = c.textMuted, fontSize = 11.sp)
                    }
                    ValueBadge("$concurrent")
                }
                Slider(
                    value = concurrent.toFloat(),
                    onValueChange = { concurrent = it.roundToInt().coerceIn(1, AppPreferences.MAX_CONCURRENT_LIMIT) },
                    onValueChangeFinished = {
                        preferences.maxConcurrent = concurrent
                        onMaxConcurrentChange(concurrent)
                    },
                    valueRange = 1f..AppPreferences.MAX_CONCURRENT_LIMIT.toFloat(),
                    steps = AppPreferences.MAX_CONCURRENT_LIMIT - 2,
                    colors = appSliderColors()
                )
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hilos por archivo", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Equilibra velocidad y estabilidad", color = c.textMuted, fontSize = 11.sp)
                    }
                    ValueBadge("${threadOptions[threadIndex]}x")
                }
                Slider(
                    value = threadIndex.toFloat(),
                    onValueChange = { threadIndex = it.roundToInt().coerceIn(0, threadOptions.lastIndex) },
                    onValueChangeFinished = { preferences.defaultThreads = threadOptions[threadIndex] },
                    valueRange = 0f..threadOptions.lastIndex.toFloat(),
                    steps = threadOptions.size - 2,
                    colors = appSliderColors()
                )
            }
            SettingsRow(
                AppIcons.Retry,
                "Reanudación automática",
                "Reintenta y continúa si la señal falla",
                trailing = {
                    Switch(checked = resume, onCheckedChange = { resume = it; preferences.autoResume = it }, colors = appSwitchColors())
                }
            )
            SettingsRow(
                AppIcons.Bell,
                "Avisar al terminar",
                "Notificación cuando una descarga finaliza o falla",
                trailing = {
                    Switch(checked = notify, onCheckedChange = { notify = it; preferences.showNotifications = it }, colors = appSwitchColors())
                }
            )
        }

        SettingsGroup("Destino", AppIcons.Library) {
            SettingsRow(
                AppIcons.FolderOpen,
                "Carpeta de descargas",
                downloadDirText,
                trailing = { Icon(AppIcons.Chevron, null, tint = c.textMuted, modifier = Modifier.size(18.dp)) },
                onClick = onPickFolder
            )
            if (!isDefaultDir) {
                SettingsRow(
                    AppIcons.Retry,
                    "Usar la carpeta por defecto",
                    "Download/${AppPreferences.DEFAULT_FOLDER}",
                    onClick = onResetFolder
                )
            }
            SettingsRow(
                AppIcons.Shield,
                "Acceso al almacenamiento",
                if (storageGranted) "Concedido" else "Falta el permiso: toca para concederlo",
                trailing = {
                    if (!storageGranted) {
                        Icon(AppIcons.Warning, null, tint = c.warn, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(AppIcons.Check, null, tint = c.success, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = if (storageGranted) null else onFixStorage
            )
        }

        SettingsGroup("Apariencia", AppIcons.Palette) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tema", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                SegmentedChoice(
                    options = listOf(
                        AppIcons.Phone to "Sistema",
                        AppIcons.Sun to "Claro",
                        AppIcons.Moon to "Oscuro"
                    ),
                    selectedIndex = themeMode,
                    onSelected = onThemeModeChange
                )
            }
            SettingsRow(
                AppIcons.Palette,
                "Color de énfasis",
                "Elige entre 100 colores",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(AccentPalette.color(accentIndex, c.isDark))
                                .border(2.dp, c.border, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(AppIcons.Chevron, null, tint = c.textMuted, modifier = Modifier.size(18.dp))
                    }
                },
                onClick = onChooseAccent
            )
        }

        SettingsGroup("Información", AppIcons.Info) {
            SettingsRow(AppIcons.Bolt, "toDus Download", "Versión $versionName")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ValueBadge(text: String) {
    val c = AppTheme.colors
    androidx.compose.foundation.layout.Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.accent.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = c.accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}
