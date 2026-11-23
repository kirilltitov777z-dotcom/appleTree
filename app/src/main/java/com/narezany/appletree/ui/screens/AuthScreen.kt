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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.appletree.data.backendFor
import com.narezany.appletree.data.GameStore
import com.narezany.appletree.ui.components.drawMiniAppleTree
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.LeafDeep
import com.narezany.appletree.ui.theme.SkyBottom
import com.narezany.appletree.ui.theme.SkyMid
import com.narezany.appletree.ui.theme.SkyTop
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(store: GameStore, onAuthed: () -> Unit) {
    var registerMode by remember { mutableStateOf(true) }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(store.serverUrl.isNotBlank()) }
    var serverUrl by remember { mutableStateOf(store.serverUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        error = null
        if (nickname.trim().length < 3) {
            error = "Ник — минимум 3 символа"
            return
        }
        if (password.length < 4) {
            error = "Пароль — минимум 4 символа"
            return
        }
        if (registerMode && password != password2) {
            error = "Пароли не совпадают"
            return
        }
        busy = true
        scope.launch {
            var serverToken: String? = null
            var serverNick: String? = null
            if (online && serverUrl.isNotBlank()) {
                store.serverUrl = serverUrl
                try {
                    val res = if (registerMode) {
                        backendFor(serverUrl).register(nickname.trim(), password)
                    } else {
                        backendFor(serverUrl).login(nickname.trim(), password)
                    }
                    serverToken = res.token
                    serverNick = res.nickname
                } catch (e: Exception) {
                    busy = false
                    error = e.message ?: "Сервер недоступен"
                    return@launch
                }
            }
            val ok = if (registerMode) {
                store.register(nickname.trim(), password) { error = it }
            } else {
                var success = store.login(nickname.trim(), password) { error = it }
                if (!success && serverToken != null) {
                    // новый телефон: сервер пароль уже проверил
                    success = store.adoptServerAccount(nickname.trim())
                    if (success) error = null
                }
                success
            }
            if (ok) {
                if (serverToken != null) store.attachServer(serverToken, serverNick ?: nickname.trim())
                busy = false
                onAuthed()
            } else {
                busy = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(Modifier.size(90.dp)) { drawMiniAppleTree() }
            Text("AppleTree", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                "спокойный сад, который растёт вместе с тобой",
                fontSize = 13.sp,
                color = InkSoft,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardCream)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF0EEE5))
                        .padding(4.dp)
                ) {
                    AuthTab("Регистрация", registerMode, Modifier.weight(1f)) {
                        registerMode = true; error = null
                    }
                    AuthTab("Вход", !registerMode, Modifier.weight(1f)) {
                        registerMode = false; error = null
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(20); error = null },
                    label = { Text("Имя садовника") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(40); error = null },
                    label = { Text("Пароль") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { visible = !visible }) {
                            Text(if (visible) "🙈" else "👁", fontSize = 15.sp)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (registerMode) ImeAction.Next else ImeAction.Done
                    )
                )
                if (registerMode) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password2,
                        onValueChange = { password2 = it.take(40); error = null },
                        label = { Text("Пароль ещё раз") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Играть по сети", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(
                            "общие страны, карта и заявки",
                            fontSize = 11.sp,
                            color = InkSoft
                        )
                    }
                    Switch(
                        checked = online,
                        onCheckedChange = { online = it; error = null },
                        colors = SwitchDefaults.colors(checkedTrackColor = LeafDeep)
                    )
                }
                if (online) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it.trim() },
                        label = { Text("Адрес сервера") },
                        placeholder = { Text("http://192.168.1.5:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        error!!,
                        color = Color(0xFFB5544A),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8E7E3))
                            .padding(10.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { submit() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = LeafDeep)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (registerMode) "Посадить сад" else "Войти в сад",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    if (registerMode) "Уже есть сад? Нажми «Вход»"
                    else "Впервые здесь? Выбери «Регистрация»",
                    fontSize = 12.sp,
                    color = InkSoft,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Без сети игра тоже работает: прогресс хранится на телефоне.",
                fontSize = 11.sp,
                color = Color(0xFF4E6B62),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) CardCream else Color.Transparent)
            .padding(vertical = 9.dp)
            .then(
                if (!selected) Modifier else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = if (selected) Ink else InkSoft,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
                .clickableNoRipple(onClick)
                .padding(vertical = 0.dp)
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LeafDeep,
    unfocusedBorderColor = Color(0xFFD8D2C2),
    focusedLabelColor = LeafDeep,
    cursorColor = LeafDeep,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        indication = null,
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
    ) { onClick() }
