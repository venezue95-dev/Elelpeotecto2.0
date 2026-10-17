package cu.todus.download.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.download.ui.components.GlassCard
import cu.todus.download.ui.components.bouncyClickable
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AppTheme

/**
 * Pantalla de permisos: almacenamiento y notificaciones.
 * Se cierra sola en cuanto los dos están concedidos (la actividad vuelve a comprobarlos al regresar de Ajustes).
 */
@Composable
fun PermissionScreen(
    storageGranted: Boolean,
    notificationsGranted: Boolean,
    onRequestStorage: () -> Unit,
    onRequestNotifications: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "permission-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse"
    )
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Column(
        modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(132.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.22f), c.accent.copy(alpha = 0f))))
            )
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(c.accent, c.accent2))),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.Shield, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Antes de empezar", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Necesitamos dos permisos para guardar tus descargas y avisarte cuando terminen.",
            color = c.textSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))

        AnimatedVisibility(visible = shown, enter = fadeIn(tween(350)) + slideInVertically(tween(400)) { it / 4 }) {
            PermissionCard(
                icon = AppIcons.FolderOpen,
                title = "Almacenamiento",
                description = "Para guardar los archivos en la carpeta que elijas.",
                granted = storageGranted,
                onClick = onRequestStorage
            )
        }
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(visible = shown, enter = fadeIn(tween(550)) + slideInVertically(tween(550)) { it / 4 }) {
            PermissionCard(
                icon = AppIcons.Bell,
                title = "Notificaciones",
                description = "Para ver el progreso y saber cuándo termina cada descarga.",
                granted = notificationsGranted,
                onClick = onRequestNotifications
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Ahora no",
            color = c.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .bouncyClickable(onSkip, pressedScale = 0.94f)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
        Text(
            "Sin el permiso de almacenamiento los archivos se guardan en la carpeta privada de la app.",
            color = c.textMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    val c = AppTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(c.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(description, color = c.textMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            AnimatedContent(
                targetState = granted,
                transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(300), initialScale = 0.6f)) togetherWith fadeOut(tween(150)) },
                label = "permission-state"
            ) { isGranted ->
                if (isGranted) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(c.success.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.Check, contentDescription = "Concedido", tint = c.success, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.accent)
                            .bouncyClickable(onClick, pressedScale = 0.92f)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Permitir", color = c.onAccent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
