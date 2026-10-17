package cu.todus.download.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Acceso corto a los colores del tema: `AppTheme.colors.accent`. */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
private fun animated(target: Color): Color =
    animateColorAsState(target, tween(380), label = "theme-color").value

/** Los colores cambian con una transición suave al alternar tema o color de énfasis. */
@Composable
private fun AppColors.animatedColors(): AppColors = AppColors(
    isDark = isDark,
    bg = animated(bg),
    surface = animated(surface),
    card = animated(card),
    cardAlt = animated(cardAlt),
    border = animated(border),
    textPrimary = animated(textPrimary),
    textSecondary = animated(textSecondary),
    textMuted = animated(textMuted),
    accent = animated(accent),
    accent2 = animated(accent2),
    onAccent = animated(onAccent),
    success = animated(success),
    error = animated(error),
    warn = animated(warn)
)

@Composable
fun TodusDownloadTheme(
    darkTheme: Boolean,
    accentIndex: Int,
    content: @Composable () -> Unit
) {
    val target = remember(darkTheme, accentIndex) { buildColors(darkTheme, accentIndex) }
    val c = target.animatedColors()

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            primaryContainer = c.cardAlt, onPrimaryContainer = c.textPrimary,
            secondary = c.accent2, onSecondary = c.onAccent,
            background = c.bg, onBackground = c.textPrimary,
            surface = c.surface, onSurface = c.textPrimary,
            surfaceVariant = c.card, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.card, surfaceContainerHigh = c.surface, surfaceContainerHighest = c.cardAlt,
            outline = c.border, outlineVariant = c.border,
            error = c.error, onError = c.onAccent
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            primaryContainer = c.cardAlt, onPrimaryContainer = c.textPrimary,
            secondary = c.accent2, onSecondary = c.onAccent,
            background = c.bg, onBackground = c.textPrimary,
            surface = c.surface, onSurface = c.textPrimary,
            surfaceVariant = c.card, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.card, surfaceContainerHigh = c.surface, surfaceContainerHighest = c.cardAlt,
            outline = c.border, outlineVariant = c.border,
            error = c.error, onError = c.onAccent
        )
    }

    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = TodusTypography, content = content)
    }
}

private val TodusTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-1.1).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 25.sp, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
)
