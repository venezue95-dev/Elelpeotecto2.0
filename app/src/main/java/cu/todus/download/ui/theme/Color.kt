package cu.todus.download.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Colores semánticos de la app para el tema activo (claro u oscuro) y el color de énfasis elegido. */
@Immutable
class AppColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val card: Color,
    val cardAlt: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accent2: Color,
    val onAccent: Color,
    val success: Color,
    val error: Color,
    val warn: Color
)

/**
 * Paleta de 100 colores de énfasis: 10 columnas × 10 filas.
 * Cada fila alterna medio paso de matiz y cada par de filas cambia de intensidad, así que hay
 * 20 matices × 5 tonos. El brillo se ajusta según el tema para que el color siempre tenga contraste.
 */
object AccentPalette {
    const val SIZE = 100
    const val COLUMNS = 10

    // Saturación y luminosidad por tono: (saturación, luminosidad)
    private val darkTones = arrayOf(0.95f to 0.60f, 0.85f to 0.72f, 0.70f to 0.66f, 0.95f to 0.48f, 0.55f to 0.62f)
    private val lightTones = arrayOf(0.95f to 0.38f, 0.80f to 0.45f, 0.65f to 0.34f, 0.90f to 0.30f, 0.50f to 0.42f)

    fun color(index: Int, dark: Boolean, hueShift: Float = 0f): Color {
        val i = index.coerceIn(0, SIZE - 1)
        val column = i % COLUMNS
        val row = i / COLUMNS
        val hue = ((column * 36f + (row % 2) * 18f + hueShift) % 360f + 360f) % 360f
        val (saturation, lightness) = (if (dark) darkTones else lightTones)[row / 2]
        return Color.hsl(hue, saturation, lightness)
    }
}

fun buildColors(dark: Boolean, accentIndex: Int): AppColors {
    val accent = AccentPalette.color(accentIndex, dark)
    val accent2 = AccentPalette.color(accentIndex, dark, hueShift = 32f)
    val onAccent = if (accent.luminance() > 0.45f) Color(0xFF0A0E17) else Color.White
    return if (dark) {
        AppColors(
            isDark = true,
            bg = Color(0xFF0A0E17),
            surface = Color(0xFF111827),
            card = Color(0xFF172235),
            cardAlt = Color(0xFF1D2B42),
            border = Color(0xFF2A3A53),
            textPrimary = Color(0xFFF4F8FF),
            textSecondary = Color(0xFFB0BDD0),
            textMuted = Color(0xFF74839A),
            accent = accent,
            accent2 = accent2,
            onAccent = onAccent,
            success = Color(0xFF39D98A),
            error = Color(0xFFFF6B81),
            warn = Color(0xFFFFC857)
        )
    } else {
        AppColors(
            isDark = false,
            bg = Color(0xFFF3F6FB),
            surface = Color(0xFFFFFFFF),
            card = Color(0xFFFFFFFF),
            cardAlt = Color(0xFFE9EFF8),
            border = Color(0xFFD5DEEA),
            textPrimary = Color(0xFF0E1726),
            textSecondary = Color(0xFF475569),
            textMuted = Color(0xFF7A889C),
            accent = accent,
            accent2 = accent2,
            onAccent = onAccent,
            success = Color(0xFF12A15C),
            error = Color(0xFFD92D48),
            warn = Color(0xFFB7791F)
        )
    }
}

val LocalAppColors = staticCompositionLocalOf { buildColors(dark = true, accentIndex = 5) }
