package com.newsstream.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Retro-futuristic "tactical HUD": amber phosphor on near-black, teal readouts, red alerts.
val Amber = Color(0xFFFFB000)
val Teal = Color(0xFF3DDBD9)
val AlertRed = Color(0xFFFF4E42)
private val Night = Color(0xFF0A0C0B)
private val Panel = Color(0xFF111513)
private val PanelRaised = Color(0xFF1A201D)
private val Ink = Color(0xFFE8E3D3)
private val InkDim = Color(0xFF8C8A80)

private val HudColors = darkColorScheme(
    primary = Amber,
    onPrimary = Night,
    primaryContainer = Color(0xFF3A2A00),
    onPrimaryContainer = Amber,
    secondary = Teal,
    onSecondary = Night,
    secondaryContainer = Color(0xFF0F3534),
    onSecondaryContainer = Teal,
    tertiary = AlertRed,
    background = Night,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = InkDim,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelRaised,
    surfaceContainerHighest = PanelRaised,
    outline = Color(0xFF5C4A1E),
    outlineVariant = Color(0xFF2C2A22),
    error = AlertRed,
    onError = Night,
)

private val Mono = FontFamily.Monospace
private val Base = Typography()

private val HudTypography = Base.copy(
    headlineSmall = Base.headlineSmall.copy(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = Base.titleLarge.copy(fontFamily = Mono, fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontFamily = Mono, letterSpacing = 1.5.sp),
    labelMedium = Base.labelMedium.copy(fontFamily = Mono, letterSpacing = 1.sp),
    labelSmall = Base.labelSmall.copy(fontFamily = Mono, letterSpacing = 1.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = Mono),
)

// Hard, nearly square edges; the corner brackets do the decorating.
private val HudShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
)

@Composable
fun NewsStreamTheme(content: @Composable () -> Unit) {
    // Always dark: the phosphor palette is designed for a dark background.
    MaterialTheme(colorScheme = HudColors, typography = HudTypography, shapes = HudShapes, content = content)
}

/** Targeting-reticle frame: a faint outline plus bright L-shaped brackets on each corner. */
fun Modifier.hudFrame(color: Color = Amber, bracket: Dp = 14.dp): Modifier = drawWithContent {
    drawContent()
    val stroke = 2.dp.toPx()
    val len = bracket.toPx()
    val w = size.width
    val h = size.height
    drawRect(color.copy(alpha = 0.18f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    listOf(
        Offset(0f, 0f) to Offset(1f, 1f),
        Offset(w, 0f) to Offset(-1f, 1f),
        Offset(0f, h) to Offset(1f, -1f),
        Offset(w, h) to Offset(-1f, -1f),
    ).forEach { (corner, dir) ->
        drawLine(color, corner, Offset(corner.x + dir.x * len, corner.y), stroke)
        drawLine(color, corner, Offset(corner.x, corner.y + dir.y * len), stroke)
    }
}

/** Full-screen backdrop: dark vignette with faint CRT scanlines. */
@Composable
fun HudBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Night)
            .background(Brush.radialGradient(listOf(Color(0xFF1A1A10), Night)))
            .drawBehind {
                val step = 3.dp.toPx()
                val line = Color.Black.copy(alpha = 0.35f)
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y))
                    y += step
                }
            },
        content = content,
    )
}
