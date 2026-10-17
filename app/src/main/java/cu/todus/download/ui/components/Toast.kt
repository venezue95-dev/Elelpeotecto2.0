package cu.todus.download.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AppTheme

enum class ToastType { SUCCESS, ERROR, WARNING, INFO }

@Immutable
data class ToastMessage(val id: Long, val text: String, val type: ToastType)

/** Estado de los avisos: muestra uno a la vez; uno nuevo sustituye al anterior. */
@Stable
class ToastState {
    var current by mutableStateOf<ToastMessage?>(null)
        private set
    private var counter = 0L

    fun show(text: String, type: ToastType = ToastType.INFO) {
        counter += 1
        current = ToastMessage(counter, text, type)
    }

    fun dismiss(id: Long) {
        if (current?.id == id) current = null
    }
}

val LocalToast = staticCompositionLocalOf { ToastState() }

private const val TOAST_MS = 3000

/** Colocar una vez, encima de todo el contenido de la pantalla. */
@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val message = state.current
    // Se recuerda el último aviso para que siga visible mientras dura la animación de salida.
    val last = remember { arrayOfNulls<ToastMessage>(1) }
    if (message != null) last[0] = message
    val shown = last[0]
    val progress = remember { Animatable(1f) }

    LaunchedEffect(message?.id) {
        if (message != null) {
            progress.snapTo(1f)
            progress.animateTo(0f, tween(TOAST_MS, easing = LinearEasing))
            state.dismiss(message.id)
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(spring(dampingRatio = 0.62f, stiffness = 380f)) { -it } +
                fadeIn(tween(160)) + scaleIn(tween(220), initialScale = 0.86f),
            exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.92f)
        ) {
            if (shown != null) {
                ToastCard(shown, progress = { progress.value }, onClick = { state.dismiss(shown.id) })
            }
        }
    }
}

@Composable
private fun toastColor(type: ToastType): Color {
    val c = AppTheme.colors
    return when (type) {
        ToastType.SUCCESS -> c.success
        ToastType.ERROR -> c.error
        ToastType.WARNING -> c.warn
        ToastType.INFO -> c.accent
    }
}

private fun toastIcon(type: ToastType): ImageVector = when (type) {
    ToastType.SUCCESS -> AppIcons.Check
    ToastType.ERROR -> AppIcons.Close
    ToastType.WARNING -> AppIcons.Warning
    ToastType.INFO -> AppIcons.Info
}

@Composable
private fun ToastCard(message: ToastMessage, progress: () -> Float, onClick: () -> Unit) {
    val c = AppTheme.colors
    val color = toastColor(message.type)
    val shape = RoundedCornerShape(20.dp)
    val pop = remember(message.id) { Animatable(0.3f) }
    LaunchedEffect(message.id) {
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }

    Row(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .shadow(14.dp, shape, ambientColor = color.copy(alpha = 0.35f), spotColor = color.copy(alpha = 0.35f))
            .clip(shape)
            .background(c.card)
            .border(1.dp, color.copy(alpha = 0.40f), shape)
            .clickable(onClick = onClick)
            .drawBehind {
                val barHeight = 3.dp.toPx()
                drawRect(
                    color = color.copy(alpha = 0.9f),
                    topLeft = Offset(0f, size.height - barHeight),
                    size = Size(size.width * progress().coerceIn(0f, 1f), barHeight)
                )
            }
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                toastIcon(message.type),
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            message.text,
            color = c.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
