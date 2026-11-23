package com.narezany.appletree.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.data.GameStore
import com.narezany.appletree.ui.components.AppleTreeArt
import com.narezany.appletree.ui.components.LongBar
import com.narezany.appletree.ui.components.SoftCard
import com.narezany.appletree.ui.components.drawBackgroundHills
import com.narezany.appletree.ui.theme.AppleRed
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.Leaf
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.LeafLight
import com.narezany.appletree.ui.theme.SkyBottom
import com.narezany.appletree.ui.theme.SkyMid
import com.narezany.appletree.ui.theme.SkyTop
import com.narezany.appletree.util.Feedback
import com.narezany.appletree.util.boostLeft
import com.narezany.appletree.util.fmt
import com.narezany.appletree.util.stageName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val born: Long,
    val crit: Boolean,
)

@Composable
fun GardenScreen(
    store: GameStore,
    onOpenShop: () -> Unit,
    onOpenCountries: () -> Unit,
) {
    val context = LocalContext.current
    val feedback = remember { Feedback(context) }
    val p = store.profile
    val rev = store.rev

    var time by remember { mutableLongStateOf(0L) }
    var floats by remember { mutableStateOf(listOf<FloatingText>()) }
    var levelUpTo by remember { mutableStateOf<Int?>(null) }
    var showNoCountry by remember { mutableStateOf(false) }

    val treeScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScopeCompat()

    LaunchedEffect(Unit) {
        while (true) {
            delay(60)
            time++
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            store.tick(1)
        }
    }
    LaunchedEffect(floats) {
        while (floats.isNotEmpty()) {
            delay(120)
            val t = System.currentTimeMillis()
            floats = floats.filter { t - it.born < 900 }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)))
    ) {
        // фон: холмы, облака
        Canvas(Modifier.fillMaxSize()) {
            drawBackgroundHills(time / 10f)
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            // ---------- верхняя панель ----------
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CounterChip("🍎", fmt(p.apples), Color(0xFFFFF6EC))
                    CounterChip("🪙", fmt(p.coins), Color(0xFFFFF9E4))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xE6F7FBF4))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("ур. ${p.level}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LeafDeep)
                    }
                    Text(stageName(p.level), fontSize = 10.sp, color = InkSoft)
                }
            }

            // бейдж страны
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (p.countryName != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xE6FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("🏳 ${p.countryName}", fontSize = 11.sp, color = Ink)
                    }
                } else {
                    TextButton(onClick = onOpenCountries, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                        Text("Вступи в страну →", fontSize = 11.sp, color = InkSoft)
                    }
                }
            }

            // ---------- дерево ----------
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val fieldW = maxWidth
                val fieldH = maxHeight
                AppleTreeArt(
                    level = p.level,
                    growth = p.growth,
                    treeScale = 1f,
                    time = time / 10f,
                    highlight = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = treeScale.value
                            scaleY = treeScale.value
                        },
                    onTap = {
                        if (store.canTap()) {
                            val r = store.tap()
                            feedback.tap(p.sound, p.vibration, r.crit)
                            val id = System.currentTimeMillis() + (0..999).random()
                            floats = floats + FloatingText(
                                id = id,
                                text = "+${fmt(r.gained)}" + if (r.crit) " ✨" else "",
                                x = 0.35f + (0..30).random() / 100f,
                                y = 0.35f + (0..20).random() / 100f,
                                born = System.currentTimeMillis(),
                                crit = r.crit
                            )
                            if (r.leveledUp) {
                                levelUpTo = p.level
                                p.coins += 10
                                feedback.tap(p.sound, p.vibration, true)
                            }
                            scope.launch {
                                treeScale.snapTo(0.965f)
                                treeScale.animateTo(1f, tween(180))
                            }
                        } else {
                            scope.launch {
                                treeScale.animateTo(1.008f, tween(60))
                                treeScale.animateTo(1f, tween(80))
                            }
                        }
                    }
                )

                // всплывающие числа
                floats.forEach { f ->
                    val age = (System.currentTimeMillis() - f.born).coerceIn(0, 900)
                    val progress = age / 900f
                    Text(
                        f.text,
                        color = if (f.crit) Color(0xFFB07A2A) else AppleRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (f.crit) 24.sp else 20.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = fieldW * f.x - 20.dp,
                                y = fieldH * (f.y - progress * 0.18f)
                            )
                            .graphicsLayer { alpha = (1f - progress).coerceIn(0f, 1f) }
                    )
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "нажми на дерево — задержки больше нет",
                        fontSize = 12.sp,
                        color = Color(0xCC4E6B62)
                    )
                }
            }

            // ---------- панель прогресса ----------
            SoftCard(Modifier.padding(bottom = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Рост дерева", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                    Text(
                        "${(p.growth * 100).roundToInt()}% · ур. ${p.level}",
                        fontSize = 12.sp,
                        color = InkSoft
                    )
                }
                Spacer(Modifier.height(8.dp))
                LongBar(p.growth, height = 20)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Удача · крит ×2", fontSize = 11.sp, color = InkSoft)
                    Text("${p.critChance}%", fontSize = 11.sp, color = LeafDeep, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(5.dp))
                LongBar(
                    p.critChance / 60f,
                    height = 7,
                    colors = listOf(Color(0xFFD9A46A), Color(0xFFC98A55))
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStat("🍎 за нажатие", "+${fmt(p.tapValue)}", Modifier.weight(1f))
                    MiniStat(
                        "🧺 в секунду",
                        run {
                            val total = p.gardenerPerSec + store.regionBonusPerSec()
                            if (total > 0) "+${String.format("%.1f", total)}" else "—"
                        },
                        Modifier.weight(1f)
                    )
                    if (p.boostActive) {
                        MiniStat("✨ буст", boostLeft(p.boostUntil), Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenShop,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
                    ) {
                        Text("Улучшить сад", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onOpenCountries,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5C8DBC)
                        )
                    ) {
                        Text("Карта", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (levelUpTo != null) {
        AlertDialog(
            onDismissRequest = { levelUpTo = null },
            containerColor = CardCream,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Дерево выросло!", fontWeight = FontWeight.Bold, color = Ink) },
            text = {
                Text(
                    "Теперь это уровень ${levelUpTo}. Дерево стало выше и приносит больше яблок.\nНаграда: +10 🪙",
                    color = InkSoft
                )
            },
            confirmButton = {
                TextButton(onClick = { levelUpTo = null }) {
                    Text("Хорошо", color = LeafDeep, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
    if (showNoCountry) {
        AlertDialog(
            onDismissRequest = { showNoCountry = false },
            containerColor = CardCream,
            title = { Text("Сначала страна") },
            text = { Text("Создай страну или вступи в существующую — тогда сможешь захватывать регионы.") },
            confirmButton = {
                TextButton(onClick = { showNoCountry = false; onOpenCountries() }) {
                    Text("К странам", color = LeafDeep)
                }
            }
        )
    }
}

@Composable
private fun CounterChip(icon: String, value: String, bg: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1EEE3))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 9.sp, color = InkSoft, maxLines = 1)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
