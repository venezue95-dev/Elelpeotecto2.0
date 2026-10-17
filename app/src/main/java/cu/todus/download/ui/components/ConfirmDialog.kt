package cu.todus.download.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import cu.todus.download.ui.theme.AppTheme

/**
 * Diálogo de confirmación con el estilo de la app.
 * [onDismissClick] es el botón secundario; [onDismissRequest] es tocar fuera / atrás.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    destructive: Boolean = false
) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(26.dp),
        containerColor = c.surface,
        titleContentColor = c.textPrimary,
        textContentColor = c.textSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) c.error else c.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) { Text(dismissLabel, color = c.textSecondary) }
        }
    )
}
