package com.narezany.appletree.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.game.Sky

/** 12 345 -> "12 345" */
fun fmt(n: Long): String {
    val s = n.toString()
    if (s.length <= 3) return s
    val sb = StringBuilder()
    var count = 0
    for (i in s.lastIndex downTo 0) {
        sb.append(s[i])
        count++
        if (count % 3 == 0 && i != 0) sb.append(' ')
    }
    return sb.reverse().toString()
}

fun fmtOne(v: Double): String = String.format(java.util.Locale.US, "%.1f", v)

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    fill: Color = Cream,
    pad: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(fill)
            .border(1.dp, CardLine, RoundedCornerShape(22.dp))
            .padding(pad),
        content = content
    )
}

@Composable
fun Bar(
    value: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    track: Color = Color(0xFFE7EEE1)
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(height / 2))
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.85f), color)
                    )
                )
        )
    }
}

@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ApplePill(apples: Long, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Cream)
            .border(1.dp, CardLine, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🍎", fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            fmt(apples),
            style = MaterialTheme.typography.titleMedium,
            color = Ink
        )
    }
}

@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = LeafGreen,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE2E9DC),
            disabledContentColor = Muted
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardLine)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Ink)
    }
}

@Composable
fun SectionTitle(text: String, hint: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = Ink)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ColorDot(color: Color, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(label, style = MaterialTheme.typography.bodySmall, color = InkSoft)
    }
}

@Composable
fun SkyBackdrop(
    sky: Sky,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(sky.top), Color(sky.bottom))
                )
            ),
        content = content
    )
}

/** Маленькое яблоко для логотипа и пустых экранов. */
@Composable
fun AppleMark(modifier: Modifier = Modifier, color: Color = AppleRed, leaf: Color = LeafDark) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) * 0.34f
        val c = Offset(w / 2f, h * 0.58f)
        drawCircle(color, radius = r, center = c)
        drawCircle(
            Color.White.copy(alpha = 0.35f),
            radius = r * 0.26f,
            center = Offset(c.x - r * 0.34f, c.y - r * 0.34f)
        )
        drawLine(
            Color(0xFF7A5640),
            start = Offset(w / 2f, h * 0.26f),
            end = Offset(w / 2f + w * 0.02f, h * 0.38f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )
        drawOval(
            brush = Brush.linearGradient(listOf(leaf, Color(0xFF7FAE6B))),
            topLeft = Offset(w * 0.52f, h * 0.2f),
            size = Size(w * 0.24f, h * 0.13f)
        )
    }
}

/** Тонкая подпись по центру. */
@Composable
fun CenterHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = InkSoft
    )
}

/** Пунктирная линия-разделитель. */
@Composable
fun DottedDivider(modifier: Modifier = Modifier, color: Color = CardLine) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        val step = 6.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x + step * 0.6f, 0f), strokeWidth = 1.dp.toPx())
            x += step
        }
    }
}

/** Заглушка для пустых списков. */
@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = InkSoft, textAlign = TextAlign.Center)
    }
}

@Composable
fun SoftStrokeBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, CardLine, RoundedCornerShape(18.dp)),
        content = content
    )
}
