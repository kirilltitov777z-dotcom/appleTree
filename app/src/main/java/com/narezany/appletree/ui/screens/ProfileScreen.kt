package com.narezany.appletree.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.data.GameStore
import com.narezany.appletree.ui.components.SoftCard
import com.narezany.appletree.ui.components.drawMiniAppleTree
import com.narezany.appletree.ui.theme.AppleRed
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.SkyBottom
import com.narezany.appletree.ui.theme.SkyMid
import com.narezany.appletree.ui.theme.SkyTop
import com.narezany.appletree.util.fmt
import com.narezany.appletree.util.parseColor
import com.narezany.appletree.util.stageName
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(store: GameStore, onLogout: () -> Unit) {
    val p = store.profile
    val rev = store.rev
    var showReset by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(store.serverUrl) }
    var msg by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScopeCompatP()

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Профиль", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("AppleTree 1.0", fontSize = 11.sp, color = InkSoft)
        }

        Spacer(Modifier.height(10.dp))
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE7F0DC)),
                    contentAlignment = Alignment.Center
                ) { Canvas(Modifier.size(56.dp)) { drawMiniAppleTree() } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        "${stageName(p.level)} · уровень ${p.level}",
                        fontSize = 12.sp, color = InkSoft
                    )
                    if (p.countryName != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x22FFFFFF).copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(Color(parseColor(p.countryColor)))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(p.countryName!!, fontSize = 10.sp, color = Ink)
                            if (p.isLeader) Text(" · лидер", fontSize = 10.sp, color = InkSoft)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat("🍎 всего", fmt(p.totalApples), Modifier.weight(1f))
                ProfileStat("🪙 монет", fmt(p.coins), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat("🗺 захвачено", "${p.captured}", Modifier.weight(1f))
                ProfileStat("🏳 страна", p.countryName ?: "нет", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))
        SoftCard {
            Text("Настройки", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            SettingRow("Звук сбора", p.sound) { store.setSound(it) }
            SettingRow("Вибрация", p.vibration) { store.setVibration(it) }
        }

        Spacer(Modifier.height(10.dp))
        SoftCard {
            Text("Сеть", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            Text(
                if (store.onlineEnabled) "подключено как ${p.serverNickname ?: p.nickname}"
                else "локальный режим — прогресс на телефоне",
                fontSize = 11.sp, color = InkSoft
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Адрес сервера") },
                placeholder = { Text("http://192.168.1.5:8080") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        store.serverUrl = serverUrl
                        scope.launch {
                            checking = true
                            val ok = com.narezany.appletree.data.ApiClient(serverUrl).ping()
                            checking = false
                            msg = if (ok) "Сервер на связи ✓" else "Сервер не отвечает"
                        }
                    },
                    enabled = !checking && serverUrl.isNotBlank(),
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C8DBC))
                ) { Text(if (checking) "…" else "Проверить", fontSize = 12.sp) }

                if (store.onlineEnabled) {
                    Button(
                        onClick = {
                            scope.launch {
                                checking = true
                                val err = store.refreshWorld()
                                checking = false
                                msg = err ?: "Мир синхронизирован ✓"
                            }
                        },
                        enabled = !checking,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
                    ) { Text("Синхр.", fontSize = 12.sp) }
                }
            }
            if (msg != null) {
                Spacer(Modifier.height(6.dp))
                Text(msg!!, fontSize = 11.sp, color = InkSoft)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Чтобы играть с другими людьми, подними server/ на любом ПК или хостинге и укажи адрес при входе.",
                fontSize = 10.sp, color = InkSoft, lineHeight = 14.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        SoftCard {
            Text("Аккаунт", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            ActionRow("Сменить аккаунт", "выйти и войти заново") { showLogout = true }
            ActionRow("Сбросить прогресс", "дерево начнётся заново", danger = true) { showReset = true }
            Spacer(Modifier.height(6.dp))
            Text(
                "Спокойная игра про яблоню. Сделано с заботой о тишине.",
                fontSize = 10.sp, color = InkSoft
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "AppleTree · Kotlin + Jetpack Compose",
            fontSize = 10.sp, color = InkSoft,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            containerColor = CardCream,
            title = { Text("Сбросить прогресс?") },
            text = { Text("Яблоки, монеты и улучшения обнулятся. Страна и карта останутся.") },
            confirmButton = {
                TextButton(onClick = { store.resetProgress(); showReset = false; msg = "Прогресс сброшен" }) {
                    Text("Сбросить", color = AppleRed)
                }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Отмена") } }
        )
    }
    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            containerColor = CardCream,
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Прогресс сохранится, войти можно будет по паролю.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    store.logout()
                    onLogout()
                }) { Text("Выйти", color = LeafDeep) }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Остаться") } }
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF2EFE4))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 9.sp, color = InkSoft)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Ink)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = LeafDeep)
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = if (danger) AppleRed else Ink)
            Text(subtitle, fontSize = 10.sp, color = InkSoft)
        }
        Text("›", fontSize = 18.sp, color = InkSoft)
    }
}

@Composable
private fun rememberCoroutineScopeCompatP() = androidx.compose.runtime.rememberCoroutineScope()
