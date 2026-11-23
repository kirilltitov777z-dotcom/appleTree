package com.narezany.appletree.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.ui.theme.AppleRed
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.Leaf
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.LeafLight

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = CardCream,
    padding: Int = 16,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .border(1.dp, Color(0x14000000), RoundedCornerShape(22.dp))
            .padding(padding.dp),
        content = content
    )
}

@Composable
fun LongBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Int = 18,
    colors: List<Color> = listOf(LeafLight, Leaf, LeafDeep),
    track: Color = Color(0xFFE7EFDD),
) {
    val p by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 260),
        label = "bar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(p)
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(colors))
        )
    }
}

@Composable
fun StatPill(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = CardCream,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, fontSize = 15.sp)
        Column {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink, maxLines = 1)
            Text(label, fontSize = 10.sp, color = InkSoft, maxLines = 1)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ink)
        if (trailing != null) {
            Text(trailing, fontSize = 12.sp, color = InkSoft)
        }
    }
}

@Composable
fun RoundIconButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = CardCream,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 16.sp) }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.fillMaxWidth().padding(vertical = 18.dp),
        textAlign = TextAlign.Center,
        color = InkSoft,
        fontSize = 13.sp
    )
}

@Composable
fun AppleBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF3E8E2))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🍎", fontSize = 12.sp)
        Text(" $text", fontSize = 12.sp, color = AppleRed, fontWeight = FontWeight.SemiBold)
    }
}
