package com.roohu.roohux.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** True when the user enabled Reduce Motion or the system animation scale is 0. */
val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val dark = isDarkSurface()
    val brush: Brush = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF0E1530), Color(0xFF0B1020), Color(0xFF05070F)))
    } else SolidColor(MaterialTheme.colorScheme.background)
    Box(Modifier.fillMaxSize().background(brush)) { content() }
}

/** Translucent "glass" panel with a soft gradient border and a spring press effect when clickable. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val dark = isDarkSurface()
    val fill = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.85f)
    val border: Brush = if (dark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f)))
    } else SolidColor(Color.Black.copy(alpha = 0.08f))
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun loopingValue(durationMillis: Int, reduce: Boolean, fixed: Float = 0.3f): State<Float> {
    if (reduce) return remember { mutableStateOf(fixed) }
    val transition = rememberInfiniteTransition(label = "loop")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
}

/**
 * Static ambient emblem. Deliberately avoids orbital movement, floating particles, and 3D-like motion.
 */
@Composable
fun HoloOrb(modifier: Modifier = Modifier) {
    val violet = Violet
    val cyan = Cyan
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = minOf(size.width, size.height) / 2f * 0.78f
        drawCircle(Brush.radialGradient(listOf(violet.copy(alpha = 0.20f), Color.Transparent), center = c, radius = r), radius = r, center = c)
        drawCircle(color = violet.copy(alpha = 0.52f), radius = r * 0.78f, center = c, style = Stroke(width = 1.5f))
        drawCircle(color = cyan.copy(alpha = 0.45f), radius = r * 0.61f, center = c, style = Stroke(width = 1.dp.toPx()))
        drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = 0.22f), violet.copy(alpha = 0.08f)), center = c, radius = r * 0.45f), radius = r * 0.45f, center = c)
        drawCircle(color = Color.White.copy(alpha = 0.75f), radius = r * 0.035f, center = Offset(c.x, c.y - r * 0.61f))
        drawCircle(color = cyan.copy(alpha = 0.85f), radius = r * 0.025f, center = Offset(c.x + r * 0.60f, c.y + r * 0.12f))
    }
}

/** Three softly pulsing dots for loading states. */
@Composable
fun PulsingDots(modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val t = loopingValue(1200, reduce, 1f)
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val alpha = if (reduce) 0.8f else (0.25f + 0.75f * (0.5f + 0.5f * sin((t.value * 2f * PI.toFloat()) - i * 1.1f)))
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        }
    }
}
