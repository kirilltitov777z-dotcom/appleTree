package com.narezany.appletree.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.data.GameStore
import com.narezany.appletree.data.UpgradeKind
import com.narezany.appletree.ui.components.SoftCard
import com.narezany.appletree.ui.theme.AppleRed
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.SkyBottom
import com.narezany.appletree.ui.theme.SkyMid
import com.narezany.appletree.ui.theme.SkyTop
import com.narezany.appletree.util.boostLeft
import com.narezany.appletree.util.fmt

@Composable
fun ShopScreen(store: GameStore) {
    val p = store.profile
    val rev = store.rev
    var notice by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Магазин сада", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("тихие улучшения без суеты", fontSize = 11.sp, color = InkSoft)
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFF9E4)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🪙", fontSize = 14.sp)
                Text(" ${fmt(p.coins)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)
        ) {
            if (notice != null) {
                Text(
                    notice!!,
                    fontSize = 12.sp,
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE6FFFFFF))
                        .padding(10.dp)
                )
            }

            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍎", fontSize = 28.sp)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Урожай", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                        Text(
                            "в закромах ${fmt(p.apples)} яблок",
                            fontSize = 11.sp, color = InkSoft
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val coinsForAll = (p.apples * p.coinsPerApple).toLong()
                Button(
                    onClick = {
                        val got = store.sellApples()
                        notice = if (got > 0) "Продано яблок: +${fmt(got)} 🪙" else "Нужно хотя бы 10 яблок"
                    },
                    enabled = p.apples >= 10,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC98A55))
                ) {
                    Text(
                        if (p.apples >= 10) "Продать всё · +${fmt(coinsForAll)} 🪙"
                        else "Собери 10 яблок",
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Курс: ${String.format("%.2f", p.coinsPerApple * 10)} монеты за 10 яблок · пресс повышает курс",
                    fontSize = 10.sp, color = InkSoft
                )
            }

            Spacer(Modifier.height(10.dp))

            SoftCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ускорение ×2", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                        Text(
                            if (p.boostActive) "активно ещё ${boostLeft(p.boostUntil)}" else "все нажатия и садовник дают вдвое больше",
                            fontSize = 11.sp, color = InkSoft
                        )
                    }
                    Text("✨", fontSize = 26.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            notice = if (store.buyBoost(1, 150)) "Ускорение ×2 на час включено"
                            else "Не хватает монет"
                        },
                        enabled = p.coins >= 150,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
                    ) { Text("1 час · 150", fontSize = 13.sp) }
                    Button(
                        onClick = {
                            notice = if (store.buyBoost(3, 380)) "Ускорение ×2 на 3 часа включено"
                            else "Не хватает монет"
                        },
                        enabled = p.coins >= 380,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C8DBC))
                    ) { Text("3 часа · 380", fontSize = 13.sp) }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Прокачка",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            UpgradeKind.entries.forEach { kind ->
                UpgradeCard(store = store, kind = kind, onBought = { ok ->
                    notice = if (ok) "${kind.title}: уровень ${store.profile.levelOf(kind)}"
                    else "Не хватает монет — продай яблоки"
                })
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))
            SoftCard {
                Text("Как это работает", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• «Сила сбора» — больше яблок за нажатие\n" +
                        "• «Ловкость» — короче задержка между нажатиями\n" +
                        "• «Садовник» — собирает яблоки сам, даже когда игра закрыта\n" +
                        "• «Удобрение» — быстрее заполняется полоска роста\n" +
                        "• «Пресс» — выгоднее продавать урожай",
                    fontSize = 12.sp,
                    color = InkSoft,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Яблоки — валюта на карте мира, монеты — на прокачку.",
                    fontSize = 11.sp, color = AppleRed
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(store: GameStore, kind: UpgradeKind, onBought: (Boolean) -> Unit) {
    val p = store.profile
    val level = p.levelOf(kind)
    val cost = p.costOf(kind)
    val can = p.coins >= cost

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardCream)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1EEE3)),
            contentAlignment = Alignment.Center
        ) { Text(kind.icon, fontSize = 20.sp) }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(kind.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                Spacer(Modifier.size(6.dp))
                Text("ур. $level", fontSize = 10.sp, color = InkSoft)
            }
            Text(kind.desc, fontSize = 10.sp, color = InkSoft, lineHeight = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(effectText(store, kind), fontSize = 11.sp, color = LeafDeep)
        }
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = { onBought(store.buyUpgrade(kind)) },
            enabled = can,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = if (can) LeafDeep else Color(0xFFD8D3C4)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("${fmt(cost)} 🪙", fontSize = 11.sp)
        }
    }
}

private fun effectText(store: GameStore, kind: UpgradeKind): String {
    val p = store.profile
    return when (kind) {
        UpgradeKind.TAP_POWER -> "сейчас: +${fmt(p.tapValue)} за нажатие"
        UpgradeKind.TAP_SPEED -> "задержка: ${p.tapCooldownMs} мс"
        UpgradeKind.GARDENER -> "сейчас: +${String.format("%.1f", p.gardenerPerSec)} яблок/с"
        UpgradeKind.GROWTH -> "полоска: +${String.format("%.1f", p.growthPerTap * 100)}% за нажатие"
        UpgradeKind.PRESS -> "курс: ${String.format("%.2f", p.coinsPerApple)} 🪙 за яблоко"
    }
}
