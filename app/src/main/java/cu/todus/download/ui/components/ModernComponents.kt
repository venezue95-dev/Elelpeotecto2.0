package cu.todus.download.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AppTheme
import kotlin.math.roundToInt

/** Botón/elemento que "rebota" al pulsarlo. Úsalo en lugar de `clickable`. */
@Composable
fun Modifier.bouncyClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.95f
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "press-scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier.border(
            BorderStroke(
                1.dp,
                Brush.linearGradient(listOf(c.accent.copy(alpha = if (c.isDark) 0.28f else 0.22f), c.border, c.border.copy(alpha = 0.4f)))
            ),
            shape
        ),
        shape = shape,
        color = c.card,
        tonalElevation = 0.dp,
        shadowElevation = if (c.isDark) 0.dp else 2.dp,
        content = content
    )
}

@Composable
fun GradientButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .shadow(14.dp, shape, ambientColor = c.accent.copy(alpha = 0.3f), spotColor = c.accent.copy(alpha = 0.3f))
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(c.accent, c.accent2)))
            .bouncyClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = c.onAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

/** Botón redondo pequeño con ícono (pausar, cancelar, abrir…). [filled] lo pinta con el color de énfasis. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.accent,
    filled: Boolean = false
) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (filled) tint else tint.copy(alpha = 0.12f))
            .bouncyClickable(onClick, pressedScale = 0.88f),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (filled) c.onAccent else tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun StatusPill(text: String, detail: String? = null, active: Boolean = true) {
    val c = AppTheme.colors
    val color by animateColorAsState(if (active) c.success else c.warn, tween(300), label = "pill-color")
    val transition = rememberInfiniteTransition(label = "pill-dot")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dot-alpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = dotAlpha)))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        detail?.let { Text("  ·  $it", color = c.textSecondary, fontSize = 11.sp) }
    }
}

data class NavBarItem(val label: String, val icon: ImageVector)

/**
 * Barra inferior flotante. [position] es la página actual más el desplazamiento del pager
 * (por ejemplo 1.4 = a 40% entre la segunda y la tercera), así el resaltado se desliza con el dedo.
 */
@Composable
fun FloatingBottomBar(
    items: List<NavBarItem>,
    position: () -> Float,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().border(1.dp, c.border, shape),
            shape = shape,
            color = c.surface,
            shadowElevation = 18.dp,
            tonalElevation = 0.dp
        ) {
            BoxWithConstraints(Modifier.padding(6.dp)) {
                val itemWidth = maxWidth / items.size
                val itemWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemWidth.toPx() }

                // Resaltado que sigue al pager
                Box(
                    Modifier
                        .offset { IntOffset((position() * itemWidthPx).roundToInt(), 0) }
                        .width(itemWidth)
                        .height(58.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.20f), c.accent.copy(alpha = 0.10f))))
                )

                Row(Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        val selected = position().roundToInt() == index
                        val tint by animateColorAsState(if (selected) c.accent else c.textMuted, tween(250), label = "nav-tint")
                        val scale by animateFloatAsState(
                            if (selected) 1.14f else 1f,
                            spring(dampingRatio = 0.5f, stiffness = 400f),
                            label = "nav-scale"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .bouncyClickable({ onSelected(index) }, pressedScale = 0.92f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                item.label,
                                color = tint,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        GlassCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(6.dp)) { content() } }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = c.textMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        trailing()
    }
}

/** Selector con indicador deslizante (por ejemplo Sistema / Claro / Oscuro). */
@Composable
fun SegmentedChoice(
    options: List<Pair<ImageVector, String>>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.cardAlt)
            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / options.size
        val offset by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
            label = "segment-offset"
        )
        val itemWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemWidth.toPx() }
        Box(
            Modifier
                .offset { IntOffset((offset * itemWidthPx).roundToInt(), 0) }
                .width(itemWidth)
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.accent)
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (icon, label) ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(if (selected) c.onAccent else c.textSecondary, tween(220), label = "segment-tint")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelected(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun appSwitchColors(): SwitchColors {
    val c = AppTheme.colors
    return SwitchDefaults.colors(
        checkedThumbColor = c.onAccent,
        checkedTrackColor = c.accent,
        checkedBorderColor = c.accent,
        uncheckedThumbColor = c.textMuted,
        uncheckedTrackColor = c.cardAlt,
        uncheckedBorderColor = c.border
    )
}

@Composable
fun appSliderColors(): SliderColors {
    val c = AppTheme.colors
    return SliderDefaults.colors(
        thumbColor = c.accent,
        activeTrackColor = c.accent,
        inactiveTrackColor = c.cardAlt,
        activeTickColor = c.onAccent.copy(alpha = 0.6f),
        inactiveTickColor = c.textMuted.copy(alpha = 0.5f)
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = AppIcons.Download,
    action: (@Composable () -> Unit)? = null
) {
    val c = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "empty-float")
    val floatY by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "float-y"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
        Box(
            Modifier
                .graphicsLayer { translationY = floatY * density }
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.20f), c.accent.copy(alpha = 0.06f))))
                .border(1.dp, c.accent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text(message, color = c.textMuted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
        action?.let { Spacer(Modifier.height(16.dp)); it() }
    }
}
