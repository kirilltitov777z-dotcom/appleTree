package com.narezany.appletree.data

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.pow

// ---------- Улучшения ----------

data class Upgrades(
    var tapPower: Int = 0,
    var tapSpeed: Int = 0,
    var gardener: Int = 0,
    var growth: Int = 0,
    var press: Int = 0,
) {
    fun toJson() = JSONObject()
        .put("tapPower", tapPower)
        .put("tapSpeed", tapSpeed)
        .put("gardener", gardener)
        .put("growth", growth)
        .put("press", press)

    companion object {
        fun fromJson(o: JSONObject) = Upgrades(
            tapPower = o.optInt("tapPower"),
            tapSpeed = o.optInt("tapSpeed"),
            gardener = o.optInt("gardener"),
            growth = o.optInt("growth"),
            press = o.optInt("press"),
        )
    }
}

enum class UpgradeKind(val title: String, val desc: String, val icon: String) {
    TAP_POWER("Сила сбора", "Больше яблок за одно нажатие", "🍎"),
    TAP_SPEED("Удача", "Чаще выпадает критический урожай ×2", "⚡"),
    GARDENER("Садовник", "Собирает яблоки, пока ты отдыхаешь", "🧺"),
    GROWTH("Удобрение", "Дерево растёт быстрее", "🌱"),
    PRESS("Пресс", "Выгоднее продавать яблоки", "🪙"),
}

// ---------- Игрок ----------

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    var nickname: String = "",
    var passHash: String = "",
    var salt: String = "",
    var apples: Long = 0,
    var coins: Long = 0,
    var totalApples: Long = 0,
    var level: Int = 1,
    var growth: Float = 0f,
    var upgrades: Upgrades = Upgrades(),
    var lastSeen: Long = System.currentTimeMillis(),
    var countryId: String? = null,
    var countryName: String? = null,
    var countryColor: String? = null,
    var isLeader: Boolean = false,
    var captured: Int = 0,
    var serverToken: String? = null,
    var serverNickname: String? = null,
    var boostUntil: Long = 0L,
    var sound: Boolean = true,
    var vibration: Boolean = true,
    var tutorialDone: Boolean = false,
) {
    /** Задержки больше нет — можно жать на дерево как угодно часто. */
    val tapCooldownMs: Long get() = 0L

    val critChance: Int
        get() = (6 + upgrades.tapSpeed * 5).coerceAtMost(60)

    val tapValue: Long
        get() = ((2 + upgrades.tapPower * 3) * (1f + (level - 1) * 0.08f)).toLong()
            .coerceAtLeast(1)

    val gardenerPerSec: Double
        get() = upgrades.gardener * 1.2 * (1.0 + upgrades.gardener * 0.08)

    val growthPerTap: Float
        get() = 0.06f * (1f + upgrades.growth * 0.3f)

    val growthNeeded: Float get() = 1f

    val coinsPerApple: Double
        get() = 0.12 * (1.0 + upgrades.press * 0.45)

    val boostActive: Boolean get() = boostUntil > System.currentTimeMillis()

    fun costOf(kind: UpgradeKind): Long {
        val lvl = levelOf(kind)
        val base = when (kind) {
            UpgradeKind.TAP_POWER -> 12.0
            UpgradeKind.TAP_SPEED -> 18.0
            UpgradeKind.GARDENER -> 25.0
            UpgradeKind.GROWTH -> 16.0
            UpgradeKind.PRESS -> 20.0
        }
        val factor = when (kind) {
            UpgradeKind.TAP_POWER -> 1.35
            UpgradeKind.TAP_SPEED -> 1.42
            UpgradeKind.GARDENER -> 1.45
            UpgradeKind.GROWTH -> 1.32
            UpgradeKind.PRESS -> 1.38
        }
        return (base * factor.pow(lvl)).toLong().coerceAtLeast(1)
    }

    fun levelOf(kind: UpgradeKind): Int = when (kind) {
        UpgradeKind.TAP_POWER -> upgrades.tapPower
        UpgradeKind.TAP_SPEED -> upgrades.tapSpeed
        UpgradeKind.GARDENER -> upgrades.gardener
        UpgradeKind.GROWTH -> upgrades.growth
        UpgradeKind.PRESS -> upgrades.press
    }

    fun applyUpgrade(kind: UpgradeKind) {
        when (kind) {
            UpgradeKind.TAP_POWER -> upgrades.tapPower++
            UpgradeKind.TAP_SPEED -> upgrades.tapSpeed++
            UpgradeKind.GARDENER -> upgrades.gardener++
            UpgradeKind.GROWTH -> upgrades.growth++
            UpgradeKind.PRESS -> upgrades.press++
        }
    }

    fun addApples(amount: Long) {
        if (amount <= 0) return
        apples += amount
        totalApples += amount
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("nickname", nickname)
        .put("passHash", passHash)
        .put("salt", salt)
        .put("apples", apples)
        .put("coins", coins)
        .put("totalApples", totalApples)
        .put("level", level)
        .put("growth", growth.toDouble())
        .put("upgrades", upgrades.toJson())
        .put("lastSeen", lastSeen)
        .put("countryId", countryId ?: JSONObject.NULL)
        .put("countryName", countryName ?: JSONObject.NULL)
        .put("countryColor", countryColor ?: JSONObject.NULL)
        .put("isLeader", isLeader)
        .put("captured", captured)
        .put("serverToken", serverToken ?: JSONObject.NULL)
        .put("serverNickname", serverNickname ?: JSONObject.NULL)
        .put("boostUntil", boostUntil)
        .put("sound", sound)
        .put("vibration", vibration)
        .put("tutorialDone", tutorialDone)

    companion object {
        fun fromJson(o: JSONObject) = Profile(
            id = o.optString("id", UUID.randomUUID().toString()),
            nickname = o.optString("nickname"),
            passHash = o.optString("passHash"),
            salt = o.optString("salt"),
            apples = o.optLong("apples"),
            coins = o.optLong("coins"),
            totalApples = o.optLong("totalApples"),
            level = o.optInt("level", 1).coerceAtLeast(1),
            growth = o.optDouble("growth", 0.0).toFloat(),
            upgrades = o.optJSONObject("upgrades")?.let { Upgrades.fromJson(it) } ?: Upgrades(),
            lastSeen = o.optLong("lastSeen", System.currentTimeMillis()),
            countryId = if (o.isNull("countryId")) null else o.optString("countryId"),
            countryName = if (o.isNull("countryName")) null else o.optString("countryName"),
            countryColor = if (o.isNull("countryColor")) null else o.optString("countryColor"),
            isLeader = o.optBoolean("isLeader"),
            captured = o.optInt("captured"),
            serverToken = if (o.isNull("serverToken")) null else o.optString("serverToken"),
            serverNickname = if (o.isNull("serverNickname")) null else o.optString("serverNickname"),
            boostUntil = o.optLong("boostUntil"),
            sound = o.optBoolean("sound", true),
            vibration = o.optBoolean("vibration", true),
            tutorialDone = o.optBoolean("tutorialDone"),
        )
    }
}

// ---------- Страны и карта ----------

data class Country(
    val id: String,
    var name: String,
    var color: String,
    var leader: String,
    var members: Int = 1,
    var score: Long = 0,
    var regions: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var emblem: String = "🍎",
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("color", color)
        .put("leader", leader)
        .put("members", members)
        .put("score", score)
        .put("regions", regions)
        .put("createdAt", createdAt)
        .put("emblem", emblem)

    companion object {
        fun fromJson(o: JSONObject) = Country(
            id = o.optString("id"),
            name = o.optString("name"),
            color = o.optString("color", "#7FB069"),
            leader = o.optString("leader"),
            members = o.optInt("members", 1),
            score = o.optLong("score"),
            regions = o.optInt("regions"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            emblem = o.optString("emblem", "🍎"),
        )
    }
}

data class Region(
    val id: String,
    val name: String,
    var ownerId: String? = null,
    var ownerName: String? = null,
    var ownerColor: String? = null,
    var progressCountryId: String? = null,
    var progress: Int = 0, // 0..100
    var level: Int = 1,
    var invested: Long = 0,
    val bonus: String = "🍎",
    // координаты доли в процентах от области карты
    val x: Float,
    val y: Float,
    val r: Float,
) {
    /** Бонус региона в яблоках в секунду (только для владельца). */
    val bonusPerSec: Double get() = level * 0.5

    val bonusName: String
        get() = when (bonus) {
            "🪙" -> "монеты"
            "⚡" -> "урожай"
            "🌿" -> "рост"
            else -> "яблоки"
        }

    fun toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("ownerId", ownerId ?: JSONObject.NULL)
        .put("ownerName", ownerName ?: JSONObject.NULL)
        .put("ownerColor", ownerColor ?: JSONObject.NULL)
        .put("progressCountryId", progressCountryId ?: JSONObject.NULL)
        .put("progress", progress)
        .put("level", level)
        .put("invested", invested)
        .put("bonus", bonus)
        .put("x", x.toDouble())
        .put("y", y.toDouble())
        .put("r", r.toDouble())

    companion object {
        fun fromJson(o: JSONObject) = Region(
            id = o.optString("id"),
            name = o.optString("name"),
            ownerId = if (o.isNull("ownerId")) null else o.optString("ownerId"),
            ownerName = if (o.isNull("ownerName")) null else o.optString("ownerName"),
            ownerColor = if (o.isNull("ownerColor")) null else o.optString("ownerColor"),
            progressCountryId = if (o.isNull("progressCountryId")) null else o.optString("progressCountryId"),
            progress = o.optInt("progress"),
            level = o.optInt("level", 1).coerceAtLeast(1),
            invested = o.optLong("invested"),
            bonus = o.optString("bonus", "🍎"),
            x = o.optDouble("x", 50.0).toFloat(),
            y = o.optDouble("y", 50.0).toFloat(),
            r = o.optDouble("r", 10.0).toFloat(),
        )
    }
}

data class ChatMessage(
    val id: String,
    val countryId: String,
    val nickname: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("countryId", countryId)
        .put("nickname", nickname)
        .put("text", text)
        .put("createdAt", createdAt)

    companion object {
        fun fromJson(o: JSONObject) = ChatMessage(
            id = o.optString("id"),
            countryId = o.optString("countryId"),
            nickname = o.optString("nickname"),
            text = o.optString("text"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        )
    }
}

data class Application(
    val id: String,
    val countryId: String,
    val countryName: String,
    val nickname: String,
    val createdAt: Long,
)

// ---------- Пароли ----------

object Crypto {
    private val rnd = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(16)
        rnd.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((salt + password).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

// ---------- Сериализация состояния ----------

data class GameState(
    var profile: Profile = Profile(),
    var countries: MutableList<Country> = mutableListOf(),
    var regions: MutableList<Region> = mutableListOf(),
    var applications: MutableList<Application> = mutableListOf(),
    var leaderboard: MutableList<Country> = mutableListOf(),
    var chat: MutableList<ChatMessage> = mutableListOf(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("profile", profile.toJson())
        .put("countries", JSONArray(countries.map { it.toJson() }))
        .put("regions", JSONArray(regions.map { it.toJson() }))
        .put("leaderboard", JSONArray(leaderboard.map { it.toJson() }))
        .put("chat", JSONArray(chat.map { it.toJson() }))

    companion object {
        fun fromJson(o: JSONObject): GameState {
            val countries = mutableListOf<Country>()
            o.optJSONArray("countries")?.let { arr ->
                for (i in 0 until arr.length()) countries += Country.fromJson(arr.getJSONObject(i))
            }
            val regions = mutableListOf<Region>()
            o.optJSONArray("regions")?.let { arr ->
                for (i in 0 until arr.length()) regions += Region.fromJson(arr.getJSONObject(i))
            }
            val lb = mutableListOf<Country>()
            o.optJSONArray("leaderboard")?.let { arr ->
                for (i in 0 until arr.length()) lb += Country.fromJson(arr.getJSONObject(i))
            }
            val chat = mutableListOf<ChatMessage>()
            o.optJSONArray("chat")?.let { arr ->
                for (i in 0 until arr.length()) chat += ChatMessage.fromJson(arr.getJSONObject(i))
            }
            return GameState(
                profile = o.optJSONObject("profile")?.let { Profile.fromJson(it) } ?: Profile(),
                countries = countries,
                regions = regions.ifEmpty { defaultRegions() },
                leaderboard = lb,
                chat = chat,
            )
        }
    }
}

fun defaultRegions(): MutableList<Region> = mutableListOf(
    Region("north", "Северный сад", x = 30f, y = 18f, r = 13f, bonus = "🍎"),
    Region("pine", "Сосновый край", x = 62f, y = 15f, r = 11f, bonus = "🌿"),
    Region("lake", "Озёрный берег", x = 82f, y = 30f, r = 12f, bonus = "🪙"),
    Region("valley", "Тихая долина", x = 18f, y = 40f, r = 14f, bonus = "🍎"),
    Region("orchard", "Большой сад", x = 45f, y = 38f, r = 15f, bonus = "⚡"),
    Region("hills", "Яблочные холмы", x = 72f, y = 48f, r = 13f, bonus = "🍎"),
    Region("river", "Речные луга", x = 25f, y = 62f, r = 14f, bonus = "🪙"),
    Region("field", "Пшеничные поля", x = 52f, y = 63f, r = 13f, bonus = "🌿"),
    Region("coast", "Тёплый берег", x = 80f, y = 68f, r = 12f, bonus = "🪙"),
    Region("south", "Южная роща", x = 33f, y = 82f, r = 12f, bonus = "🍎"),
    Region("meadow", "Медовый луг", x = 60f, y = 86f, r = 13f, bonus = "🌿"),
    Region("cape", "Мыс Ветров", x = 86f, y = 87f, r = 10f, bonus = "⚡"),
)

val COUNTRY_COLORS = listOf(
    "#E07A5F", "#7FB069", "#5C8DBC", "#C9A227", "#9B7EBD",
    "#4E9A8E", "#D08C60", "#8A9A5B", "#B56576", "#6C8EBF",
)

val COUNTRY_EMBLEMS = listOf("🍎", "🌿", "🌳", "☀", "🐝", "🦌", "🕊", "🌊", "🍂", "⛰")
