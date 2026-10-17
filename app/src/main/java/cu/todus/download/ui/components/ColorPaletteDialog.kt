package cu.todus.download.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.theme.AccentPalette
import cu.todus.download.ui.theme.AppTheme
import cu.todus.download.ui.theme.buildColors

/**
 * Selector de color de énfasis: 100 colores (10×10) ajustados al tema actual.
 * Al tocar un color la app entera cambia al instante ([onSelect]); "Listo" cierra el diálogo.
 */
@Composable
fun ColorPaletteDialog(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = AppTheme.colors
    val dark = c.isDark
    val previewAccent by animateColorAsState(AccentPalette.color(selectedIndex, dark), tween(250), label = "preview")
    val previewAccent2 = AccentPalette.color(selectedIndex, dark, hueShift = 32f)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Text("Color de énfasis", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text("100 colores adaptados al tema ${if (dark) "oscuro" else "claro"}", color = c.textMuted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            // Vista previa
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(previewAccent, previewAccent2))),
                contentAlignment = Alignment.Center
            ) {
                val onColor = buildColors(dark, selectedIndex).onAccent
                Text("Así se verá la app", color = onColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))

            repeat(AccentPalette.SIZE / AccentPalette.COLUMNS) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(AccentPalette.COLUMNS) { column ->
                        val index = row * AccentPalette.COLUMNS + column
                        Swatch(
                            index = index,
                            dark = dark,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.accent)
                    .bouncyClickable(onDismiss, pressedScale = 0.97f),
                contentAlignment = Alignment.Center
            ) {
                Text("Listo", color = c.onAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Swatch(index: Int, dark: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val color = AccentPalette.color(index, dark)
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.82f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "swatch-scale"
    )
    Box(modifier.aspectRatio(1f).padding(2.5.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(color)
                .then(if (selected) Modifier.border(2.dp, c.textPrimary, CircleShape) else Modifier)
                .bouncyClickable(onClick, pressedScale = 0.85f),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(AppIcons.Check, contentDescription = "Seleccionado", tint = buildColors(dark, index).onAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}
