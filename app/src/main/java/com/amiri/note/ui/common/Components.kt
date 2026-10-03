package com.amiri.note.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amiri.note.ui.theme.*

/** Frosted-glass panel look: translucent gradient fill + bright rim light. */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false
): Modifier {
    val top = if (strong) 0x38FFFFFF else 0x26FFFFFF
    val bottom = if (strong) 0x1AFFFFFF else 0x0DFFFFFF
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(top.toInt()), Color(bottom.toInt()))))
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(Color(0x80FFFFFF), Color(0x14FFFFFF), Color(0x33FFFFFF))
            ),
            shape
        )
}

/** Colourful drifting backdrop that the glass panels sit on. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "glass-bg")
    val drift = t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom)))
            .drawBehind {
                val w = size.width; val h = size.height
                val r = maxOf(w, h)
                drawCircle(
                    Brush.radialGradient(
                        listOf(BlobBlue.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(w * (0.15f + 0.15f * drift.value), h * (0.12f + 0.05f * drift.value)),
                        radius = r * 0.55f
                    ),
                    radius = r * 0.55f,
                    center = Offset(w * (0.15f + 0.15f * drift.value), h * (0.12f + 0.05f * drift.value))
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(BlobViolet.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(w * (0.95f - 0.15f * drift.value), h * (0.45f + 0.08f * drift.value)),
                        radius = r * 0.5f
                    ),
                    radius = r * 0.5f,
                    center = Offset(w * (0.95f - 0.15f * drift.value), h * (0.45f + 0.08f * drift.value))
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(BlobTeal.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(w * (0.2f + 0.2f * drift.value), h * (0.95f - 0.06f * drift.value)),
                        radius = r * 0.5f
                    ),
                    radius = r * 0.5f,
                    center = Offset(w * (0.2f + 0.2f * drift.value), h * (0.95f - 0.06f * drift.value))
                )
            }
    )
}

/** A frosted-glass card used throughout the app. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
    )
}

/** Slim animated-looking progress bar. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun StatPill(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
