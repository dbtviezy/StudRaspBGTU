package ru.bgtu.schedule.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

data class BgtuPalette(
    val bg: Color,
    val blue: Color,
    val blueDark: Color,
    val blueDeep: Color,
    val blueSoft: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val card: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val isDark: Boolean,
)

private val LightPalette = BgtuPalette(
    bg = Color(0xFFF4F6FB),
    blue = Color(0xFF2A56F6),
    blueDark = Color(0xFF1E3FCB),
    blueDeep = Color(0xFF1736B5),
    blueSoft = Color(0xFFE7EEFF),
    ink = Color(0xFF1B1E28),
    muted = Color(0xFF8B93A7),
    line = Color(0xFFE7EBF3),
    card = Color.White,
    heroStart = Color(0xFF3D6BFF),
    heroEnd = Color(0xFF2346E6),
    isDark = false,
)

private val DarkPalette = BgtuPalette(
    bg = Color(0xFF0E1422),
    blue = Color(0xFF5B86FF),
    blueDark = Color(0xFF9BB4FF),
    blueDeep = Color(0xFF1736B5),
    blueSoft = Color(0xFF243056),
    ink = Color(0xFFF4F6FB),
    muted = Color(0xFF9AA6BF),
    line = Color(0xFF2A3348),
    card = Color(0xFF182033),
    heroStart = Color(0xFF3A64F0),
    heroEnd = Color(0xFF1E3FCB),
    isDark = true,
)

val BrandBlue = Color(0xFF2A56F6)
val Teal = Color(0xFF12998A)
val Amber = Color(0xFFE39B2D)
val Violet = Color(0xFF6D5BD0)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

val LocalBottomInset = staticCompositionLocalOf { 0.dp }

val Bg: Color @Composable get() = LocalPalette.current.bg
val Blue: Color @Composable get() = LocalPalette.current.blue
val BlueDark: Color @Composable get() = LocalPalette.current.blueDark
val BlueDeep: Color @Composable get() = LocalPalette.current.blueDeep
val BlueSoft: Color @Composable get() = LocalPalette.current.blueSoft
val Ink: Color @Composable get() = LocalPalette.current.ink
val Muted: Color @Composable get() = LocalPalette.current.muted
val Line: Color @Composable get() = LocalPalette.current.line
val Card: Color @Composable get() = LocalPalette.current.card
val HeroStart: Color @Composable get() = LocalPalette.current.heroStart
val HeroEnd: Color @Composable get() = LocalPalette.current.heroEnd
val IsDark: Boolean @Composable get() = LocalPalette.current.isDark

@Composable
fun BgtuTheme(dark: Boolean, lightStatusIcons: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (lightStatusIcons) BrandBlue.toArgb() else palette.bg.toArgb()
            window.navigationBarColor = if (lightStatusIcons) BrandBlue.toArgb() else palette.card.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            val darkIcons = !lightStatusIcons && !palette.isDark
            controller.isAppearanceLightStatusBars = darkIcons
            controller.isAppearanceLightNavigationBars = darkIcons
        }
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.blue,
            onPrimary = Color.White,
            primaryContainer = palette.blueSoft,
            onPrimaryContainer = palette.blueDark,
            secondary = Teal,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.blueSoft,
            outline = palette.line,
        )
    } else {
        lightColorScheme(
            primary = palette.blue,
            onPrimary = Color.White,
            primaryContainer = palette.blueSoft,
            onPrimaryContainer = palette.blueDark,
            secondary = Teal,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.blueSoft,
            outline = palette.line,
        )
    }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    CompositionLocalProvider(LocalPalette provides palette, LocalBottomInset provides bottomInset) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography.copy(
                headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = palette.ink, letterSpacing = (-0.4).sp),
                titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = palette.ink),
                titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.ink),
                bodyMedium = TextStyle(fontSize = 14.sp, color = palette.ink),
                labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = palette.muted),
            ),
            content = content,
        )
    }
}
