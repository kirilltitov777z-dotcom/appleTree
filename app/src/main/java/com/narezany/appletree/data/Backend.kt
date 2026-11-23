package com.narezany.appletree.data

import org.json.JSONObject
import kotlin.math.max

/** Настройки бэкенда. Ключ publishable/anon можно держать в приложении. */
object AppConfig {
    const val SUPABASE_URL = "https://yfcwrdxeihsbggtmemcf.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_lwFAGdagCXMHaSpsL3Yqpg_jAhPshiQ"
}

class ApiException(message: String) : Exception(message)

data class AuthResult(val token: String, val nickname: String)

data class World(
    val countries: List<Country>,
    val regions: List<Region>,
    val applications: List<Application>,
    val leaderboard: List<Country>,
    val meCountryId: String?,
    val meIsLeader: Boolean,
)

/** Общий интерфейс: свой Node-сервер или Supabase. */
interface Backend {
    suspend fun ping(): Boolean
    suspend fun register(nickname: String, password: String): AuthResult
    suspend fun login(nickname: String, password: String): AuthResult
    suspend fun world(token: String): World
    suspend fun createCountry(token: String, name: String, color: String, emblem: String): Country
    suspend fun apply(token: String, countryId: String)
    suspend fun acceptApplication(token: String, id: String)
    suspend fun declineApplication(token: String, id: String)
    suspend fun leaveCountry(token: String)
    suspend fun capture(token: String, regionId: String, amount: Long): JSONObject
    suspend fun pushScore(token: String, score: Long, captured: Int)
    suspend fun leaderboard(token: String?): List<Country>
    suspend fun sendChat(token: String, countryId: String, text: String)
    suspend fun chat(token: String, countryId: String, limit: Int): List<ChatMessage>
    suspend fun developRegion(token: String, regionId: String, level: Int, invested: Long)
}

fun backendFor(url: String): Backend {
    val clean = url.trim().trimEnd('/')
    return if (clean.contains("supabase.co", ignoreCase = true)) {
        SupabaseBackend(clean, AppConfig.SUPABASE_KEY)
    } else {
        ApiClient(clean)
    }
}

internal fun supabaseError(text: String, code: Int): String {
    val err = runCatching { JSONObject(text) }.getOrNull()
    val msg = err?.optString("message").orEmpty()
    if (err?.optString("code") == "PGRST205") {
        return "База не настроена: вставь supabase/schema.sql в SQL Editor Supabase"
    }
    if (!msg.isNullOrBlank()) {
        return when {
            msg.contains("duplicate key", true) -> "Уже существует"
            msg.contains("violates foreign key", true) -> "Связанные данные не найдены"
            msg.contains("row-level security", true) -> "Нет доступа к базе"
            else -> msg
        }
    }
    return "Ошибка сервера ($code)"
}

internal fun encode(v: String): String = java.net.URLEncoder.encode(v, "UTF-8")

internal fun jsonArray(text: String): org.json.JSONArray =
    if (text.isBlank()) org.json.JSONArray() else org.json.JSONArray(text)

internal fun maxLong(a: Long, b: Long): Long = max(a, b)
