package com.narezany.appletree.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Прямая работа с Supabase (PostgREST) без своего сервера.
 * Авторизация — publishable/anon ключ, данные игроков — в таблицах players/countries/...
 */
class SupabaseBackend(private val baseUrl: String, private val key: String) : Backend {

    private val root = baseUrl.trimEnd('/') + "/rest/v1"

    private fun call(method: String, path: String, body: JSONObject? = null): String {
        val conn = (URL(root + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", key)
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Prefer", "return=representation")
            }
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
            if (code !in 200..299) throw ApiException(supabaseError(text, code))
            return text
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Нет связи с Supabase: ${e.message ?: "ошибка"}")
        } finally {
            conn.disconnect()
        }
    }

    private fun get(path: String): String = call("GET", path)

    private fun array(text: String): JSONArray = if (text.isBlank()) JSONArray() else JSONArray(text)

    private fun objects(text: String): List<JSONObject> {
        val arr = array(text)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    // ---------- маппинг ----------

    private fun country(o: JSONObject) = Country(
        id = o.optString("id"),
        name = o.optString("name"),
        color = o.optString("color", "#7FB069"),
        leader = o.optString("leader"),
        members = o.optInt("members", 1),
        score = o.optLong("score"),
        regions = o.optInt("regions"),
        createdAt = System.currentTimeMillis(),
        emblem = o.optString("emblem", "🍎"),
    )

    private val countryCols = "id,name,color,emblem,leader,members,score,regions"
    private val regionCols =
        "id,name,x,y,r,ownerId:owner_id,ownerName:owner_name,ownerColor:owner_color," +
            "progress,progressCountryId:progress_country_id,level,invested,bonus"

    private fun region(o: JSONObject) = Region.fromJson(o)

    private fun me(token: String): JSONObject? =
        objects(get("/players?token=eq.${encode(token)}&select=*&limit=1")).firstOrNull()

    private fun countryById(id: String): JSONObject? =
        objects(get("/countries?id=eq.${encode(id)}&select=$countryCols&limit=1")).firstOrNull()

    private fun playerByNick(nick: String): JSONObject? =
        objects(get("/players?nickname=eq.${encode(nick)}&select=*&limit=1")).firstOrNull()

    // ---------- API ----------

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching { get("/regions?select=id&limit=1"); true }.getOrDefault(false)
    }

    override suspend fun register(nickname: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val salt = Crypto.newSalt()
            val token = UUID.randomUUID().toString()
            val body = JSONObject()
                .put("nickname", nickname)
                .put("salt", salt)
                .put("pass_hash", Crypto.hash(password, salt))
                .put("token", token)
            try {
                val rows = array(call("POST", "/players", body))
                val row = if (rows.length() > 0) rows.getJSONObject(0) else JSONObject()
                AuthResult(row.optString("token", token), row.optString("nickname", nickname))
            } catch (e: ApiException) {
                val msg = e.message ?: ""
                if (msg.contains("Уже существует", true) || msg.contains("duplicate", true)) {
                    throw ApiException("Такой садовник уже есть — попробуй войти")
                }
                throw e
            }
        }

    override suspend fun login(nickname: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val row = playerByNick(nickname) ?: throw ApiException("Аккаунт не найден")
            val salt = row.optString("salt")
            if (Crypto.hash(password, salt) != row.optString("pass_hash")) {
                throw ApiException("Неверный пароль")
            }
            val token = UUID.randomUUID().toString()
            call(
                "PATCH", "/players?nickname=eq.${encode(nickname)}",
                JSONObject().put("token", token)
            )
            AuthResult(token, row.optString("nickname", nickname))
        }

    override suspend fun world(token: String): World = withContext(Dispatchers.IO) {
        val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
        val countries = objects(get("/countries?select=$countryCols&order=score.desc"))
            .map(::country)
        val regions = objects(get("/regions?select=$regionCols&order=id.asc")).map(::region)
        val names = countries.associate { it.id to it.name }

        val myCountryId = if (player.isNull("country_id")) null else player.optString("country_id")
        val myCountry = countries.firstOrNull { it.id == myCountryId }
        val isLeader = myCountry != null && myCountry.leader == player.optString("nickname")

        val appsPath = if (isLeader && myCountryId != null) {
            "/applications?country_id=eq.${encode(myCountryId)}&select=*&order=created_at.asc"
        } else {
            "/applications?nickname=eq.${encode(player.optString("nickname"))}&select=*&order=created_at.asc"
        }
        val apps = objects(get(appsPath)).map { o ->
            Application(
                id = o.optString("id"),
                countryId = o.optString("country_id"),
                countryName = names[o.optString("country_id")] ?: "",
                nickname = o.optString("nickname"),
                createdAt = System.currentTimeMillis(),
            )
        }

        World(
            countries = countries,
            regions = regions,
            applications = apps,
            leaderboard = countries.sortedByDescending { it.score }.take(50),
            meCountryId = myCountryId,
            meIsLeader = isLeader,
        )
    }

    override suspend fun createCountry(
        token: String, name: String, color: String, emblem: String
    ): Country = withContext(Dispatchers.IO) {
        val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
        if (!player.isNull("country_id")) throw ApiException("Сначала выйди из текущей страны")
        val body = JSONObject()
            .put("name", name)
            .put("color", color)
            .put("emblem", emblem)
            .put("leader", player.optString("nickname"))
            .put("members", 1)
        val rows = try {
            objects(call("POST", "/countries", body))
        } catch (e: ApiException) {
            val msg = e.message ?: ""
            if (msg.contains("Уже существует", true) || msg.contains("duplicate", true)) {
                throw ApiException("Страна с таким названием уже есть")
            }
            throw e
        }
        val row = rows.firstOrNull() ?: throw ApiException("Не удалось создать страну")
        call(
            "PATCH", "/players?token=eq.${encode(token)}",
            JSONObject().put("country_id", row.optString("id"))
        )
        country(row)
    }

    override suspend fun apply(token: String, countryId: String) = withContext(Dispatchers.IO) {
        val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
        if (!player.isNull("country_id")) throw ApiException("Сначала выйди из текущей страны")
        val body = JSONObject()
            .put("country_id", countryId)
            .put("nickname", player.optString("nickname"))
        try {
            call("POST", "/applications", body)
        } catch (e: ApiException) {
            val msg = e.message ?: ""
            if (msg.contains("Уже существует", true) || msg.contains("duplicate", true)) {
                throw ApiException("Заявка уже отправлена")
            }
            throw e
        }
        Unit
    }

    override suspend fun acceptApplication(token: String, id: String) = withContext(Dispatchers.IO) {
        val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
        val app = objects(get("/applications?id=eq.${encode(id)}&select=*&limit=1"))
            .firstOrNull() ?: throw ApiException("Заявка не найдена")
        val cRow = countryById(app.optString("country_id"))
            ?: throw ApiException("Страна не найдена")
        if (cRow.optString("leader") != player.optString("nickname")) {
            throw ApiException("Принимать заявки может только лидер")
        }
        val applicant = playerByNick(app.optString("nickname"))
        if (applicant != null && applicant.isNull("country_id")) {
            call(
                "PATCH", "/players?nickname=eq.${encode(applicant.optString("nickname"))}",
                JSONObject().put("country_id", cRow.optString("id"))
            )
            call(
                "PATCH", "/countries?id=eq.${encode(cRow.optString("id"))}",
                JSONObject().put("members", cRow.optInt("members", 1) + 1)
            )
        }
        call("DELETE", "/applications?id=eq.${encode(id)}")
        Unit
    }

    override suspend fun declineApplication(token: String, id: String) = withContext(Dispatchers.IO) {
        me(token) ?: throw ApiException("Сессия истекла — войди заново")
        call("DELETE", "/applications?id=eq.${encode(id)}")
        Unit
    }

    override suspend fun leaveCountry(token: String) = withContext(Dispatchers.IO) {
        val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
        if (player.isNull("country_id")) throw ApiException("Ты не в стране")
        val countryId = player.optString("country_id")
        val cRow = countryById(countryId) ?: run {
            call("PATCH", "/players?token=eq.${encode(token)}", JSONObject().put("country_id", JSONObject.NULL))
            throw ApiException("Страна не найдена")
        }
        val nickname = player.optString("nickname")
        call(
            "PATCH", "/players?token=eq.${encode(token)}",
            JSONObject().put("country_id", JSONObject.NULL)
        )
        val members = cRow.optInt("members", 1)
        if (members <= 1) {
            call("DELETE", "/countries?id=eq.${encode(countryId)}")
            call(
                "PATCH", "/regions?progress_country_id=eq.${encode(countryId)}",
                JSONObject().put("progress", 0).put("progress_country_id", JSONObject.NULL)
            )
        } else {
            call(
                "PATCH", "/countries?id=eq.${encode(countryId)}",
                JSONObject().put("members", members - 1)
            )
            if (cRow.optString("leader") == nickname) {
                val next = objects(
                    get("/players?country_id=eq.${encode(countryId)}&select=*&limit=1")
                ).firstOrNull()
                if (next != null) {
                    call(
                        "PATCH", "/countries?id=eq.${encode(countryId)}",
                        JSONObject().put("leader", next.optString("nickname"))
                    )
                }
            }
        }
        Unit
    }

    override suspend fun capture(token: String, regionId: String, amount: Long): JSONObject =
        withContext(Dispatchers.IO) {
            val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
            if (player.isNull("country_id")) throw ApiException("Сначала вступи в страну")
            val countryId = player.optString("country_id")
            val cRow = countryById(countryId) ?: throw ApiException("Страна не найдена")
            val rRow = objects(get("/regions?id=eq.${encode(regionId)}&select=$regionCols&limit=1"))
                .firstOrNull() ?: throw ApiException("Регион не найден")

            if (!rRow.isNull("ownerId") && rRow.optString("ownerId") == countryId) {
                return@withContext JSONObject()
                    .put("ok", true).put("captured", false)
                    .put("message", "Это уже твоя земля")
            }
            val progressCountry = if (rRow.isNull("progressCountryId")) null else rRow.optString("progressCountryId")
            var progress = if (progressCountry == countryId) rRow.optInt("progress") else 0
            progress = (progress + 12).coerceAtMost(100)

            val captured = progress >= 100
            val regionBody = JSONObject().put("progress", if (captured) 0 else progress)
            if (captured) {
                regionBody.put("owner_id", countryId)
                    .put("owner_name", cRow.optString("name"))
                    .put("owner_color", cRow.optString("color"))
                    .put("progress_country_id", JSONObject.NULL)
            } else {
                regionBody.put("progress_country_id", countryId)
            }
            call("PATCH", "/regions?id=eq.${encode(regionId)}", regionBody)

            if (captured) {
                call(
                    "PATCH", "/countries?id=eq.${encode(countryId)}",
                    JSONObject()
                        .put("regions", cRow.optInt("regions") + 1)
                        .put("score", cRow.optLong("score") + amount)
                )
                call(
                    "PATCH", "/players?token=eq.${encode(token)}",
                    JSONObject().put("captured", player.optInt("captured") + 1)
                )
            }
            JSONObject()
                .put("ok", true)
                .put("captured", captured)
                .put("progress", if (captured) 0 else progress)
                .put(
                    "message",
                    if (captured) "${rRow.optString("name")} теперь часть ${cRow.optString("name")}!"
                    else "Захват: $progress%"
                )
        }

    override suspend fun pushScore(token: String, score: Long, captured: Int) =
        withContext(Dispatchers.IO) {
            call(
                "PATCH", "/players?token=eq.${encode(token)}",
                JSONObject().put("score", score).put("captured", captured)
            )
            Unit
        }

    override suspend fun leaderboard(token: String?): List<Country> = withContext(Dispatchers.IO) {
        objects(get("/countries?select=$countryCols&order=score.desc&limit=50")).map(::country)
    }

    override suspend fun sendChat(token: String, countryId: String, text: String) =
        withContext(Dispatchers.IO) {
            val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
            val body = JSONObject()
                .put("country_id", countryId)
                .put("nickname", player.optString("nickname"))
                .put("text", text.take(300))
                .put("created_at", System.currentTimeMillis())
            call("POST", "/chat", body)
            Unit
        }

    override suspend fun chat(token: String, countryId: String, limit: Int): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            objects(
                get("/chat?country_id=eq.${encode(countryId)}&select=*&order=created_at.asc&limit=$limit")
            ).map { o ->
                ChatMessage(
                    id = o.optString("id"),
                    countryId = o.optString("country_id"),
                    nickname = o.optString("nickname"),
                    text = o.optString("text"),
                    createdAt = o.optLong("created_at", System.currentTimeMillis()),
                )
            }
        }

    override suspend fun developRegion(token: String, regionId: String, level: Int, invested: Long) =
        withContext(Dispatchers.IO) {
            val player = me(token) ?: throw ApiException("Сессия истекла — войди заново")
            val countryId = player.optString("country_id")
            val rRow = objects(get("/regions?id=eq.${encode(regionId)}&select=$regionCols&limit=1"))
                .firstOrNull() ?: throw ApiException("Регион не найден")
            if (rRow.isNull("ownerId") || rRow.optString("ownerId") != countryId) {
                throw ApiException("Развивать можно только свою землю")
            }
            call(
                "PATCH", "/regions?id=eq.${encode(regionId)}",
                JSONObject().put("level", level).put("invested", invested)
            )
            Unit
        }
}
