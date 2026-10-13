package com.newsstream.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Deep Ocean": dark navy water, and every piece of text in a clearly visible blue.
val SkyBlue = Color(0xFF9CCBFF) // body text
val Azure = Color(0xFF5AA9FF) // headlines, news accent
val Aqua = Color(0xFF3DD5FF) // social accent
private val Slate = Color(0xFF7FA3D6) // secondary text
private val Abyss = Color(0xFF061226)
private val Reef = Color(0xFF0C1F3D)
private val Shelf = Color(0xFF13305C)

private val OceanColors = darkColorScheme(
    primary = Azure,
    onPrimary = Abyss,
    primaryContainer = Shelf,
    onPrimaryContainer = SkyBlue,
    secondary = Aqua,
    onSecondary = Abyss,
    secondaryContainer = Color(0xFF0B3A4D),
    onSecondaryContainer = Aqua,
    tertiary = Color(0xFF8C9BFF),
    background = Abyss,
    onBackground = SkyBlue,
    surface = Reef,
    onSurface = SkyBlue,
    surfaceVariant = Shelf,
    onSurfaceVariant = Slate,
    surfaceContainer = Reef,
    surfaceContainerHigh = Shelf,
    surfaceContainerHighest = Shelf,
    outline = Color(0xFF2F5A94),
    outlineVariant = Color(0xFF1C3A66),
    error = Color(0xFFFF6B81),
    onError = Abyss,
)

private val Base = Typography()

private val OceanTypography = Base.copy(
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

private val OceanShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun NewsStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OceanColors, typography = OceanTypography, shapes = OceanShapes) {
        // Default text colour everywhere is blue, never black.
        CompositionLocalProvider(LocalContentColor provides SkyBlue, content = content)
    }
}

/** A translucent deep-blue card with a glowing outline in [accent]. */
fun Modifier.panel(accent: Color): Modifier {
    val shape = RoundedCornerShape(20.dp)
    return clip(shape)
        .background(Brush.verticalGradient(listOf(Shelf.copy(alpha = 0.85f), Reef.copy(alpha = 0.85f))))
        .border(1.dp, Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.15f))), shape)
}

/** Full-screen backdrop: deepening ocean gradient with light rays from the surface. */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B2347), Abyss, Color(0xFF02070F))))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Azure.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width,
                    ),
                    radius = size.width,
                    center = Offset(size.width * 0.5f, 0f),
                )
            },
        content = content,
    )
}
