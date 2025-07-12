package com.focusfloat.app.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FocusThemeMode {
    System,
    Light,
    Dark,
    Amoled,
}

enum class FocusTextScale(val multiplier: Float) {
    Small(0.85f),
    Medium(1f),
    Large(1.15f),
    ExtraLarge(1.3f),
}

@Immutable
data class FocusColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val line: Color,
    val line2: Color,
    val press: Color,
    val dot: Color,
    val err: Color,
    val errDim: Color,
    val scrubber: Color,
)

@Immutable
data class FocusSpacing(
    val screenPad: Dp = 24.dp,
    val rowH: Dp = 56.dp,
    val favRowH: Dp = 52.dp,
    val sheetPad: Dp = 24.dp,
    val sheetRowH: Dp = 56.dp,
    val tap: Dp = 48.dp,
    val statusH: Dp = 36.dp,
    val gestureH: Dp = 24.dp,
    val radiusSheet: Dp = 26.dp,
    val radiusDialog: Dp = 28.dp,
    val radiusBanner: Dp = 16.dp,
)

@Immutable
data class FocusType(
    val clock: TextStyle,
    val date: TextStyle,
    val appRow: TextStyle,
    val sheetTitle: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
    val section: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
)

object FocusTheme {
    val colors: FocusColors
        @Composable get() = LocalFocusColors.current

    val spacing: FocusSpacing
        @Composable get() = LocalFocusSpacing.current

    val type: FocusType
        @Composable get() = LocalFocusType.current
}

private val LocalFocusColors = staticCompositionLocalOf { amoledColors() }
private val LocalFocusSpacing = staticCompositionLocalOf { FocusSpacing() }
private val LocalFocusType = staticCompositionLocalOf { focusType(FocusTextScale.Medium) }

@Composable
fun FocusFloatTheme(
    mode: FocusThemeMode = FocusThemeMode.Amoled,
    textScale: FocusTextScale = FocusTextScale.Medium,
    content: @Composable () -> Unit,
) {
    val resolvedMode = when (mode) {
        FocusThemeMode.System -> if (isSystemInDarkTheme()) FocusThemeMode.Dark else FocusThemeMode.Light
        else -> mode
    }
    val colors = when (resolvedMode) {
        FocusThemeMode.Light -> lightFocusColors()
        FocusThemeMode.Dark -> darkFocusColors()
        FocusThemeMode.Amoled,
        FocusThemeMode.System -> amoledColors()
    }
    val materialColors = if (resolvedMode == FocusThemeMode.Light) {
        lightColorScheme(
            background = colors.bg,
            surface = colors.surface,
            primary = colors.text,
            onPrimary = colors.bg,
            onBackground = colors.text,
            onSurface = colors.text,
            error = colors.err,
        )
    } else {
        darkColorScheme(
            background = colors.bg,
            surface = colors.surface,
            primary = colors.text,
            onPrimary = colors.bg,
            onBackground = colors.text,
            onSurface = colors.text,
            error = colors.err,
        )
    }

    CompositionLocalProvider(
        LocalFocusColors provides colors,
        LocalFocusSpacing provides FocusSpacing(),
        LocalFocusType provides focusType(textScale),
    ) {
        MaterialTheme(colorScheme = materialColors, content = content)
    }
}

private fun focusType(scale: FocusTextScale): FocusType {
    fun scaled(size: Int) = (size * scale.multiplier).sp
    return FocusType(
        clock = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = scaled(52),
            lineHeight = scaled(52),
            letterSpacing = (-0.5).sp,
        ),
        date = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(15),
            lineHeight = scaled(18),
        ),
        appRow = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(22),
            lineHeight = scaled(26),
        ),
        sheetTitle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(19),
            lineHeight = scaled(24),
        ),
        body = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(16),
            lineHeight = scaled(23),
        ),
        button = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(16),
            lineHeight = scaled(19),
        ),
        section = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(12),
            lineHeight = scaled(14),
            letterSpacing = 0.6.sp,
        ),
        caption = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(13),
            lineHeight = scaled(17),
        ),
        mono = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(12),
            lineHeight = scaled(18),
        ),
    )
}

private fun amoledColors() = FocusColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF0C0C0D),
    surface2 = Color(0xFF161618),
    text = Color(0xFFF4F4F5),
    text2 = Color(0x8FF4F4F5),
    text3 = Color(0x4DF4F4F5),
    line = Color(0x1AF4F4F5),
    line2 = Color(0x0FF4F4F5),
    press = Color(0x12F4F4F5),
    dot = Color(0x66F4F4F5),
    err = Color(0xFFE26A5C),
    errDim = Color(0x29E26A5C),
    scrubber = Color(0x66F4F4F5),
)

private fun darkFocusColors() = FocusColors(
    bg = Color(0xFF111113),
    surface = Color(0xFF1C1C1F),
    surface2 = Color(0xFF26262A),
    text = Color(0xFFEAEAEC),
    text2 = Color(0x94EAEAEC),
    text3 = Color(0x52EAEAEC),
    line = Color(0x1FEAEAEC),
    line2 = Color(0x12EAEAEC),
    press = Color(0x14EAEAEC),
    dot = Color(0x6BEAEAEC),
    err = Color(0xFFE57367),
    errDim = Color(0x2EE57367),
    scrubber = Color(0x6BEAEAEC),
)

private fun lightFocusColors() = FocusColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF1F1F2),
    text = Color(0xFF161618),
    text2 = Color(0x8F161618),
    text3 = Color(0x52161618),
    line = Color(0x1F161618),
    line2 = Color(0x12161618),
    press = Color(0x0D161618),
    dot = Color(0x73161618),
    err = Color(0xFFC5392E),
    errDim = Color(0x1AC5392E),
    scrubber = Color(0x73161618),
)
