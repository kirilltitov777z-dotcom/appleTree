package com.narezany.appletree.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.data.Application
import com.narezany.appletree.data.COUNTRY_COLORS
import com.narezany.appletree.data.COUNTRY_EMBLEMS
import com.narezany.appletree.data.Country
import com.narezany.appletree.data.GameStore
import com.narezany.appletree.data.Region
import com.narezany.appletree.ui.components.LongBar
import com.narezany.appletree.ui.components.SoftCard
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.Leaf
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.LeafLight
import com.narezany.appletree.ui.theme.SkyBottom
import com.narezany.appletree.ui.theme.SkyMid
import com.narezany.appletree.ui.theme.SkyTop
import com.narezany.appletree.util.fmt
import com.narezany.appletree.util.parseColor
import com.narezany.appletree.util.timeAgo
import kotlinx.coroutines.launch

@Composable
fun CountriesScreen(store: GameStore) {
    val scope = rememberCoroutineScope()
    val rev = store.rev
    val p = store.profile
    var tab by remember { mutableStateOf(1) } // 0 страны, 1 карта, 2 заявки
    var showCreate by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val selectedRegion = selected?.let { id -> store.state.regions.firstOrNull { it.id == id } }
    val myCountry = store.myCountry

    LaunchedEffect(Unit) {
        if (store.onlineEnabled) {
            refreshing = true
            reason = store.refreshWorld()
            store.loadChatOnline()
            refreshing = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Страны", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    if (store.onlineEnabled) "сеть: " + (p.serverNickname ?: "подключено")
                    else "локальный мир",
                    fontSize = 11.sp,
                    color = InkSoft
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (store.onlineEnabled) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xE6FFFFFF))
                            .clickable {
                                scope.launch {
                                    refreshing = true
                                    reason = store.refreshWorld()
                                    refreshing = false
                                    toast = reason ?: "Мир обновлён"
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (refreshing) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        else Text("⟳ обновить", fontSize = 12.sp, color = LeafDeep)
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF5C8DBC))
                        .clickable { tab = 1 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) { Text("Карта", fontSize = 12.sp, color = Color.White) }
            }
        }

        if (toast != null) {
            Text(
                toast!!,
                fontSize = 11.sp,
                color = InkSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE6FFFFFF))
                    .padding(8.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x33000000).copy(alpha = 0.08f))
                .padding(4.dp)
        ) {
            SegTab("Страны", tab == 0, Modifier.weight(1f)) { tab = 0 }
            SegTab("Карта", tab == 1, Modifier.weight(1f)) { tab = 1 }
            SegTab("Чат", tab == 3, Modifier.weight(1f)) { tab = 3 }
            SegTab("Заявки", tab == 2, Modifier.weight(1f)) { tab = 2 }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            when (tab) {
                0 -> CountriesTab(
                    store = store,
                    myCountry = myCountry,
                    onCreate = { showCreate = true },
                    onApply = { country ->
                        scope.launch {
                            busy = true
                            val err = if (store.onlineEnabled) store.applyOnline(country.id)
                            else {
                                store.applyToCountry(country.id) { reason = it }
                                null
                            }
                            busy = false
                            toast = err ?: "Заявка отправлена в ${country.name}"
                        }
                    },
                    onLeave = {
                        scope.launch {
                            if (store.onlineEnabled) {
                                val err = store.leaveOnline()
                                toast = err ?: "Ты вышел из страны"
                            } else {
                                store.leaveCountry()
                                toast = "Ты вышел из страны"
                            }
                        }
                    },
                    onOpenMap = { tab = 1 },
                    onOpenApps = { tab = 2 },
                    onOpenChat = { tab = 3 }
                )
                1 -> MapTab(
                    store = store,
                    selected = selectedRegion,
                    onSelect = { selected = it },
                    onCapture = {
                        val r = it
                        scope.launch {
                            busy = true
                            val res = if (store.onlineEnabled) store.captureOnline(r.id)
                            else store.captureRegion(r.id)
                            busy = false
                            toast = res.message
                            if (res.captured) selected = null
                        }
                    },
                    busy = busy,
                    online = store.onlineEnabled,
                    onDevelop = { r ->
                        scope.launch {
                            busy = true
                            toast = if (store.onlineEnabled) store.developRegionOnline(r.id)
                            else store.developRegion(r.id)
                            busy = false
                        }
                    }
                )
                3 -> ChatTab(
                    store = store,
                    onSend = { text ->
                        scope.launch {
                            val err = if (store.onlineEnabled) store.sendChatOnline(text)
                            else store.sendChat(text)
                            if (err != null) toast = err
                        }
                    },
                    onRefresh = {
                        scope.launch {
                            if (store.onlineEnabled) {
                                val err = store.loadChatOnline()
                                toast = err ?: "Чат обновлён"
                            } else toast = "Локальный чат"
                        }
                    }
                )
                2 -> ApplicationsTab(
                    store = store,
                    onAccept = { app ->
                        scope.launch {
                            if (store.onlineEnabled) store.acceptOnline(app.id)
                            else store.acceptApplication(app.id)
                            toast = "Заявка принята: ${app.nickname}"
                        }
                    },
                    onDecline = { app ->
                        scope.launch {
                            if (store.onlineEnabled) store.declineOnline(app.id)
                            else store.declineApplication(app.id)
                        }
                    },
                    onCancel = { app ->
                        store.cancelApplication(app.id)
                        toast = "Заявка отменена"
                    }
                )
            }
        }
    }

    if (showCreate) {
        CreateCountryDialog(
            coins = p.coins,
            onDismiss = { showCreate = false },
            onCreate = { name, color, emblem ->
                scope.launch {
                    busy = true
                    val err = if (store.onlineEnabled) store.createCountryOnline(name, color, emblem)
                    else {
                        var e: String? = null
                        store.createCountry(name, color, emblem) { e = it }
                        e
                    }
                    busy = false
                    if (err == null) {
                        showCreate = false
                        toast = "Страна основана"
                    } else {
                        reason = err
                    }
                }
            }
        )
    }

    if (reason != null) {
        AlertDialog(
            onDismissRequest = { reason = null },
            containerColor = CardCream,
            title = { Text("Не получилось", fontWeight = FontWeight.Bold) },
            text = { Text(reason!!) },
            confirmButton = {
                TextButton(onClick = { reason = null }) { Text("Понятно", color = LeafDeep) }
            }
        )
    }
}

@Composable
private fun SegTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) CardCream else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (selected) Ink else InkSoft,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ---------------- вкладка стран ----------------

@Composable
private fun CountriesTab(
    store: GameStore,
    myCountry: Country?,
    onCreate: () -> Unit,
    onApply: (Country) -> Unit,
    onLeave: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenChat: () -> Unit,
) {
    val p = store.profile
    val applications = store.state.applications

    if (myCountry != null) {
        SoftCard(Modifier.padding(top = 6.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(parseColor(myCountry.color))),
                    contentAlignment = Alignment.Center
                ) { Text(myCountry.emblem, fontSize = 24.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(myCountry.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        (if (p.isLeader) "твоя страна · ты лидер" else "твоя страна") +
                            " · ${myCountry.members} чел.",
                        fontSize = 11.sp,
                        color = InkSoft
                    )
                }
                Text("${myCountry.regions} 🗺", fontSize = 13.sp, color = Ink)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoBox("Очки", fmt(myCountry.score), Modifier.weight(1f))
                InfoBox("Захвачено", "${p.captured}", Modifier.weight(1f))
                InfoBox("Участников", "${myCountry.members}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
                ) { Text("К карте", fontSize = 14.sp) }
                Button(
                    onClick = onOpenChat,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C8DBC))
                ) { Text("💬 Чат", fontSize = 14.sp) }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (p.isLeader) {
                    TextButton(onClick = onOpenApps, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                        Text("Заявки ${if (applications.isNotEmpty()) "(${applications.size})" else ""}", fontSize = 12.sp, color = LeafDeep)
                    }
                } else Spacer(Modifier.size(1.dp))
                TextButton(onClick = onLeave, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                    Text("Выйти", color = InkSoft, fontSize = 12.sp)
                }
            }
        }
    } else {
        SoftCard(Modifier.padding(top = 6.dp, bottom = 10.dp)) {
            Text("У тебя пока нет страны", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
            Text(
                "Создай свою или подай заявку в существующую. Вместе захватывать регионы веселее.",
                fontSize = 12.sp,
                color = InkSoft
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
            ) { Text("Основать страну · 100 🪙", fontSize = 14.sp) }
        }
    }

    val countries = store.state.countries
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Все страны", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text("${countries.size}", fontSize = 12.sp, color = InkSoft)
    }
    countries.sortedByDescending { it.score }.forEach { c ->
        CountryRow(
            country = c,
            mine = c.id == p.countryId,
            pending = applications.any { it.countryId == c.id },
            onApply = { onApply(c) }
        )
    }
    if (countries.isEmpty()) {
        Text(
            "Пока ни одной страны. Стань первым основателем.",
            fontSize = 12.sp,
            color = InkSoft,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(10.dp))
    SoftCard {
        Text("Лучшие страны", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
        Spacer(Modifier.height(8.dp))
        store.state.leaderboard.take(5).forEachIndexed { i, c ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    listOf("🥇", "🥈", "🥉")[i.coerceAtMost(2)],
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(c.name, fontSize = 13.sp, color = Ink, modifier = Modifier.weight(1f), maxLines = 1)
                Text("${fmt(c.score)}", fontSize = 12.sp, color = LeafDeep, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("${c.regions} 🗺", fontSize = 11.sp, color = InkSoft)
            }
        }
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2EFE4))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 9.sp, color = InkSoft)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun CountryRow(
    country: Country,
    mine: Boolean,
    pending: Boolean,
    onApply: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE6FDFBF5))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(parseColor(country.color))),
            contentAlignment = Alignment.Center
        ) { Text(country.emblem, fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(country.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
            Text(
                "лидер ${country.leader} · ${country.members} чел. · ${country.regions} 🗺",
                fontSize = 10.sp,
                color = InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        if (mine) {
            Text("моя", fontSize = 11.sp, color = LeafDeep)
        } else if (pending) {
            Text("заявка", fontSize = 11.sp, color = InkSoft)
        } else {
            TextButton(onClick = onApply, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Вступить", fontSize = 11.sp, color = LeafDeep)
            }
        }
    }
}

// ---------------- вкладка карты ----------------

@Composable
private fun MapTab(
    store: GameStore,
    selected: Region?,
    onSelect: (String) -> Unit,
    onCapture: (Region) -> Unit,
    onDevelop: (Region) -> Unit,
    busy: Boolean,
    online: Boolean,
) {
    val p = store.profile
    val country = store.myCountry

    SoftCard(Modifier.padding(top = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Карта садов", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                Text("территории закрашены цветом страны", fontSize = 11.sp, color = InkSoft)
            }
            Text(
                if (online) "🌐 общая" else "🏠 локальная",
                fontSize = 11.sp,
                color = InkSoft
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    SoftCard(padding = 0) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height
                // море
                drawRect(Brush.verticalGradient(listOf(Color(0xFFD6ECF5), Color(0xFFBFDDEA))))
                // остров
                val island = Path().apply {
                    moveTo(cw * 0.13f, ch * 0.12f)
                    cubicTo(cw * 0.42f, ch * 0.02f, cw * 0.8f, ch * 0.05f, cw * 0.9f, ch * 0.22f)
                    cubicTo(cw * 0.99f, ch * 0.4f, cw * 0.94f, ch * 0.7f, cw * 0.82f, ch * 0.86f)
                    cubicTo(cw * 0.66f, ch * 0.99f, cw * 0.3f, ch * 0.98f, cw * 0.16f, ch * 0.84f)
                    cubicTo(cw * 0.02f, ch * 0.68f, cw * 0.02f, ch * 0.3f, cw * 0.13f, ch * 0.12f)
                    close()
                }
                drawPath(island, Brush.verticalGradient(listOf(Color(0xFFD7E8BE), Color(0xFFC3DCA6))))
                drawPath(island, Color(0x66FFFFFF), style = Stroke(width = 3f))
                // дороги-тропинки
                drawLine(
                    Color(0x22A08A5E),
                    Offset(cw * 0.3f, ch * 0.2f),
                    Offset(cw * 0.65f, ch * 0.8f),
                    strokeWidth = 2f
                )
            }

            store.state.regions.forEach { r ->
                // большая закрашенная территория — сразу видно, кому она принадлежит
                val d = w * (r.r / 100f) * 2.35f
                val x = w * (r.x / 100f) - d / 2
                val y = h * (r.y / 100f) - d / 2
                val ownerColor = Color(parseColor(r.ownerColor, 0xFFDCE9C5))
                val isSelected = selected?.id == r.id
                val mine = r.ownerId == store.profile.countryId
                Box(
                    Modifier
                        .offset(x = x, y = y)
                        .size(d)
                        .clip(CircleShape)
                        .background(
                            if (r.ownerId == null) Color(0xE8F6F0DF) else ownerColor.copy(alpha = 0.94f)
                        )
                        .border(
                            width = if (isSelected) 3.dp else if (mine) 2.dp else 1.dp,
                            color = if (isSelected) Ink else if (mine) LeafDeep else Color(0x66000000),
                            shape = CircleShape
                        )
                        .clickable { onSelect(r.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (r.ownerId != null) "🚩" else r.bonus,
                            fontSize = 15.sp
                        )
                        Text(
                            r.name.split(" ").first(),
                            fontSize = 9.sp,
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "ур.${r.level} ${r.bonus}" + if (r.progress > 0) " · ${r.progress}%" else "",
                            fontSize = 8.sp,
                            color = InkSoft,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    SoftCard {
        Text("Как читать карту", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink)
        Spacer(Modifier.height(6.dp))
        MapLegend(
            listOf(
                "твоя земля" to LeafDeep,
                "чужая" to Color(0xFF5C8DBC),
                "ничейная" to Color(0xFFDCE9C5),
            )
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "🍎 яблоки · 🪙 монеты · ⚡ урожай · 🌿 рост — бонусы регионов. " +
                "Захваченные земли приносят яблоки каждый секунду, их можно улучшать.",
            fontSize = 10.sp,
            color = InkSoft,
            lineHeight = 14.sp
        )
    }

    Spacer(Modifier.height(8.dp))

    if (selected != null) {
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(parseColor(selected.ownerColor, 0xFFD9D3C0))),
                    contentAlignment = Alignment.Center
                ) { Text(if (selected.ownerId != null) "🚩" else "🌾", fontSize = 18.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(selected.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Text(
                        selected.ownerName?.let { "страна: $it" } ?: "ничей регион",
                        fontSize = 11.sp,
                        color = InkSoft
                    )
                    Text(
                        "уровень ${selected.level} · бонус ${selected.bonus} ${selected.bonusName} " +
                            "+${String.format("%.1f", selected.bonusPerSec)}/с",
                        fontSize = 10.sp,
                        color = LeafDeep
                    )
                }
            }
            if (selected.progress > 0) {
                Spacer(Modifier.height(10.dp))
                Text("Захват: ${selected.progress}%", fontSize = 11.sp, color = InkSoft)
                Spacer(Modifier.height(4.dp))
                LongBar(selected.progress / 100f, height = 10, colors = listOf(Color(0xFFE0B56F), Color(0xFFC98A55)))
            }
            Spacer(Modifier.height(12.dp))
            if (country == null) {
                Text("Сначала вступи в страну на вкладке «Страны».", fontSize = 12.sp, color = InkSoft)
            } else if (selected.ownerId == country.id) {
                Text(
                    "Твоя земля. Чем выше уровень, тем больше бонус 🌿",
                    fontSize = 12.sp,
                    color = LeafDeep
                )
                Spacer(Modifier.height(10.dp))
                val devCost = store.developCost(selected)
                Button(
                    onClick = { onDevelop(selected) },
                    enabled = selected.level < 10 && p.apples >= devCost && !busy,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC98A55))
                ) {
                    Text(
                        if (selected.level >= 10) "Максимальный уровень"
                        else "Улучшить · ${fmt(devCost)} 🍎",
                        fontSize = 13.sp
                    )
                }
            } else {
                val cost = store.captureCost(selected)
                Button(
                    onClick = { onCapture(selected) },
                    enabled = p.apples >= cost && !busy,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text(
                        if (p.apples >= cost) "Захватить · ${fmt(cost)} 🍎"
                        else "Нужно ${fmt(cost)} 🍎",
                        fontSize = 14.sp
                    )
                }
            }
        }
    } else {
        SoftCard {
            Text("Регионы", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            store.state.regions.take(6).forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape)
                            .background(Color(parseColor(r.ownerColor, 0xFFD9D3C0)))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(r.name, fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f))
                    Text(r.ownerName ?: "ничей", fontSize = 10.sp, color = InkSoft)
                }
            }
            if (store.state.regions.size > 6) {
                Text("…и ещё ${store.state.regions.size - 6}", fontSize = 10.sp, color = InkSoft)
            }
        }
    }
}

// ---------------- чат страны ----------------

@Composable
private fun ChatTab(
    store: GameStore,
    onSend: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val p = store.profile
    var text by remember { mutableStateOf("") }
    val messages = store.state.chat.filter { it.countryId == p.countryId }

    SoftCard(Modifier.padding(top = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Чат страны", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                Text(
                    p.countryName?.let { "пишем в $it" } ?: "сначала вступи в страну",
                    fontSize = 11.sp,
                    color = InkSoft
                )
            }
            TextButton(onClick = onRefresh) { Text("⟳", fontSize = 18.sp, color = LeafDeep) }
        }
    }

    Spacer(Modifier.height(8.dp))

    if (p.countryId == null) {
        SoftCard {
            Text(
                "Чат доступен участникам страны. Открой вкладку «Страны» и вступи в одну из них.",
                fontSize = 12.sp,
                color = InkSoft
            )
        }
        return
    }

    SoftCard {
        if (messages.isEmpty()) {
            Text(
                "Пока тихо. Напиши первое сообщение — друзья увидят его, когда откроют чат.",
                fontSize = 12.sp,
                color = InkSoft
            )
        }
        messages.takeLast(50).forEach { m ->
            val mine = m.nickname == p.nickname
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(if (mine) 14.dp else 6.dp))
                    .background(if (mine) Color(0xFFE4EED7) else Color(0xFFF3F0E6))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalAlignment = if (mine) Alignment.End else Alignment.Start
            ) {
                Text(m.nickname, fontSize = 10.sp, color = LeafDeep, fontWeight = FontWeight.SemiBold)
                Text(m.text, fontSize = 13.sp, color = Ink)
                Text(timeAgo(m.createdAt), fontSize = 9.sp, color = InkSoft)
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(300) },
            placeholder = { Text("Сообщение…") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text.trim())
                    text = ""
                }
            },
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
        ) { Text("➤") }
    }
}

// ---------------- заявки ----------------

@Composable
private fun ApplicationsTab(
    store: GameStore,
    onAccept: (Application) -> Unit,
    onDecline: (Application) -> Unit,
    onCancel: (Application) -> Unit,
) {
    val p = store.profile
    val all = store.state.applications
    val mine = all.filter { it.nickname == p.nickname }

    if (p.isLeader) {
        SoftCard(Modifier.padding(top = 6.dp)) {
            Text("Заявки в страну", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            Text("принимай тех, кому доверяешь", fontSize = 11.sp, color = InkSoft)
        }
        Spacer(Modifier.height(8.dp))
        val incoming = all.filter { it.nickname != p.nickname }
        if (incoming.isEmpty()) {
            Text(
                "Пока новых заявок нет. Позови друзей — они найдут страну в списке.",
                fontSize = 12.sp,
                color = InkSoft,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
        incoming.forEach { app ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6FDFBF5))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFE7EDDD)),
                    contentAlignment = Alignment.Center
                ) { Text("🧑‍🌾", fontSize = 16.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.nickname, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        "${app.countryName} · ${timeAgo(app.createdAt)}",
                        fontSize = 10.sp, color = InkSoft
                    )
                }
                TextButton(onClick = { onAccept(app) }) { Text("✔", color = LeafDeep) }
                TextButton(onClick = { onDecline(app) }) { Text("✕", color = InkSoft) }
            }
        }
    } else {
        SoftCard(Modifier.padding(top = 6.dp)) {
            Text("Мои заявки", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            Text(
                if (p.countryId == null) "Ты пока не в стране" else "Ты уже в ${p.countryName}",
                fontSize = 11.sp, color = InkSoft
            )
        }
        Spacer(Modifier.height(8.dp))
        if (mine.isEmpty()) {
            Text(
                "Заявок нет. Выбери страну во вкладке «Страны».",
                fontSize = 12.sp,
                color = InkSoft,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
        mine.forEach { app ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6FDFBF5))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📨", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.countryName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("отправлена ${timeAgo(app.createdAt)}", fontSize = 10.sp, color = InkSoft)
                }
                TextButton(onClick = { onCancel(app) }) {
                    Text("Отменить", fontSize = 11.sp, color = InkSoft)
                }
            }
        }
    }

    if (p.countryId == null && store.state.countries.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        SoftCard {
            Text("Как вступить?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
            Text(
                "Открой «Страны», выбери понравившуюся и нажми «Вступить». Лидер страны увидит заявку и примет её.",
                fontSize = 12.sp, color = InkSoft
            )
        }
    }
}

// ---------------- создание страны ----------------

@Composable
private fun CreateCountryDialog(
    coins: Long,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(COUNTRY_COLORS.first()) }
    var emblem by remember { mutableStateOf(COUNTRY_EMBLEMS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardCream,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Основать страну", fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(18) },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions()
                )
                Spacer(Modifier.height(12.dp))
                Text("Цвет", fontSize = 12.sp, color = InkSoft)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    COUNTRY_COLORS.take(5).forEach { c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(parseColor(c)))
                                .border(
                                    width = if (c == color) 3.dp else 0.dp,
                                    color = Ink,
                                    shape = CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    COUNTRY_COLORS.drop(5).forEach { c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(parseColor(c)))
                                .border(
                                    width = if (c == color) 3.dp else 0.dp,
                                    color = Ink,
                                    shape = CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Символ", fontSize = 12.sp, color = InkSoft)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    COUNTRY_EMBLEMS.take(5).forEach { e ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (e == emblem) Color(0xFFE3EED6) else Color(0xFFF2EFE4))
                                .clickable { emblem = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 15.sp) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    COUNTRY_EMBLEMS.drop(5).forEach { e ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (e == emblem) Color(0xFFE3EED6) else Color(0xFFF2EFE4))
                                .clickable { emblem = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 15.sp) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Стоимость: 100 🪙 · у тебя ${fmt(coins)}",
                    fontSize = 11.sp,
                    color = if (coins >= 100) InkSoft else Color(0xFFB5544A)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, color, emblem) },
                enabled = coins >= 100 && name.trim().length >= 3
            ) { Text("Основать", color = LeafDeep, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = InkSoft) }
        }
    )
}

// показать легенду карты можно позже
@Composable
private fun MapLegend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 10.sp, color = InkSoft)
            }
        }
    }
}
