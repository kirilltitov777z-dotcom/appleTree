package com.narezany.appletree.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min
import kotlin.random.Random

class GameStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("appletree", Context.MODE_PRIVATE)

    var state by mutableStateOf(load())
        private set

    /** Счётчик изменений: экраны читают его, чтобы обновиться. */
    var rev by mutableStateOf(0)
        private set

    val sessionNickname: String?
        get() = prefs.getString("session", null)

    var serverUrl: String
        get() = prefs.getString("serverUrl", AppConfig.SUPABASE_URL) ?: AppConfig.SUPABASE_URL
        set(value) {
            prefs.edit().putString("serverUrl", value.trim().trimEnd('/')).apply()
        }

    private fun api(): Backend = backendFor(serverUrl)

    val profile: Profile get() = state.profile

    var tutorialSeen: Boolean
        get() = prefs.getBoolean("tutorialSeen", false)
        set(value) {
            prefs.edit().putBoolean("tutorialSeen", value).apply()
        }

    fun setSound(on: Boolean) {
        profile.sound = on
        save()
    }

    fun setVibration(on: Boolean) {
        profile.vibration = on
        save()
    }

    val isLoggedIn: Boolean get() = sessionNickname != null && profile.nickname.isNotEmpty()

    // ---------- загрузка / сохранение ----------

    private fun load(): GameState {
        val raw = prefs.getString("state", null) ?: return freshState()
        return runCatching { GameState.fromJson(JSONObject(raw)) }.getOrElse { freshState() }
    }

    private fun freshState(): GameState {
        val st = GameState()
        st.regions = defaultRegions()
        seedCountries(st)
        return st
    }

    private fun seedCountries(st: GameState) {
        val now = System.currentTimeMillis()
        val seeds = listOf(
            Country("bot_1", "Тихие Сады", "#7FB069", "Мира", 24, 12400, 3, now, "🌿"),
            Country("bot_2", "Речная Долина", "#5C8DBC", "Илья", 17, 8600, 2, now, "🌊"),
            Country("bot_3", "Медовый Край", "#C9A227", "Вера", 11, 4300, 1, now, "🐝"),
        )
        st.countries = seeds.toMutableList()
        st.leaderboard = seeds.toMutableList()
        st.regions[1].apply {
            ownerId = "bot_1"; ownerName = "Тихие Сады"; ownerColor = "#7FB069"
        }
        st.regions[2].apply {
            ownerId = "bot_2"; ownerName = "Речная Долина"; ownerColor = "#5C8DBC"
        }
        st.regions[4].apply {
            ownerId = "bot_3"; ownerName = "Медовый Край"; ownerColor = "#C9A227"
        }
    }

    fun save() {
        prefs.edit().putString("state", state.toJson().toString()).apply()
        persistCurrentAccount()
        rev++
    }

    private fun accounts(): MutableList<JSONObject> {
        val raw = prefs.getString("accounts", "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until arr.length()).map { arr.getJSONObject(it) }.toMutableList()
    }

    private fun persistCurrentAccount() {
        val nick = sessionNickname ?: return
        val list = accounts()
        val idx = list.indexOfFirst { it.optString("nickname") == nick }
        val obj = JSONObject()
            .put("nickname", nick)
            .put("salt", profile.salt)
            .put("hash", profile.passHash)
            .put("profile", profile.toJson())
        if (idx >= 0) list[idx] = obj else list += obj
        prefs.edit().putString("accounts", JSONArray(list).toString()).apply()
    }

    // ---------- аккаунты ----------

    fun register(nickname: String, password: String, onError: (String) -> Unit): Boolean {
        val nick = nickname.trim()
        if (nick.length < 3) {
            onError("Ник должен быть не короче 3 символов")
            return false
        }
        if (password.length < 4) {
            onError("Пароль должен быть не короче 4 символов")
            return false
        }
        if (accounts().any { it.optString("nickname").equals(nick, ignoreCase = true) }) {
            onError("Такой садовник уже есть — попробуй войти")
            return false
        }
        val salt = Crypto.newSalt()
        val st = GameState(regions = defaultRegions())
        seedCountries(st)
        st.profile = Profile(
            id = UUID.randomUUID().toString(),
            nickname = nick,
            salt = salt,
            passHash = Crypto.hash(password, salt),
            lastSeen = System.currentTimeMillis(),
            tutorialDone = true,
        )
        state = st
        prefs.edit().putString("session", nick).apply()
        save()
        return true
    }

    fun login(nickname: String, password: String, onError: (String) -> Unit): Boolean {
        val nick = nickname.trim()
        val acc = accounts().firstOrNull { it.optString("nickname").equals(nick, ignoreCase = true) }
        if (acc == null) {
            onError("Аккаунт не найден. Зарегистрируйся")
            return false
        }
        val salt = acc.optString("salt")
        val hash = Crypto.hash(password, salt)
        if (hash != acc.optString("hash")) {
            onError("Неверный пароль")
            return false
        }
        loadAccount(acc)
        return true
    }

    /**
     * Вход на новом телефоне: сервер уже проверил пароль, создаём локальный профиль.
     */
    fun adoptServerAccount(nickname: String): Boolean {
        val nick = nickname.trim()
        if (nick.isEmpty()) return false
        val st = GameState(regions = defaultRegions())
        seedCountries(st)
        val shared = load()
        if (shared.countries.isNotEmpty()) st.countries = shared.countries
        if (shared.regions.isNotEmpty()) st.regions = shared.regions
        st.leaderboard = shared.leaderboard
        st.profile = Profile(
            nickname = nick,
            tutorialDone = true,
            lastSeen = System.currentTimeMillis(),
        )
        state = st
        prefs.edit().putString("session", nick).apply()
        save()
        return true
    }

    private fun loadAccount(acc: JSONObject) {
        val nick = acc.optString("nickname")
        val stored = acc.optJSONObject("profile")
        val st = if (stored != null) GameState.fromJson(JSONObject().put("profile", stored))
        else freshState()
        if (st.regions.isEmpty()) st.regions = defaultRegions()
        if (st.countries.isEmpty()) seedCountries(st)
        // общие страны и карта хранятся в общем состоянии
        val shared = load()
        st.countries = shared.countries.ifEmpty { mutableListOf<Country>().also { seedCountries(GameState(countries = it)) } }
        st.regions = shared.regions
        st.leaderboard = shared.leaderboard
        st.profile.nickname = nick
        st.profile.salt = acc.optString("salt")
        st.profile.passHash = acc.optString("hash")
        applyOfflineIncome(st.profile)
        state = st
        prefs.edit().putString("session", nick).apply()
        save()
    }

    fun logout() {
        persistCurrentAccount()
        prefs.edit().remove("session").apply()
        state = GameState(regions = defaultRegions()).also { seedCountries(it) }
        save()
    }

    fun resetProgress() {
        val p = profile
        val fresh = Profile(
            id = p.id,
            nickname = p.nickname,
            salt = p.salt,
            passHash = p.passHash,
            tutorialDone = true,
        )
        state.profile = fresh
        save()
    }

    // ---------- игра ----------

    var lastTapAt: Long = 0L
        private set

    fun canTap(): Boolean = System.currentTimeMillis() - lastTapAt >= profile.tapCooldownMs

    fun tapCooldownLeft(): Float {
        val cd = profile.tapCooldownMs
        if (cd <= 0L) return 0f
        val passed = System.currentTimeMillis() - lastTapAt
        return (1f - passed.toFloat() / cd).coerceIn(0f, 1f)
    }

    fun tap(): TapResult {
        if (!canTap()) return TapResult(0, false, false)
        lastTapAt = System.currentTimeMillis()
        val p = profile
        val boost = if (p.boostActive) 2 else 1
        val gained = p.tapValue * boost
        p.addApples(gained)
        p.growth += p.growthPerTap
        var leveled = false
        while (p.growth >= 1f) {
            p.growth -= 1f
            p.level++
            leveled = true
        }
        val crit = Random.nextInt(100) < p.critChance
        if (crit) p.addApples(gained)
        p.lastSeen = System.currentTimeMillis()
        save()
        return TapResult(gained * if (crit) 2 else 1, leveled, crit)
    }

    data class TapResult(val gained: Long, val leveledUp: Boolean, val crit: Boolean)

    fun buyUpgrade(kind: UpgradeKind): Boolean {
        val p = profile
        val cost = p.costOf(kind)
        if (p.coins < cost) return false
        p.coins -= cost
        p.applyUpgrade(kind)
        save()
        return true
    }

    fun sellApples(): Long {
        val p = profile
        val apples = p.apples
        if (apples < 10) return 0
        val coins = (apples * p.coinsPerApple).toLong().coerceAtLeast(1)
        p.apples = 0
        p.coins += coins
        save()
        return coins
    }

    fun buyBoost(hours: Int, price: Long): Boolean {
        val p = profile
        if (p.coins < price) return false
        p.coins -= price
        val from = maxOf(p.boostUntil, System.currentTimeMillis())
        p.boostUntil = from + hours * 3600_000L
        save()
        return true
    }

    fun applyOfflineIncome(p: Profile) {
        val now = System.currentTimeMillis()
        val seconds = ((now - p.lastSeen) / 1000).coerceIn(0, 8 * 3600)
        val perSec = p.gardenerPerSec + regionBonusPerSec()
        if (perSec > 0 && seconds > 60) {
            val earned = (perSec * seconds).toLong()
            p.addApples(earned)
        }
        p.lastSeen = now
    }

    // ---------- страны ----------

    val myCountry: Country?
        get() = profile.countryId?.let { id -> state.countries.firstOrNull { it.id == id } }

    fun createCountry(name: String, color: String, emblem: String, onError: (String) -> Unit): Boolean {
        val p = profile
        if (p.countryId != null) {
            onError("Ты уже в стране")
            return false
        }
        val n = name.trim()
        if (n.length < 3) {
            onError("Название страны — от 3 символов")
            return false
        }
        if (state.countries.any { it.name.equals(n, ignoreCase = true) }) {
            onError("Страна с таким названием уже есть")
            return false
        }
        if (p.coins < 100) {
            onError("Нужно 100 монет на основание страны")
            return false
        }
        p.coins -= 100
        val c = Country(
            id = "c_" + UUID.randomUUID().toString().take(8),
            name = n,
            color = color,
            leader = p.nickname,
            members = 1,
            emblem = emblem,
        )
        state.countries.add(c)
        state.leaderboard.add(c)
        p.countryId = c.id
        p.countryName = c.name
        p.countryColor = c.color
        p.isLeader = true
        save()
        return true
    }

    fun applyToCountry(countryId: String, onError: (String) -> Unit): Boolean {
        val p = profile
        if (p.countryId != null) {
            onError("Сначала выйди из текущей страны")
            return false
        }
        if (state.applications.any { it.countryId == countryId && it.nickname == p.nickname }) {
            onError("Заявка уже отправлена")
            return false
        }
        val c = state.countries.firstOrNull { it.id == countryId } ?: run {
            onError("Страна не найдена")
            return false
        }
        state.applications.add(
            Application(
                id = UUID.randomUUID().toString(),
                countryId = c.id,
                countryName = c.name,
                nickname = p.nickname,
                createdAt = System.currentTimeMillis(),
            )
        )
        save()
        return true
    }

    fun cancelApplication(id: String) {
        state.applications.removeAll { it.id == id }
        save()
    }

    fun acceptApplication(id: String) {
        val app = state.applications.firstOrNull { it.id == id } ?: return
        val c = state.countries.firstOrNull { it.id == app.countryId } ?: return
        c.members++
        state.applications.removeAll { it.id == id }
        // локальный игрок принимает себя (для офлайн-демо)
        if (app.nickname == profile.nickname) joinLocal(c)
        save()
    }

    fun declineApplication(id: String) {
        state.applications.removeAll { it.id == id }
        save()
    }

    private fun joinLocal(c: Country) {
        val p = profile
        p.countryId = c.id
        p.countryName = c.name
        p.countryColor = c.color
        p.isLeader = c.leader == p.nickname
    }

    fun leaveCountry() {
        val p = profile
        state.countries.firstOrNull { it.id == p.countryId }?.let { c ->
            c.members = (c.members - 1).coerceAtLeast(1)
            if (c.leader == p.nickname && c.members <= 1) {
                state.countries.remove(c)
                state.leaderboard.removeAll { it.id == c.id }
                state.regions.forEach {
                    if (it.ownerId == c.id) {
                        it.ownerId = null; it.ownerName = null; it.ownerColor = null
                    }
                }
            }
        }
        p.countryId = null
        p.countryName = null
        p.countryColor = null
        p.isLeader = false
        save()
    }

    // ---------- карта ----------

    fun captureCost(region: Region): Long {
        val base = 60L + profile.level * 12L
        return if (region.ownerId == null) base else (base * 1.5).toLong()
    }

    fun developCost(region: Region): Long = 60L * region.level * region.level

    /** Бонус захваченных земель: яблоки в секунду. */
    fun regionBonusPerSec(): Double =
        state.regions.filter { it.ownerId == profile.countryId }.sumOf { it.bonusPerSec }

    fun captureRegion(regionId: String): CaptureResult {
        val p = profile
        val country = myCountry ?: return CaptureResult(false, "Сначала вступи в страну")
        val region = state.regions.firstOrNull { it.id == regionId }
            ?: return CaptureResult(false, "Регион не найден")
        if (region.ownerId == country.id) return CaptureResult(false, "Это уже твоя земля")
        val cost = captureCost(region)
        if (p.apples < cost) return CaptureResult(false, "Нужно $cost 🍎")
        p.apples -= cost
        if (region.progressCountryId != country.id) {
            region.progressCountryId = country.id
            region.progress = 0
        }
        val step = 10 + p.level * 2
        region.progress = min(100, region.progress + step)
        if (region.progress >= 100) {
            region.ownerId = country.id
            region.ownerName = country.name
            region.ownerColor = country.color
            region.progress = 0
            region.progressCountryId = null
            country.regions++
            country.score += cost
            p.captured++
            if (p.captured % 3 == 0) p.coins += 25
            save()
            return CaptureResult(true, "${region.name} теперь часть ${country.name}!")
        }
        country.score += cost / 2
        save()
        return CaptureResult(false, "Захват: ${region.progress}%")
    }

    /** Прокачка своей территории: дороже, но даёт больше бонуса. */
    fun developRegion(regionId: String): String {
        val country = myCountry ?: return "Сначала вступи в страну"
        val region = state.regions.firstOrNull { it.id == regionId }
            ?: return "Регион не найден"
        if (region.ownerId != country.id) return "Развивать можно только свою землю"
        if (region.level >= 10) return "Это максимальный уровень региона"
        val cost = developCost(region)
        if (profile.apples < cost) return "Нужно $cost 🍎"
        profile.apples -= cost
        region.invested += cost
        region.level++
        country.score += cost
        save()
        return "${region.name} улучшен до ур. ${region.level} · +${region.bonus} бонус"
    }

    data class CaptureResult(val captured: Boolean, val message: String)

    // ---------- лидерборд ----------

    fun refreshLeaderboard() {
        val list = state.countries.sortedWith(
            compareByDescending<Country> { it.score }
                .thenByDescending { it.regions }
                .thenByDescending { it.members }
        )
        state.leaderboard = list.toMutableList()
    }

    fun worldApples(): Long = state.countries.sumOf { it.score }

    // ---------- онлайн ----------

    val onlineEnabled: Boolean
        get() = profile.serverToken != null && serverUrl.isNotBlank()

    fun attachServer(token: String, nickname: String) {
        profile.serverToken = token
        profile.serverNickname = nickname
        save()
    }

    /** Загружает общий мир с сервера. Возвращает текст ошибки или null. */
    suspend fun refreshWorld(): String? {
        val token = profile.serverToken ?: return "Нужно войти по сети"
        if (serverUrl.isBlank()) return "Адрес сервера не задан"
        return try {
            val world = api().world(token)
            if (world.countries.isNotEmpty() || world.regions.isNotEmpty()) {
                state.countries = world.countries.toMutableList()
                if (world.regions.isNotEmpty()) state.regions = world.regions.toMutableList()
                state.applications = world.applications.toMutableList()
                state.leaderboard = world.leaderboard.toMutableList()
            }
            profile.countryId = world.meCountryId
            profile.isLeader = world.meIsLeader
            val c = world.countries.firstOrNull { it.id == world.meCountryId }
            profile.countryName = c?.name
            profile.countryColor = c?.color
            profile.captured = state.regions.count { it.ownerId == world.meCountryId }
            save()
            null
        } catch (e: Exception) {
            e.message ?: "Не удалось загрузить мир"
        }
    }

    suspend fun createCountryOnline(name: String, color: String, emblem: String): String? {
        val token = profile.serverToken ?: return "Нужно войти по сети"
        if (profile.coins < 100) return "Нужно 100 монет на основание страны"
        if (state.countries.any { it.name.equals(name.trim(), ignoreCase = true) })
            return "Страна с таким названием уже есть"
        return try {
            profile.coins -= 100
            api().createCountry(token, name.trim(), color, emblem)
            refreshWorld()
        } catch (e: Exception) {
            profile.coins += 100
            e.message ?: "Не удалось создать страну"
        }
    }

    suspend fun applyOnline(countryId: String): String? {
        val token = profile.serverToken ?: return "Нужно войти по сети"
        return try {
            api().apply(token, countryId)
            refreshWorld()
        } catch (e: Exception) {
            e.message ?: "Не удалось отправить заявку"
        }
    }

    suspend fun acceptOnline(appId: String): String? = try {
        val token = profile.serverToken ?: return "Нужно войти по сети"
        api().acceptApplication(token, appId)
        refreshWorld()
    } catch (e: Exception) {
        e.message ?: "Ошибка"
    }

    suspend fun declineOnline(appId: String): String? = try {
        val token = profile.serverToken ?: return "Нужно войти по сети"
        api().declineApplication(token, appId)
        refreshWorld()
    } catch (e: Exception) {
        e.message ?: "Ошибка"
    }

    suspend fun leaveOnline(): String? = try {
        val token = profile.serverToken ?: return "Нужно войти по сети"
        api().leaveCountry(token)
        refreshWorld()
    } catch (e: Exception) {
        e.message ?: "Ошибка"
    }

    suspend fun captureOnline(regionId: String): CaptureResult {
        val token = profile.serverToken ?: return CaptureResult(false, "Нужно войти по сети")
        val region = state.regions.firstOrNull { it.id == regionId }
            ?: return CaptureResult(false, "Регион не найден")
        val country = myCountry ?: return CaptureResult(false, "Сначала вступи в страну")
        if (region.ownerId == country.id) return CaptureResult(false, "Это уже твоя земля")
        val cost = captureCost(region)
        if (profile.apples < cost) return CaptureResult(false, "Нужно $cost 🍎")
        return try {
            profile.apples -= cost
            profile.captured++
            val r = api().capture(token, regionId, cost)
            refreshWorld()
            if (r.optBoolean("captured")) {
                CaptureResult(true, r.optString("message", "Регион захвачен!"))
            } else {
                CaptureResult(false, r.optString("message", "Захват продолжается"))
            }
        } catch (e: Exception) {
            profile.apples += cost
            profile.captured = (profile.captured - 1).coerceAtLeast(0)
            CaptureResult(false, e.message ?: "Ошибка сети")
        }
    }

    suspend fun pushProgressOnline() {
        val token = profile.serverToken ?: return
        if (serverUrl.isBlank()) return
        runCatching { api().pushScore(token, profile.totalApples, profile.captured) }
    }

    // ---------- чат страны ----------

    fun sendChat(text: String): String? {
        val countryId = profile.countryId ?: return "Сначала вступи в страну"
        val clean = text.trim().take(300)
        if (clean.isEmpty()) return "Напиши сообщение"
        state.chat += ChatMessage(
            id = UUID.randomUUID().toString(),
            countryId = countryId,
            nickname = profile.nickname,
            text = clean,
        )
        if (state.chat.size > 100) {
            state.chat.removeAll(state.chat.take(state.chat.size - 100).toSet())
        }
        save()
        return null
    }

    suspend fun sendChatOnline(text: String): String? {
        val local = sendChat(text)
        if (local != null) return local
        val token = profile.serverToken ?: return null
        val countryId = profile.countryId ?: return null
        return try {
            api().sendChat(token, countryId, text.trim().take(300))
            null
        } catch (e: Exception) {
            e.message
        }
    }

    suspend fun loadChatOnline(): String? {
        val token = profile.serverToken ?: return null
        val countryId = profile.countryId ?: return null
        return try {
            val list = api().chat(token, countryId, 100)
            if (list.isNotEmpty()) {
                state.chat = list.toMutableList()
                save()
            }
            null
        } catch (e: Exception) {
            e.message
        }
    }

    suspend fun developRegionOnline(regionId: String): String {
        val local = developRegion(regionId)
        if (local.startsWith("Сначала") || local.startsWith("Регион") ||
            local.startsWith("Развивать") || local.startsWith("максимальный") ||
            local.startsWith("Нужно")
        ) return local
        val token = profile.serverToken ?: return local
        return try {
            val r = state.regions.first { it.id == regionId }
            api().developRegion(token, regionId, r.level, r.invested)
            local
        } catch (e: Exception) {
            e.message ?: local
        }
    }

    fun tick(seconds: Int = 1) {
        val p = profile
        val bonus = regionBonusPerSec()
        val gardener = p.gardenerPerSec + if (bonus > 0) bonus else 0.0
        if (gardener > 0) {
            val gain = (gardener * seconds * (if (p.boostActive) 2 else 1)).toLong()
            if (gain > 0) {
                p.addApples(gain)
                p.lastSeen = System.currentTimeMillis()
            }
        }
    }
}
