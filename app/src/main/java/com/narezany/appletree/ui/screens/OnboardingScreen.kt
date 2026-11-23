package com.narezany.appletree.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.ui.components.AppleTreeArt
import com.narezany.appletree.ui.components.drawBackgroundHills
import com.narezany.appletree.ui.components.drawTree
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.Leaf
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.LeafLight
import com.narezany.appletree.ui.theme.SkyBottom
import com.narezany.appletree.ui.theme.SkyMid
import com.narezany.appletree.ui.theme.SkyTop
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class TutorialPage(
    val title: String,
    val text: String,
    val art: Int,
)

private val pages = listOf(
    TutorialPage(
        "Тихий сад",
        "AppleTree — спокойная игра про яблоню. Здесь не нужно спешить: дерево растёт, пока ты собираешь яблоки и возвращаешься.",
        0
    ),
    TutorialPage(
        "Собирай яблоки",
        "Нажимай на дерево. У каждого нажатия есть короткая задержка — длинная полоска показывает, когда снова можно собирать. Полоску роста тоже можно прокачивать.",
        1
    ),
    TutorialPage(
        "Страны и карта",
        "Создай свою страну или вступи в чужую по заявке. Вместе захватывайте регионы на карте и поднимайте страну в списке лучших.",
        2
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text("Пропустить", color = InkSoft)
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(30.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFCDE9FA), Color(0xFFF2FAFF))))
                    ) {
                        when (page.art) {
                            0 -> Canvas(Modifier.fillMaxSize()) {
                                drawBackgroundHills(0f)
                                drawTree(level = 8, growth = 0.4f, treeScale = 1f, time = 0f, highlight = false)
                            }
                            1 -> TickBarArt()
                            2 -> CountryArt()
                        }
                    }

                    Spacer(Modifier.height(26.dp))
                    Text(page.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        page.text,
                        fontSize = 15.sp,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .size(if (i == pager.currentPage) 22.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) LeafDeep else Color(0x55708A6A))
                    )
                }
            }

            Button(
                onClick = {
                    if (pager.currentPage < pages.lastIndex) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(54.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
            ) {
                Text(
                    if (pager.currentPage < pages.lastIndex) "Далее" else "Создать сад",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TickBarArt() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppleTreeArt(
                level = 6,
                growth = 0.2f,
                treeScale = 0.9f,
                time = 0f,
                highlight = false,
                modifier = Modifier.size(width = 260.dp, height = 190.dp),
                onTap = {}
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(Color(0xFFE3EED6))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.62f)
                        .height(20.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(LeafLight, LeafDeep)))
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("полоска роста", fontSize = 12.sp, color = InkSoft)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(Color(0x33FFFFFF))
            )
        }
    }
}

@Composable
private fun CountryArt() {
    Canvas(Modifier.fillMaxSize().padding(26.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFCBE3B8), w * 0.34f, Offset(w * 0.4f, h * 0.44f))
        drawCircle(Color(0xFFA9D0E8), w * 0.30f, Offset(w * 0.68f, h * 0.60f))
        drawCircle(Color(0xFFE6D3AE), w * 0.24f, Offset(w * 0.62f, h * 0.30f))
        drawCircle(Color(0xFFD8B5B0), w * 0.20f, Offset(w * 0.30f, h * 0.68f))
        drawCircle(Color.White.copy(alpha = 0.9f), w * 0.045f, Offset(w * 0.40f, h * 0.40f))
        drawCircle(Color.White.copy(alpha = 0.9f), w * 0.035f, Offset(w * 0.69f, h * 0.57f))
        drawCircle(Color.White.copy(alpha = 0.9f), w * 0.03f, Offset(w * 0.60f, h * 0.28f))
    }
}
