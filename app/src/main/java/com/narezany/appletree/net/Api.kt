package com.narezany.appletree.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class RemoteCountry(
    val id: String,
    val name: String,
    val color: Int,
    val leader: String,
    val members: Int,
    val apples: Long,
    val tiles: Int
)

data class OnlineState(
    val status: Status = Status.Offline,
    val message: String = "Оффлайн-сад",
    val best: Int = 0
) {
    enum class Status { Offline, Checking, Online, Error }

    val isOnline: Boolean get() = status == Status.Online
}

/**
 * Клиент к маленькому серверу appleTree (папка server/).
 * Никаких паролей по сети: сервер выдаёт токен по нику, пароль остаётся на телефоне.
 */
object Api {

    private const val TIMEOUT = 8_000

    suspend fun ping(base: String): Result<String> = runCatching {
        val json = call("GET", base, "/api/health", null, null)
        json.optString("version", "appleTree")
    }

    suspend fun guest(base: String, name: String): Result<String> = runCatching {
        val json = call("POST", base, "/api/guest", JSONObject().put("name", name), null)
        json.optString("token").takeIf { it.isNotEmpty() } ?: error("сервер не выдал токен")
    }

    suspend fun pushScore(
        base: String,
        token: String,
        name: String,
        apples: Long,
        lifetime: Long,
        tiles: Int,
        country: String
    ): Result<Int> = runCatching {
        val body = JSONObject()
            .put("name", name)
            .put("apples", apples)
            .put("lifetime", lifetime)
            .put("tiles", tiles)
            .put("country", country)
        val json = call("POST", base, "/api/score", body, token)
        json.optInt("rank", 0)
    }

    suspend fun countries(base: String): Result<List<RemoteCountry>> = runCatching {
        val json = call("GET", base, "/api/countries", null, null)
        val arr = json.optJSONArray("items") ?: return@runCatching emptyList()
        val out = mutableListOf<RemoteCountry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                RemoteCountry(
                    id = o.optString("id"),
                    name = o.optString("name", "Страна"),
                    color = o.optInt("color", 0xFF7FAE6B.toInt()),
                    leader = o.optString("leader", ""),
                    members = o.optInt("members", 1),
                    apples = o.optLong("apples", 0),
                    tiles = o.optInt("tiles", 0)
                )
            )
        }
        out
    }

    suspend fun createCountry(base: String, token: String, name: String, color: Int): Result<Unit> =
        runCatching {
            call(
                "POST", base, "/api/countries",
                JSONObject().put("name", name).put("color", color), token
            )
            Unit
        }

    private suspend fun call(
        method: String,
        base: String,
        path: String,
        body: JSONObject?,
        token: String?
    ): JSONObject = withContext(Dispatchers.IO) {
        val url = base.trim().trimEnd('/') + path
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw IOException(if (text.isBlank()) "HTTP $code" else text.take(160))
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
