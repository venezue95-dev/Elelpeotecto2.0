package cu.todus.download.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cu.todus.download.data.EnqueueResult
import cu.todus.download.data.TodusUrl
import cu.todus.download.engine.FileNames
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AppTheme

/**
 * Diálogo para añadir enlaces. Detecta todos los enlaces de toDus del texto (pegados uno por línea,
 * copiados de un chat, compartidos desde otra app…) y los añade de una vez.
 * [onConfirm] devuelve cuántos se añadieron; si fue ninguno, [EnqueueResult.error] se muestra aquí.
 */
@Composable
fun AddUrlDialog(
    initialText: String = "",
    defaultThreads: Int = 4,
    maxConcurrent: Int = 3,
    onDismiss: () -> Unit,
    onConfirm: (text: String, fileName: String, threads: Int) -> EnqueueResult
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    var text by remember { mutableStateOf(initialText) }
    var customName by remember { mutableStateOf("") }
    var selectedThreads by remember { mutableIntStateOf(defaultThreads) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val results = remember(text) { TodusUrl.parseAll(text) }
    val valid = remember(results) { results.filterIsInstance<TodusUrl.Result.Ok>() }
    val invalidCount = results.size - valid.size

    fun paste() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                text = clip.getItemAt(0).coerceToText(context).toString().trim()
                errorMessage = null
            }
        } catch (_: Exception) {
        }
    }

    fun submit() {
        val result = onConfirm(text, customName.trim(), selectedThreads)
        if (result.added > 0) onDismiss() else errorMessage = result.error ?: "No se añadió ninguna descarga."
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.Link, contentDescription = null, tint = c.accent, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Nueva descarga", color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pega uno o varios enlaces de toDus", color = c.textMuted, fontSize = 11.sp)
                }
                RoundIconButton(AppIcons.Close, "Cerrar", onDismiss, tint = c.textMuted)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    errorMessage = null
                },
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("Un enlace por línea, o pega el texto completo", color = c.textMuted, fontSize = 13.sp) },
                colors = fieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("url_input_field")
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.accent.copy(alpha = 0.12f))
                    .clickable { paste() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("paste_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(AppIcons.Clipboard, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Pegar del portapapeles", color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Enlaces detectados
            AnimatedVisibility(visible = results.isNotEmpty()) {
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        buildString {
                            append(if (valid.size == 1) "1 enlace detectado" else "${valid.size} enlaces detectados")
                            if (invalidCount > 0) append("  ·  $invalidCount no válido${if (invalidCount > 1) "s" else ""}")
                        },
                        color = if (valid.isEmpty()) c.error else c.success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    valid.take(4).forEach { ok ->
                        val name = FileNames.fromUrl(ok.url).ifBlank { "archivo" }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape).background(c.success.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(AppIcons.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(11.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(name, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (valid.size > 4) {
                        Text("y ${valid.size - 4} más…", color = c.textMuted, fontSize = 11.sp)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.error.copy(alpha = 0.10f)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Warning, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(errorMessage ?: "", color = c.error, fontSize = 12.sp)
                }
            }

            // El nombre solo tiene sentido con un único enlace
            AnimatedVisibility(visible = valid.size == 1) {
                Column(Modifier.padding(top = 16.dp)) {
                    Text("Nombre personalizado (opcional)", color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        singleLine = true,
                        placeholder = { Text("Detectar automáticamente", color = c.textMuted, fontSize = 13.sp) },
                        colors = fieldColors(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Hilos por archivo", color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(1, 2, 4, 8, 16).forEach { threads ->
                    val isSelected = selectedThreads == threads
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) c.accent else c.cardAlt)
                            .bouncyClickable({ selectedThreads = threads }, pressedScale = 0.9f)
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            "${threads}x",
                            color = if (isSelected) c.onAccent else c.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Queue, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Se descargan hasta $maxConcurrent a la vez; las demás esperan en cola.",
                    color = c.textMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.accent)
                    .bouncyClickable({ submit() }, pressedScale = 0.97f)
                    .testTag("submit_download_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(AppIcons.Download, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (valid.size > 1) "Descargar ${valid.size} archivos" else "Descargar",
                    color = c.onAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = AppTheme.colors.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.border,
        cursorColor = c.accent,
        focusedContainerColor = c.card,
        unfocusedContainerColor = c.card
    )
}
