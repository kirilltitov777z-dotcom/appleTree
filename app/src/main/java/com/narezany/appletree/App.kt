package com.narezany.appletree

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.narezany.appletree.data.GameStore
import com.narezany.appletree.ui.screens.AuthScreen
import com.narezany.appletree.ui.screens.CountriesScreen
import com.narezany.appletree.ui.screens.GardenScreen
import com.narezany.appletree.ui.screens.OnboardingScreen
import com.narezany.appletree.ui.screens.ProfileScreen
import com.narezany.appletree.ui.screens.ShopScreen
import com.narezany.appletree.ui.theme.AppleTreeTheme
import com.narezany.appletree.ui.theme.CardCream
import com.narezany.appletree.ui.theme.Ink
import com.narezany.appletree.ui.theme.InkSoft
import com.narezany.appletree.ui.theme.LeafDeep
import kotlinx.coroutines.launch

class AppleTreeApp : Application()

private enum class Root { ONBOARDING, AUTH, MAIN }

private enum class Tab(val title: String, val icon: String) {
    GARDEN("Сад", "🌳"),
    COUNTRIES("Страны", "🏳"),
    SHOP("Магазин", "🪙"),
    PROFILE("Профиль", "🌤"),
}

@Composable
fun AppleTreeRoot() {
    val context = LocalContext.current
    val store = remember { GameStore(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var root by remember {
        mutableStateOf(
            when {
                store.isLoggedIn -> Root.MAIN
                !store.tutorialSeen -> Root.ONBOARDING
                else -> Root.AUTH
            }
        )
    }
    var tab by remember { mutableIntStateOf(0) }

    // сохраняем прогресс при уходе в фон и считаем доход садовника при возврате
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (store.isLoggedIn) {
                        store.profile.lastSeen = System.currentTimeMillis()
                        store.save()
                        scope.launch { store.pushProgressOnline() }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (store.isLoggedIn) store.applyOfflineIncome(store.profile)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = root == Root.MAIN && tab != 0) { tab = 0 }

    AppleTreeTheme {
        when (root) {
            Root.ONBOARDING -> OnboardingScreen(
                onFinish = {
                    store.tutorialSeen = true
                    root = if (store.isLoggedIn) Root.MAIN else Root.AUTH
                }
            )

            Root.AUTH -> AuthScreen(
                store = store,
                onAuthed = {
                    store.tutorialSeen = true
                    root = Root.MAIN
                }
            )

            Root.MAIN -> {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    ) {
                        when (Tab.entries[tab]) {
                            Tab.GARDEN -> GardenScreen(
                                store = store,
                                onOpenShop = { tab = Tab.SHOP.ordinal },
                                onOpenCountries = { tab = Tab.COUNTRIES.ordinal }
                            )

                            Tab.COUNTRIES -> CountriesScreen(store = store)
                            Tab.SHOP -> ShopScreen(store = store)
                            Tab.PROFILE -> ProfileScreen(
                                store = store,
                                onLogout = {
                                    tab = 0
                                    root = Root.AUTH
                                }
                            )
                        }
                    }
                    BottomBar(
                        selected = tab,
                        onSelect = { tab = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardCream)
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Tab.entries.forEachIndexed { index, item ->
                val active = index == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Color(0xFFEAF2E1) else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.icon, fontSize = 18.sp)
                    Text(
                        item.title,
                        fontSize = 10.sp,
                        color = if (active) LeafDeep else InkSoft,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "AppleTree · спокойный сад",
            fontSize = 9.sp,
            color = InkSoft.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            textAlign = TextAlign.Center
        )
    }
}
