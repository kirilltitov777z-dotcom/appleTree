package com.narezany.appletree.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narezany.appletree.data.Account
import com.narezany.appletree.data.Auth
import com.narezany.appletree.data.Store
import com.narezany.appletree.net.Api
import com.narezany.appletree.net.OnlineState
import com.narezany.appletree.net.RemoteCountry
import com.narezany.appletree.ui.fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

enum class Tab(val title: String) {
    Garden("Сад"),
    Countries("Страны"),
    Map("Карта"),
    Shop("Лавка"),
    More("Ещё")
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val rnd = Random(System.currentTimeMillis().toInt())

    var save by mutableStateOf(store.loadSave())
        private set

    /** Текущая сессия. null — нужно войти по паролю. */
    var account by mutableStateOf<Account?>(null)
        private set

    var hasAccount by mutableStateOf(store.loadAccount() != null)
        private set

    var tab by mutableStateOf(Tab.Garden)
    var notice by mutableStateOf<String?>(null)
        private set

    var tapCount by mutableStateOf(0)
        private set
    var lastTapAt by mutableStateOf(0L)
        private set

    var online by mutableStateOf(OnlineState())
        private set
    var onlineCountries by mutableStateOf<List<RemoteCountry>>(emptyList())
        private set
    var connecting by mutableStateOf(false)
        private set

    private var passiveAcc = 0.0
    private var tickCount = 0
    private var savedAt = 0L

    init {
        val now = System.currentTimeMillis()
        val away = ((now - save.lastSeen) / 1000L).coerceIn(0L, 12L * 3600L)
        val gain = (perSec() * away).roundToLong()
        if (save.tutorial && gain > 20L) {
            save = save.copy(apples = save.apples + gain, lifetime = save.lifetime + gain)
            val text = "Пока тебя не было, сад собрал ${fmt(gain)} 🍎"
            viewModelScope.launch {
                delay(900)
                notice = text
            }
        }
        save = save.copy(lastSeen = now)

        val server = save.server
        if (server.isNotEmpty() && save.tutorial) {
            viewModelScope.launch {
                delay(1200)
                connect(server, quiet = true)
            }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
    }

    // ---------------------------------------------------------------- счётчики

    fun level(id: String): Int = save.level(id)

    fun upgradeCost(id: String): Long {
        val u = Upgrades.byId(id) ?: return 0
        return Upgrades.cost(u, level(id))
    }

    fun tapPower(): Long = 1L + level("power") + save.gardenLevel / 2L

    fun cooldownMs(): Long = (340L - level("agility") * 22L).coerceAtLeast(60L)

    fun ringPerTap(): Float = 0.05f + level("ring") * 0.006f

    fun ringPerSec(): Float = 0.0028f + level("ring") * 0.0016f

    fun harvestReward(): Long = 15L + level("harvest") * 9L + save.gardenLevel * 2L

    fun perSec(): Double {
        val tiles = myTileCount()
        return level("gardener") * 0.4 + tiles * 0.25 + save.gardenLevel * 0.08
    }

    fun xpNeed(level: Int = save.gardenLevel): Long =
        (70.0 * level.toDouble().pow(1.4)).roundToLong().coerceAtLeast(50L)

    fun xpProgress(): Float =
        (save.xp.toFloat() / xpNeed().toFloat()).coerceIn(0f, 1f)

    fun canTap(): Boolean = System.currentTimeMillis() - lastTapAt >= cooldownMs()

    fun myTileCount(): Int = save.map.count { it.value == "mine" }

    fun myTileSet(): Set<Int> = save.map.filterValues { it == "mine" }.keys

    fun countryTiles(id: String): Int = save.map.count { it.value == id }

    fun tileCost(index: Int): Long {
        val owned = myTileCount()
        val base = (45.0 * (owned + 1).toDouble().pow(1.25)).roundToLong()
        return if (save.map.containsKey(index)) base * 2 else base
    }

    fun captureAllowed(index: Int): Boolean {
        if (save.countryName == null) return false
        if (!MapData.isLand(index)) return false
        if (save.map[index] == "mine") return false
        val owned = myTileSet()
        return owned.isEmpty() || index in MapData.capturable(owned)
    }

    // ---------------------------------------------------------------- действия

    fun tap(): Long {
        val now = System.currentTimeMillis()
        if (now - lastTapAt < cooldownMs()) return 0L

        val gain = tapPower()
        lastTapAt = now
        tapCount++

        var ring = save.ring + ringPerTap()
        var apples = save.apples + gain
        var lifetime = save.lifetime + gain
        var levelMsg: String? = null

        if (ring >= 1f) {
            val bonus = harvestReward()
            ring = 0f
            apples += bonus
            lifetime += bonus
            levelMsg = "Полное кольцо: +${fmt(bonus)} 🍎"
            addXpInternal(12L)
        }

        save = save.copy(apples = apples, lifetime = lifetime, ring = ring.coerceIn(0f, 1f))
        addXpInternal(1L)
        if (levelMsg != null) notice = levelMsg
        if (now - savedAt > 4000L) saveNow()
        return gain
    }

    private fun tick() {
        tickCount++
        val now = System.currentTimeMillis()

        val rate = perSec()
        if (rate > 0.0) {
            passiveAcc += rate
            val whole = passiveAcc.toLong()
            if (whole > 0L) {
                passiveAcc -= whole
                save = save.copy(apples = save.apples + whole, lifetime = save.lifetime + whole)
                addXpInternal(whole / 3L)
            }
        }

        if (save.ring < 1f && ringPerSec() > 0f) {
            save = save.copy(ring = (save.ring + ringPerSec()).coerceAtMost(1f))
        }
        if (save.ring >= 1f) {
            val bonus = harvestReward()
            save = save.copy(
                apples = save.apples + bonus,
                lifetime = save.lifetime + bonus,
                ring = 0f
            )
            addXpInternal(12L)
            notice = "Кольцо созрело само: +${fmt(bonus)} 🍎"
        }

        // заявка в ботовую страну: лидер отвечает через несколько секунд
        val pending = save.pending
        if (pending != null && now - save.pendingAt >= 12_000L) {
            val c = Bots.byId(pending)
            if (c != null) {
                save = save.copy(
                    countryName = c.name,
                    countryColor = c.color,
                    countryMotto = c.motto,
                    countryLeader = false,
                    pending = null
                )
                notice = "«${c.name}» приняла тебя в сад 🌿"
                saveNow()
            }
        }

        // лидеру приходят заявки
        if (save.countryLeader && save.countryName != null && save.apps.size < 4 && tickCount % 27 == 0) {
            val pool = Bots.memberNames.filter { name -> save.apps.none { it.name == name } }
            if (pool.isNotEmpty()) {
                val name = pool.random()
                save = save.copy(apps = save.apps + CountryApp(name, now))
                notice = "$name просится в «${save.countryName}»"
            }
        }

        // соседи потихоньку расширяют луга
        if (tickCount % 45 == 0) {
            val c = Bots.countries.random()
            if (countryTiles(c.id) < 9) {
                val free = MapData.landTiles().filter { it !in save.map }
                if (free.isNotEmpty()) {
                    val near = free.filter { t -> MapData.neighbors(t).any { save.map[it] == c.id } }
                    val pick = if (near.isNotEmpty()) near.random() else free.random()
                    save = save.copy(map = save.map + (pick to c.id))
                }
            }
        }

        if (tickCount % 60 == 0 && online.isOnline) pushScoreQuiet()
        if (tickCount % 12 == 0) saveNow()
    }

    private fun addXpInternal(amount: Long) {
        if (amount <= 0L) return
        var xp = save.xp + amount
        var lvl = save.gardenLevel
        var need = xpNeed(lvl)
        var grown = false
        while (xp >= need && lvl < 200) {
            xp -= need
            lvl++
            need = xpNeed(lvl)
            grown = true
        }
        save = save.copy(xp = xp, gardenLevel = lvl)
        if (grown) notice = "Сад вырос до уровня $lvl 🌳"
    }

    // ---------------------------------------------------------------- магазин

    fun buyUpgrade(id: String) {
        val u = Upgrades.byId(id) ?: return
        val lvl = level(id)
        if (lvl >= u.max) {
            notice = "«${u.title}» уже на максимуме"
            return
        }
        val cost = upgradeCost(id)
        if (save.apples < cost) {
            notice = "Нужно ${fmt(cost)} 🍎 — собери ещё"
            return
        }
        save = save.copy(
            apples = save.apples - cost,
            levels = save.levels + (id to lvl + 1)
        )
        saveNow()
    }

    fun buySky(id: String) {
        val s = Skins.sky(id)
        if (id in save.ownsSky) {
            equipSky(id)
            return
        }
        if (save.apples < s.price) {
            notice = "Нужно ${fmt(s.price)} 🍎"
            return
        }
        save = save.copy(apples = save.apples - s.price, ownsSky = save.ownsSky + id, sky = id)
        saveNow()
        notice = "Небо «${s.title}» открыто"
    }

    fun equipSky(id: String) {
        if (id !in save.ownsSky) return
        save = save.copy(sky = id)
        saveNow()
    }

    fun buyTree(id: String) {
        val t = Skins.tree(id)
        if (id in save.ownsTree) {
            equipTree(id)
            return
        }
        if (save.apples < t.price) {
            notice = "Нужно ${fmt(t.price)} 🍎"
            return
        }
        save = save.copy(apples = save.apples - t.price, ownsTree = save.ownsTree + id, tree = id)
        saveNow()
        notice = "«${t.title}» растёт в саду"
    }

    fun equipTree(id: String) {
        if (id !in save.ownsTree) return
        save = save.copy(tree = id)
        saveNow()
    }

    // ---------------------------------------------------------------- аккаунт

    fun register(name: String, password: String): String? {
        if (store.loadAccount() != null) return "Аккаунт уже есть — войди по паролю"
        val nick = name.trim()
        if (nick.length < 3) return "Имя — хотя бы 3 буквы"
        if (nick.length > 16) return "Имя — не длиннее 16 букв"
        if (password.length < 4) return "Пароль — хотя бы 4 символа"
        val acc = Auth.create(nick, password)
        store.writeAccount(acc)
        account = acc
        hasAccount = true
        save = save.copy(tutorial = true)
        saveNow()
        if (online.isOnline) ensureTokenQuiet()
        return null
    }

    fun login(password: String): String? {
        val acc = store.loadAccount() ?: return "Аккаунта ещё нет — зарегистрируйся"
        if (!Auth.check(acc, password)) return "Пароль не подходит"
        account = acc
        hasAccount = true
        if (save.ring == 0f && save.tutorial) notice = "С возвращением, ${acc.name} 🌿"
        if (online.isOnline) ensureTokenQuiet()
        return null
    }

    fun knownName(): String? = store.loadAccount()?.name

    fun logout() {
        saveNow()
        account = null
        notice = "Вышел. Сад и прогресс на месте"
    }

    fun deleteEverything() {
        store.wipe()
        account = null
        hasAccount = false
        save = Save()
        online = OnlineState()
        onlineCountries = emptyList()
        tab = Tab.Garden
        notice = "Всё начато заново"
    }

    fun finishTutorial() {
        save = save.copy(tutorial = true)
        saveNow()
    }

    // ---------------------------------------------------------------- страны

    fun createCountry(name: String, color: Int, motto: String): String? {
        if (save.countryName != null) return "Ты уже в стране"
        val n = name.trim()
        if (n.length < 3) return "Название — хотя бы 3 буквы"
        if (n.length > 20) return "Название — до 20 букв"
        if (Bots.countries.any { it.name.equals(n, ignoreCase = true) }) return "Страна с таким названием уже есть"
        if (color == 0) return "Выбери цвет"
        save = save.copy(
            countryName = n,
            countryColor = color,
            countryMotto = motto.trim().take(40),
            countryLeader = true,
            pending = null,
            apps = emptyList()
        )
        saveNow()
        notice = "Страна «$n» основана 🌳"
        if (online.isOnline) {
            val base = save.server
            val token = store.token
            if (token != null) {
                viewModelScope.launch {
                    Api.createCountry(base, token, n, color).onFailure {
                        online = online.copy(message = "Страна создана локально, сервер молчит")
                    }
                }
            }
        }
        return null
    }

    fun applyToCountry(id: String) {
        if (save.countryName != null) {
            notice = "Сначала выйди из своей страны"
            return
        }
        val c = Bots.byId(id) ?: return
        if (save.pending == id) {
            notice = "Заявка уже у лидера"
            return
        }
        save = save.copy(pending = id, pendingAt = System.currentTimeMillis())
        saveNow()
        notice = "Заявка в «${c.name}» отправлена ✉️"
    }

    fun cancelApplication() {
        if (save.pending == null) return
        save = save.copy(pending = null, pendingAt = 0L)
        saveNow()
        notice = "Заявка отозвана"
    }

    fun resolveApplication(name: String, accept: Boolean) {
        save = save.copy(apps = save.apps.filterNot { it.name == name })
        if (accept) {
            save = save.copy(apples = save.apples + 40, lifetime = save.lifetime + 40)
            notice = "$name теперь в «${save.countryName}» · +40 🍎"
        } else {
            notice = "Заявка $name отклонена"
        }
        saveNow()
    }

    fun leaveCountry() {
        val name = save.countryName ?: return
        save = save.copy(
            countryName = null,
            countryColor = 0,
            countryMotto = "",
            countryLeader = true,
            apps = emptyList(),
            pending = null,
            map = save.map.filterValues { it != "mine" }
        )
        saveNow()
        notice = "Ты покинул «$name»"
    }

    // ---------------------------------------------------------------- карта

    fun capture(index: Int) {
        if (save.countryName == null) {
            notice = "Сначала создай страну или вступи в чужую"
            return
        }
        if (!MapData.isLand(index)) {
            notice = "Здесь вода — только яблони по суше 🌊"
            return
        }
        if (save.map[index] == "mine") {
            notice = "Этот луг уже твой"
            return
        }
        val owned = myTileSet()
        if (owned.isNotEmpty() && index !in MapData.capturable(owned)) {
            notice = "Бери только соседние луга"
            return
        }
        val cost = tileCost(index)
        if (save.apples < cost) {
            notice = "Нужно ${fmt(cost)} 🍎"
            return
        }
        val previous = save.map[index]?.let { Bots.byId(it)?.name }
        save = save.copy(
            apples = save.apples - cost,
            map = save.map + (index to "mine")
        )
        addXpInternal(cost / 2)
        notice = if (previous != null) "Луг отбит у «$previous»" else "Новый луг в саду 🌿"
        saveNow()
    }

    // ---------------------------------------------------------------- настройки

    fun setHaptics(on: Boolean) {
        save = save.copy(haptics = on)
        saveNow()
    }

    fun setCalm(on: Boolean) {
        save = save.copy(calm = on)
        saveNow()
    }

    fun connect(raw: String, quiet: Boolean = false) {
        val base = raw.trim().trimEnd('/')
        if (base.isEmpty()) {
            save = save.copy(server = "")
            saveNow()
            online = OnlineState()
            onlineCountries = emptyList()
            if (!quiet) notice = "Онлайн выключен — тихий оффлайн-сад"
            return
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            notice = "Адрес начинается с http:// или https://"
            return
        }
        save = save.copy(server = base)
        saveNow()
        connecting = true
        viewModelScope.launch {
            online = online.copy(status = OnlineState.Status.Checking, message = "Стучимся на сервер…")
            val res = Api.ping(base)
            connecting = false
            res.onSuccess { version ->
                online = online.copy(
                    status = OnlineState.Status.Online,
                    message = "Сервер на связи · $version"
                )
                if (!quiet) notice = "Онлайн включён 🌍"
                ensureToken(base)
                refreshOnline(base)
            }.onFailure { error ->
                online = online.copy(
                    status = OnlineState.Status.Error,
                    message = "Сервер не ответил: ${error.message ?: "ошибка"}"
                )
                if (!quiet) notice = "Сервер не ответил — сад продолжает жить оффлайн"
            }
        }
    }

    fun refreshOnline() {
        val base = save.server
        if (base.isEmpty()) {
            notice = "Сначала укажи адрес сервера"
            return
        }
        viewModelScope.launch { refreshOnline(base) }
    }

    private suspend fun refreshOnline(base: String) {
        Api.countries(base).onSuccess {
            onlineCountries = it
            if (online.status != OnlineState.Status.Online) {
                online = online.copy(status = OnlineState.Status.Online, message = "Сервер на связи")
            }
        }.onFailure {
            online = online.copy(status = OnlineState.Status.Error, message = "Нет связи с сервером")
        }
    }

    private fun ensureTokenQuiet() {
        val base = save.server
        if (base.isEmpty()) return
        if (store.token != null) {
            pushScoreQuiet()
            return
        }
        viewModelScope.launch {
            val name = (account ?: store.loadAccount())?.name ?: return@launch
            Api.guest(base, name).onSuccess { token ->
                store.token = token
                refreshOnline(base)
                pushScoreQuiet()
            }
        }
    }

    private fun ensureToken(base: String) {
        if (store.token != null) {
            pushScoreQuiet()
            return
        }
        viewModelScope.launch {
            val name = (account ?: store.loadAccount())?.name ?: return@launch
            Api.guest(base, name).onSuccess { token ->
                store.token = token
                pushScoreQuiet()
            }.onFailure {
                online = online.copy(message = "Сервер не выдал токен — только локальная игра")
            }
        }
    }

    private fun pushScoreQuiet() {
        val base = save.server
        val token = store.token ?: return
        if (base.isEmpty()) return
        val name = (account ?: store.loadAccount())?.name ?: return
        viewModelScope.launch {
            Api.pushScore(
                base, token, name, save.apples, save.lifetime, myTileCount(), save.countryName ?: ""
            ).onSuccess { rank ->
                online = online.copy(best = rank)
            }
        }
    }

    fun saveNow() {
        val now = System.currentTimeMillis()
        save = save.copy(lastSeen = now)
        store.writeSave(save)
        savedAt = now
    }

    fun clearNotice() {
        notice = null
    }
}
