package com.narezany.appletree.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.narezany.appletree.game.Sky
import com.narezany.appletree.game.TreeLook
import kotlin.math.sin

/**
 * Тихая сцена: небо, холмы, яблоня, кольцо созревания, корзина и качели.
 * Всё нарисовано кодом — никаких случайных картинок и неона.
 */
@Composable
fun GardenScene(
    sky: Sky,
    look: TreeLook,
    progress: Float,
    calm: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val motion = rememberInfiniteTransition(label = "garden")

    val sway by motion.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (calm) 12000 else 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val fall by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (calm) 24000 else 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall"
    )

    val glow by motion.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onTap() }
            }
    ) {
        val w = size.width
        val h = size.height
        val m = minOf(w, h)
        val cx = w / 2f
        val horizon = h * 0.70f

        val light = Color(look.light)
        val mid = Color(look.mid)
        val deep = Color(look.deep)
        val apple = Color(look.apple)

        // солнце
        val sun = Offset(w * 0.80f, h * 0.13f)
        val sunR = w * 0.062f
        drawCircle(Color(sky.accent).copy(alpha = 0.16f * glow), sunR * 2.2f, sun)
        drawCircle(Color(sky.accent).copy(alpha = 0.26f * glow), sunR * 1.5f, sun)
        drawCircle(Color(sky.accent), sunR, sun)

        // облака
        cloud(Color.White.copy(alpha = 0.48f), Offset(w * 0.18f, h * 0.115f), w * 0.095f)
        cloud(Color.White.copy(alpha = 0.34f), Offset(w * 0.62f, h * 0.065f), w * 0.070f)

        // дальние холмы
        val far = Path().apply {
            moveTo(0f, horizon)
            quadraticBezierTo(w * 0.20f, h * 0.60f, w * 0.46f, horizon - h * 0.015f)
            quadraticBezierTo(w * 0.72f, horizon + h * 0.02f, w, horizon - h * 0.045f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            far,
            Brush.verticalGradient(
                listOf(lerp(light, Color.White, 0.62f), lerp(mid, Color.White, 0.45f))
            )
        )

        // ближний холм
        val near = Path().apply {
            moveTo(0f, horizon + h * 0.035f)
            quadraticBezierTo(w * 0.28f, horizon - h * 0.01f, w * 0.55f, horizon + h * 0.03f)
            quadraticBezierTo(w * 0.82f, horizon + h * 0.06f, w, horizon + h * 0.02f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            near,
            Brush.verticalGradient(listOf(lerp(mid, Color.White, 0.3f), lerp(mid, Color.White, 0.05f)))
        )

        // трава на переднем плане
        drawRect(
            brush = Brush.verticalGradient(
                listOf(lerp(mid, Color.White, 0.12f), lerp(deep, Color.White, 0.08f))
            ),
            topLeft = Offset(0f, horizon + h * 0.055f),
            size = Size(w, h)
        )

        // травинки
        val tufts = listOf(0.06f, 0.13f, 0.21f, 0.30f, 0.40f, 0.58f, 0.67f, 0.76f, 0.85f, 0.93f)
        tufts.forEachIndexed { i, fx ->
            val bx = w * fx
            val by = horizon + h * (0.09f + (i % 3) * 0.022f)
            drawLine(
                deep.copy(alpha = 0.35f),
                Offset(bx, by),
                Offset(bx + m * 0.006f * (if (i % 2 == 0) 1 else -1), by - m * 0.026f),
                strokeWidth = m * 0.004f,
                cap = StrokeCap.Round
            )
        }

        // мелкие цветы
        val flowers = listOf(0.15f to 0.83f, 0.33f to 0.88f, 0.72f to 0.86f, 0.88f to 0.81f)
        flowers.forEach { (fx, fy) ->
            val c = Offset(w * fx, h * fy)
            drawCircle(Color.White.copy(alpha = 0.85f), m * 0.008f, c)
            drawCircle(Color(0xFFF2D48A), m * 0.004f, c)
        }

        // кольцо созревания
        val ringCenter = Offset(cx, h * 0.435f)
        val ringR = m * 0.345f
        val ringStroke = m * 0.028f
        drawArc(
            color = Color.White.copy(alpha = 0.42f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(ringCenter.x - ringR, ringCenter.y - ringR),
            size = Size(ringR * 2f, ringR * 2f),
            style = Stroke(width = ringStroke, cap = StrokeCap.Round)
        )
        if (progress > 0.005f) {
            drawArc(
                color = Color(0xFFE3B45E),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(ringCenter.x - ringR, ringCenter.y - ringR),
                size = Size(ringR * 2f, ringR * 2f),
                style = Stroke(width = ringStroke, cap = StrokeCap.Round)
            )
        }

        // ствол
        val trunk = Path().apply {
            moveTo(cx - w * 0.024f, h * 0.775f)
            quadraticBezierTo(cx - w * 0.019f, h * 0.64f, cx - w * 0.013f, h * 0.545f)
            lineTo(cx + w * 0.013f, h * 0.545f)
            quadraticBezierTo(cx + w * 0.020f, h * 0.65f, cx + w * 0.028f, h * 0.775f)
            close()
        }
        drawPath(
            trunk,
            Brush.horizontalGradient(
                listOf(Color(0xFF7A5640), Color(0xFF9A7359), Color(0xFF7F5B44))
            )
        )

        // ветви
        drawLine(
            Color(0xFF8A6246),
            Offset(cx, h * 0.585f),
            Offset(cx - m * 0.135f, h * 0.475f),
            strokeWidth = m * 0.014f,
            cap = StrokeCap.Round
        )
        drawLine(
            Color(0xFF8A6246),
            Offset(cx + w * 0.004f, h * 0.60f),
            Offset(cx + m * 0.125f, h * 0.49f),
            strokeWidth = m * 0.013f,
            cap = StrokeCap.Round
        )

        // качели
        val swingX = cx - m * 0.115f + sway * m * 0.004f
        val swingTop = h * 0.49f
        val swingSeat = h * 0.645f
        drawLine(Color(0xFFB49B7A), Offset(swingX - m * 0.028f, swingTop), Offset(swingX - m * 0.030f, swingSeat), strokeWidth = m * 0.004f)
        drawLine(Color(0xFFB49B7A), Offset(swingX + m * 0.040f, swingTop), Offset(swingX + m * 0.038f, swingSeat), strokeWidth = m * 0.004f)
        drawRoundRect(
            color = Color(0xFFC19A6B),
            topLeft = Offset(swingX - m * 0.040f, swingSeat),
            size = Size(m * 0.088f, m * 0.016f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(m * 0.008f)
        )

        // крона: тёмная подложка, затем объёмные пятна
        val canopy = Offset(cx + sway * m * 0.007f, h * 0.425f)
        val blobs = listOf(
            Triple(0.00f, 0.00f, 0.185f),
            Triple(-0.170f, 0.050f, 0.125f),
            Triple(0.175f, 0.045f, 0.130f),
            Triple(-0.020f, -0.140f, 0.125f),
            Triple(-0.100f, 0.135f, 0.100f),
            Triple(0.110f, 0.130f, 0.105f)
        )
        blobs.forEach { (ox, oy, rr) ->
            val c = Offset(canopy.x + ox * m, canopy.y + oy * m + m * 0.012f)
            drawCircle(deep, radius = rr * m * 1.03f, center = c)
        }
        blobs.forEach { (ox, oy, rr) ->
            val c = Offset(canopy.x + ox * m, canopy.y + oy * m)
            val r = rr * m
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lerp(light, Color.White, 0.18f), mid),
                    center = Offset(c.x - r * 0.32f, c.y - r * 0.34f),
                    radius = r * 1.55f
                ),
                radius = r,
                center = c
            )
        }

        // яблоки
        val spots = listOf(
            0.00f to 0.02f,
            -0.13f to -0.06f,
            0.13f to -0.02f,
            -0.06f to 0.13f,
            0.08f to 0.12f,
            -0.185f to 0.05f,
            0.19f to 0.06f,
            0.02f to -0.155f,
            -0.10f to 0.085f,
            0.115f to -0.125f
        )
        val shown = (2 + (progress.coerceIn(0f, 1f) * 8f).toInt()).coerceAtMost(spots.size)
        spots.take(shown).forEach { (ox, oy) ->
            val c = Offset(canopy.x + ox * m, canopy.y + oy * m)
            val r = m * 0.017f
            drawCircle(apple, r, c)
            drawCircle(Color.White.copy(alpha = 0.45f), r * 0.32f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
            drawLine(
                Color(0xFF6B4B36),
                Offset(c.x, c.y - r),
                Offset(c.x + r * 0.35f, c.y - r * 1.6f),
                strokeWidth = m * 0.003f,
                cap = StrokeCap.Round
            )
        }

        // корзина с яблоками
        val basketX = cx + m * 0.235f
        val basketY = h * 0.765f
        val bw = m * 0.115f
        val bh = m * 0.062f
        drawRoundRect(
            color = Color(0xFFC9A073),
            topLeft = Offset(basketX - bw / 2, basketY),
            size = Size(bw, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(m * 0.014f)
        )
        repeat(3) { i ->
            drawLine(
                Color(0xFFB0885F),
                Offset(basketX - bw / 2 + bw * (i + 1) / 4f, basketY + m * 0.006f),
                Offset(basketX - bw / 2 + bw * (i + 1) / 4f, basketY + bh - m * 0.006f),
                strokeWidth = m * 0.0035f
            )
        }
        drawOval(
            color = Color(0xFFB98F63),
            topLeft = Offset(basketX - bw / 2 - m * 0.004f, basketY - m * 0.012f),
            size = Size(bw + m * 0.008f, m * 0.020f)
        )
        listOf(-0.22f, 0f, 0.22f).forEach { dx ->
            drawCircle(
                apple,
                m * 0.016f,
                Offset(basketX + dx * bw * 0.8f, basketY - m * 0.012f)
            )
        }

        // падающие листья
        repeat(3) { i ->
            val seed = i * 0.31f
            val t = (fall + seed) % 1f
            val lx = w * (0.30f + 0.44f * seed) + sin((t + seed) * 6.28f) * w * 0.035f
            val ly = h * 0.30f + t * h * 0.46f
            rotate(degrees = t * 260f + seed * 90f, pivot = Offset(lx, ly)) {
                drawOval(
                    color = lerp(light, deep, 0.35f).copy(alpha = 0.75f * (1f - t) + 0.15f),
                    topLeft = Offset(lx, ly),
                    size = Size(m * 0.020f, m * 0.010f)
                )
            }
        }
    }
}

private fun DrawScope.cloud(color: Color, c: Offset, r: Float) {
    drawOval(color, topLeft = Offset(c.x - r, c.y - r * 0.42f), size = Size(r * 2f, r * 0.84f))
    drawCircle(color, r * 0.52f, Offset(c.x - r * 0.55f, c.y - r * 0.26f))
    drawCircle(color, r * 0.68f, Offset(c.x + r * 0.10f, c.y - r * 0.34f))
    drawCircle(color, r * 0.48f, Offset(c.x + r * 0.72f, c.y - r * 0.06f))
}
