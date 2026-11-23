package com.narezany.appletree.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import com.narezany.appletree.ui.theme.AppleGold
import com.narezany.appletree.ui.theme.AppleLight
import com.narezany.appletree.ui.theme.AppleRed
import com.narezany.appletree.ui.theme.Leaf
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.LeafLight
import com.narezany.appletree.ui.theme.LeafPale
import com.narezany.appletree.ui.theme.Trunk
import com.narezany.appletree.ui.theme.TrunkDark
import kotlin.math.cos
import kotlin.math.sin

private data class AppleSpot(val angle: Float, val radius: Float, val size: Float, val gold: Boolean = false)

private val appleSpots = listOf(
    AppleSpot(-2.55f, 0.62f, 0.030f),
    AppleSpot(-2.05f, 0.82f, 0.028f),
    AppleSpot(-1.55f, 0.55f, 0.030f, true),
    AppleSpot(-1.15f, 0.90f, 0.027f),
    AppleSpot(-0.70f, 0.66f, 0.030f),
    AppleSpot(-0.30f, 0.88f, 0.026f),
    AppleSpot(0.12f, 0.58f, 0.029f),
    AppleSpot(0.45f, 0.86f, 0.027f, true),
    AppleSpot(0.85f, 0.62f, 0.030f),
    AppleSpot(1.25f, 0.90f, 0.026f),
    AppleSpot(1.60f, 0.55f, 0.029f),
    AppleSpot(2.00f, 0.80f, 0.027f),
    AppleSpot(2.45f, 0.60f, 0.028f),
    AppleSpot(2.85f, 0.84f, 0.025f),
    AppleSpot(3.30f, 0.62f, 0.028f, true),
    AppleSpot(3.75f, 0.88f, 0.025f),
)

@Composable
fun AppleTreeArt(
    level: Int,
    growth: Float,
    treeScale: Float,
    time: Float,
    highlight: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { onTap() }
        }
    ) {
        drawTree(level, growth, treeScale, time, highlight)
    }
}

fun DrawScope.drawTree(
    level: Int,
    growth: Float,
    treeScale: Float,
    time: Float,
    highlight: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val baseY = h * 0.90f

    val stage = level.coerceIn(1, 24)
    val grown = (stage - 1).coerceAtMost(11) / 11f
    val trunkH = h * (0.20f + 0.20f * grown) * treeScale
    val crownR = h * (0.15f + 0.13f * grown) * treeScale

    val sway = sin(time * 0.9f) * w * 0.004f
    val crownCx = cx + sway
    val crownCy = baseY - trunkH - crownR * 0.35f

    // тень под деревом
    drawOval(
        color = Color(0x33506B3C),
        topLeft = Offset(cx - crownR * 1.15f, baseY - h * 0.02f),
        size = Size(crownR * 2.3f, h * 0.045f)
    )

    // ствол
    val trunkTop = baseY - trunkH
    val trunkBottomW = crownR * 0.30f
    val trunkTopW = crownR * 0.16f
    val trunkPath = Path().apply {
        moveTo(cx - trunkBottomW, baseY)
        quadraticTo(cx - trunkBottomW * 0.7f, baseY - trunkH * 0.45f, crownCx - trunkTopW, trunkTop)
        lineTo(crownCx + trunkTopW, trunkTop)
        quadraticTo(cx + trunkBottomW * 0.7f, baseY - trunkH * 0.45f, cx + trunkBottomW, baseY)
        close()
    }
    drawPath(
        trunkPath,
        brush = Brush.horizontalGradient(
            0f to TrunkDark,
            0.45f to Trunk,
            1f to TrunkDark,
            startX = cx - trunkBottomW,
            endX = cx + trunkBottomW
        )
    )
    // ветки
    drawLine(
        TrunkDark,
        start = Offset(crownCx, trunkTop + trunkH * 0.18f),
        end = Offset(crownCx - crownR * 0.45f, crownCy + crownR * 0.15f),
        strokeWidth = crownR * 0.09f
    )
    drawLine(
        TrunkDark,
        start = Offset(crownCx, trunkTop + trunkH * 0.12f),
        end = Offset(crownCx + crownR * 0.5f, crownCy + crownR * 0.05f),
        strokeWidth = crownR * 0.085f
    )

    // крона — мягкие слои
    val crownBrush = Brush.radialGradient(
        colors = listOf(LeafPale, LeafLight, Leaf, LeafDeep),
        center = Offset(crownCx - crownR * 0.35f, crownCy - crownR * 0.4f),
        radius = crownR * 1.75f
    )
    val puffs = listOf(
        Triple(-0.62f, 0.10f, 0.78f),
        Triple(0.60f, 0.06f, 0.80f),
        Triple(-0.28f, -0.38f, 0.72f),
        Triple(0.30f, -0.40f, 0.70f),
        Triple(0f, -0.02f, 1.00f),
        Triple(-0.05f, 0.42f, 0.66f),
    )
    puffs.forEach { (dx, dy, r) ->
        drawCircle(
            brush = crownBrush,
            radius = crownR * r,
            center = Offset(crownCx + dx * crownR, crownCy + dy * crownR)
        )
    }
    // блики листвы
    drawCircle(
        color = Color(0x55E8F5DA),
        radius = crownR * 0.42f,
        center = Offset(crownCx - crownR * 0.42f, crownCy - crownR * 0.45f)
    )

    // яблоки
    val visible = (4 + stage / 2 + (growth * 4).toInt()).coerceIn(4, appleSpots.size)
    appleSpots.take(visible).forEachIndexed { i, spot ->
        val wobble = sin(time * 1.4f + i * 0.8f) * crownR * 0.012f
        val ax = crownCx + cos(spot.angle) * crownR * spot.radius * 1.55f
        val ay = crownCy + sin(spot.angle) * crownR * spot.radius * 0.95f + crownR * 0.10f + wobble
        drawApple(Offset(ax, ay), crownR * spot.size, spot.gold)
    }

    // трава у корней
    drawOval(
        brush = Brush.verticalGradient(listOf(LeafLight, LeafDeep)),
        topLeft = Offset(cx - crownR * 0.95f, baseY - h * 0.012f),
        size = Size(crownR * 1.9f, h * 0.035f)
    )

    if (highlight) {
        drawCircle(
            color = Color(0x33FFFFFF),
            radius = crownR * 1.45f,
            center = Offset(crownCx, crownCy),
            style = Stroke(width = crownR * 0.05f)
        )
    }
}

private fun DrawScope.drawApple(center: Offset, r: Float, gold: Boolean) {
    val main = if (gold) AppleGold else AppleRed
    val light = if (gold) Color(0xFFEBC47E) else AppleLight
    drawCircle(main, r, center)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
            center = Offset(center.x - r * 0.3f, center.y - r * 0.35f),
            radius = r * 1.1f
        ),
        radius = r,
        center = center
    )
    // черешок
    drawLine(
        Color(0xFF5E4630),
        start = Offset(center.x, center.y - r * 0.85f),
        end = Offset(center.x + r * 0.15f, center.y - r * 1.45f),
        strokeWidth = r * 0.18f
    )
    drawOval(
        Color(0xFF74A45E),
        topLeft = Offset(center.x + r * 0.05f, center.y - r * 1.5f),
        size = Size(r * 0.75f, r * 0.38f)
    )
}

/** Декоративный сад позади — мягкие холмы и облака. */
fun DrawScope.drawBackgroundHills(time: Float) {
    val w = size.width
    val h = size.height

    // дальние холмы
    val far = Path().apply {
        moveTo(0f, h * 0.68f)
        cubicTo(w * 0.2f, h * 0.60f, w * 0.35f, h * 0.74f, w * 0.55f, h * 0.68f)
        cubicTo(w * 0.75f, h * 0.62f, w * 0.85f, h * 0.72f, w, h * 0.66f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(far, Brush.verticalGradient(listOf(Color(0xFFBFDDC4), Color(0xFFA9CFAF)), endY = h))

    // ближние холмы
    val near = Path().apply {
        moveTo(0f, h * 0.80f)
        cubicTo(w * 0.25f, h * 0.72f, w * 0.45f, h * 0.83f, w * 0.65f, h * 0.78f)
        cubicTo(w * 0.82f, h * 0.74f, w * 0.92f, h * 0.81f, w, h * 0.77f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(near, Brush.verticalGradient(listOf(Color(0xFFA6CB8E), Color(0xFF8FBA78)), endY = h))

    // облака
    drawCloud(Offset(w * (0.16f + 0.01f * sin(time * 0.25f)), h * 0.16f), w * 0.10f)
    drawCloud(Offset(w * (0.72f + 0.015f * sin(time * 0.18f + 1.4f)), h * 0.11f), w * 0.075f)
    drawCloud(Offset(w * 0.46f, h * (0.22f + 0.004f * sin(time * 0.3f + 0.7f))), w * 0.055f)
}

private fun DrawScope.drawCloud(center: Offset, baseR: Float) {
    val c = Color(0xCCF8FDFF)
    drawCircle(c, baseR, Offset(center.x - baseR * 0.9f, center.y + baseR * 0.15f))
    drawCircle(c, baseR * 1.15f, Offset(center.x - baseR * 0.1f, center.y))
    drawCircle(c, baseR * 0.85f, Offset(center.x + baseR * 0.95f, center.y + baseR * 0.2f))
    drawOval(
        c,
        topLeft = Offset(center.x - baseR * 1.7f, center.y + baseR * 0.25f),
        size = Size(baseR * 3.4f, baseR * 0.9f)
    )
}

/** Маленькая иконка дерева для аватарок и списков. */
fun DrawScope.drawMiniAppleTree() {
    val w = size.width
    val h = size.height
    rotate(0f) {
        translate(w * 0.5f, h * 0.9f) {
            drawRect(
                color = Trunk,
                topLeft = Offset(-w * 0.035f, -h * 0.34f),
                size = Size(w * 0.07f, h * 0.34f)
            )
            drawCircle(Leaf, w * 0.24f, Offset(0f, -h * 0.42f))
            drawCircle(LeafLight, w * 0.19f, Offset(-w * 0.14f, -h * 0.34f))
            drawCircle(LeafLight, w * 0.18f, Offset(w * 0.15f, -h * 0.33f))
            drawCircle(AppleRed, w * 0.045f, Offset(-w * 0.10f, -h * 0.46f))
            drawCircle(AppleRed, w * 0.04f, Offset(w * 0.10f, -h * 0.52f))
        }
    }
}
