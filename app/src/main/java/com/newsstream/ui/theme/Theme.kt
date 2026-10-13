package com.newsstream.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF2BD6)
val NeonGreen = Color(0xFF39FF14)
private val Void = Color(0xFF05070D)
private val Panel = Color(0xFF0B1020)
private val PanelRaised = Color(0xFF121A33)
private val Ink = Color(0xFFDDE6FF)
private val InkDim = Color(0xFF8A96B8)

private val CyberColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Void,
    primaryContainer = Color(0xFF003C40),
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Void,
    tertiary = NeonGreen,
    onTertiary = Void,
    background = Void,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = InkDim,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelRaised,
    surfaceContainerHighest = PanelRaised,
    outline = Color(0xFF1F6F7A),
    error = Color(0xFFFF3B5C),
    onError = Void,
)

private val Mono = FontFamily.Monospace
private val Base = Typography()

private val CyberTypography = Typography(
    headlineSmall = Base.headlineSmall.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 4.sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontFamily = Mono, letterSpacing = 1.5.sp),
    labelLarge = Base.labelLarge.copy(fontFamily = Mono, letterSpacing = 2.sp),
    labelSmall = Base.labelSmall.copy(fontFamily = Mono, letterSpacing = 1.5.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = Mono),
)

private val CyberShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
    medium = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
    large = CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp),
)

/** Neon gradient used for card and button outlines. */
val NeonBorderBrush = Brush.linearGradient(listOf(NeonCyan, NeonMagenta))

@Composable
fun NewsStreamTheme(content: @Composable () -> Unit) {
    // Always dark: the neon palette is designed for a dark background.
    MaterialTheme(
        colorScheme = CyberColors,
        typography = CyberTypography,
        shapes = CyberShapes,
        content = content,
    )
}

/** Full-screen backdrop: deep-space gradient with a faint cyan grid. */
@Composable
fun CyberBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0F24), Void, Color(0xFF12051C))))
            .drawBehind {
                val step = 32.dp.toPx()
                val line = NeonCyan.copy(alpha = 0.06f)
                var x = 0f
                while (x < size.width) {
                    drawLine(line, Offset(x, 0f), Offset(x, size.height))
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y))
                    y += step
                }
            },
        content = content,
    )
}
