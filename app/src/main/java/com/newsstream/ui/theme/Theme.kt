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
import androidx.compose.material3.lightColorScheme
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

// "Glacier": ice-white surfaces with all text in shades of blue.
val Navy = Color(0xFF0B2A66) // headlines and body text
val Electric = Color(0xFF1E5EFF) // news accent
val Cyan = Color(0xFF0091C2) // social accent
private val SteelBlue = Color(0xFF4A6694) // secondary text
private val Ice = Color(0xFFF2F7FD)
private val Frost = Color(0xFFFFFFFF)
private val Mist = Color(0xFFE3EDFA)

private val GlacierColors = lightColorScheme(
    primary = Electric,
    onPrimary = Frost,
    primaryContainer = Color(0xFFD9E5FF),
    onPrimaryContainer = Navy,
    secondary = Cyan,
    onSecondary = Frost,
    secondaryContainer = Color(0xFFD3F0FA),
    onSecondaryContainer = Navy,
    tertiary = Color(0xFF6B4EFF),
    background = Ice,
    onBackground = Navy,
    surface = Frost,
    onSurface = Navy,
    surfaceVariant = Mist,
    onSurfaceVariant = SteelBlue,
    surfaceContainer = Frost,
    surfaceContainerHigh = Mist,
    surfaceContainerHighest = Mist,
    outline = Color(0xFF9DB4D9),
    outlineVariant = Color(0xFFCCDAF0),
    error = Color(0xFFD32F4C),
    onError = Frost,
)

private val Base = Typography()

private val GlacierTypography = Base.copy(
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

private val GlacierShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun NewsStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GlacierColors, typography = GlacierTypography, shapes = GlacierShapes) {
        // Default text colour everywhere is navy blue, never black.
        CompositionLocalProvider(LocalContentColor provides Navy, content = content)
    }
}

/** A white card with a soft blue outline and an accent stripe on the left edge. */
fun Modifier.panel(accent: Color): Modifier {
    val shape = RoundedCornerShape(18.dp)
    return clip(shape)
        .background(Frost)
        .border(1.dp, accent.copy(alpha = 0.25f), shape)
        .drawBehind {
            drawRect(accent, size = size.copy(width = 4.dp.toPx()))
        }
}

/** Full-screen backdrop: icy gradient with a soft blue glow in the top corner. */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE6F0FF), Ice, Color(0xFFF7FAFE))))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Electric.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.8f,
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width, 0f),
                )
            },
        content = content,
    )
}
