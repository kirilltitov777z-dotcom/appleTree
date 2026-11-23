package com.narezany.appletree.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Клиент к своему Node-серверу (папка server/).
 * Работает без сторонних библиотек: HttpURLConnection + org.json.
 */
class ApiClient(private val baseUrl: String) : Backend {

    private fun requireUrl(): String {
        if (baseUrl.isBlank()) throw ApiException("Адрес сервера не задан")
        return baseUrl.trimEnd('/')
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null,
        timeoutMs: Int = 12_000,
    ): JSONObject {
        val url = URL(requireUrl() + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
            } ?: ""
            val json = if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }
                .getOrElse { JSONObject().put("error", text.take(200)) }
            if (code !in 200..299) {
                throw ApiException(json.optString("error", "Ошибка сервера ($code)"))
            }
            return json
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Нет связи с сервером: ${e.message ?: "ошибка"}")
        } finally {
            conn.disconnect()
        }
    }

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching { request("GET", "/api/ping", timeoutMs = 6000).optBoolean("ok") }
            .getOrDefault(false)
    }

    override suspend fun register(nickname: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val r = request(
                "POST", "/api/register",
                JSONObject().put("nickname", nickname).put("password", password)
            )
            AuthResult(r.getString("token"), r.optString("nickname", nickname))
        }

    override suspend fun login(nickname: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val r = request(
                "POST", "/api/login",
                JSONObject().put("nickname", nickname).put("password", password)
            )
            AuthResult(r.getString("token"), r.optString("nickname", nickname))
        }

    override suspend fun world(token: String): World = withContext(Dispatchers.IO) {
        val r = request("GET", "/api/world", token = token)
        val countries = mutableListOf<Country>()
        r.optJSONArray("countries")?.let { arr ->
            for (i in 0 until arr.length()) countries += Country.fromJson(arr.getJSONObject(i))
        }
        val regions = mutableListOf<Region>()
        r.optJSONArray("regions")?.let { arr ->
            for (i in 0 until arr.length()) regions += Region.fromJson(arr.getJSONObject(i))
        }
        val apps = mutableListOf<Application>()
        r.optJSONArray("applications")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                apps += Application(
                    id = o.optString("id"),
                    countryId = o.optString("countryId"),
                    countryName = o.optString("countryName"),
                    nickname = o.optString("nickname"),
                    createdAt = o.optLong("createdAt"),
                )
            }
        }
        val lb = mutableListOf<Country>()
        r.optJSONArray("leaderboard")?.let { arr ->
            for (i in 0 until arr.length()) lb += Country.fromJson(arr.getJSONObject(i))
        }
        World(
            countries = countries,
            regions = regions,
            applications = apps,
            leaderboard = lb,
            meCountryId = if (r.isNull("meCountryId")) null else r.optString("meCountryId"),
            meIsLeader = r.optBoolean("meIsLeader"),
        )
    }

    override suspend fun createCountry(
        token: String, name: String, color: String, emblem: String
    ): Country = withContext(Dispatchers.IO) {
        val r = request(
            "POST", "/api/countries", JSONObject()
                .put("name", name).put("color", color).put("emblem", emblem), token
        )
        Country.fromJson(r.getJSONObject("country"))
    }

    override suspend fun apply(token: String, countryId: String) = withContext(Dispatchers.IO) {
        request("POST", "/api/countries/$countryId/join", JSONObject(), token)
        Unit
    }

    override suspend fun acceptApplication(token: String, id: String) = withContext(Dispatchers.IO) {
        request("POST", "/api/applications/$id/accept", JSONObject(), token)
        Unit
    }

    override suspend fun declineApplication(token: String, id: String) = withContext(Dispatchers.IO) {
        request("POST", "/api/applications/$id/decline", JSONObject(), token)
        Unit
    }

    override suspend fun leaveCountry(token: String) = withContext(Dispatchers.IO) {
        request("POST", "/api/leave", JSONObject(), token)
        Unit
    }

    override suspend fun capture(token: String, regionId: String, amount: Long): JSONObject =
        withContext(Dispatchers.IO) {
            request("POST", "/api/capture", JSONObject().put("regionId", regionId).put("amount", amount), token)
        }

    override suspend fun pushScore(token: String, score: Long, captured: Int) = withContext(Dispatchers.IO) {
        request("POST", "/api/progress", JSONObject().put("score", score).put("captured", captured), token)
        Unit
    }

    override suspend fun leaderboard(token: String?): List<Country> = withContext(Dispatchers.IO) {
        val r = request("GET", "/api/leaderboard", token = token)
        val out = mutableListOf<Country>()
        r.optJSONArray("leaderboard")?.let { arr ->
            for (i in 0 until arr.length()) out += Country.fromJson(arr.getJSONObject(i))
        }
        out
    }

    override suspend fun sendChat(token: String, countryId: String, text: String) =
        withContext(Dispatchers.IO) {
            request(
                "POST", "/api/chat",
                JSONObject().put("countryId", countryId).put("text", text), token
            )
            Unit
        }

    override suspend fun chat(token: String, countryId: String, limit: Int): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            val r = request("GET", "/api/chat?countryId=${encode(countryId)}&limit=$limit", token = token)
            val out = mutableListOf<ChatMessage>()
            r.optJSONArray("messages")?.let { arr ->
                for (i in 0 until arr.length()) out += ChatMessage.fromJson(arr.getJSONObject(i))
            }
            out
        }

    override suspend fun developRegion(token: String, regionId: String, level: Int, invested: Long) =
        withContext(Dispatchers.IO) {
            request(
                "POST", "/api/develop",
                JSONObject().put("regionId", regionId).put("level", level).put("invested", invested), token
            )
            Unit
        }
}
