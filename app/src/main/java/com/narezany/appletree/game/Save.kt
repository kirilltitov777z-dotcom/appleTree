package com.narezany.appletree.game

import org.json.JSONArray
import org.json.JSONObject

/**
 * Всё состояние сада. Хранится локально в JSON и переживает перезапуск.
 */
data class Save(
    val tutorial: Boolean = false,
    val apples: Long = 0,
    val lifetime: Long = 0,
    val levels: Map<String, Int> = emptyMap(),
    val ring: Float = 0f,
    val gardenLevel: Int = 1,
    val xp: Long = 0,
    val sky: String = "day",
    val tree: String = "summer",
    val ownsSky: List<String> = listOf("day"),
    val ownsTree: List<String> = listOf("summer"),
    val countryName: String? = null,
    val countryColor: Int = 0,
    val countryMotto: String = "",
    val countryLeader: Boolean = true,
    val pending: String? = null,
    val pendingAt: Long = 0L,
    val map: Map<Int, String> = emptyMap(),
    val apps: List<CountryApp> = emptyList(),
    val lastSeen: Long = System.currentTimeMillis(),
    val haptics: Boolean = true,
    val calm: Boolean = false,
    val server: String = ""
) {
    fun level(id: String): Int = levels[id] ?: 0

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("v", 1)
        o.put("tutorial", tutorial)
        o.put("apples", apples)
        o.put("lifetime", lifetime)
        o.put("ring", ring.toDouble())
        o.put("gardenLevel", gardenLevel)
        o.put("xp", xp)
        o.put("sky", sky)
        o.put("tree", tree)
        o.put("countryName", countryName ?: JSONObject.NULL)
        o.put("countryColor", countryColor)
        o.put("countryMotto", countryMotto)
        o.put("countryLeader", countryLeader)
        o.put("pending", pending ?: JSONObject.NULL)
        o.put("pendingAt", pendingAt)
        o.put("lastSeen", lastSeen)
        o.put("haptics", haptics)
        o.put("calm", calm)
        o.put("server", server)

        val lv = JSONObject()
        levels.forEach { (k, v) -> lv.put(k, v) }
        o.put("levels", lv)

        val sk = JSONArray()
        ownsSky.forEach { sk.put(it) }
        o.put("ownsSky", sk)

        val tr = JSONArray()
        ownsTree.forEach { tr.put(it) }
        o.put("ownsTree", tr)

        val mp = JSONObject()
        map.forEach { (k, v) -> mp.put(k.toString(), v) }
        o.put("map", mp)

        val ap = JSONArray()
        apps.forEach {
            ap.put(JSONObject().put("n", it.name).put("t", it.at))
        }
        o.put("apps", ap)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Save {
            val levels = mutableMapOf<String, Int>()
            o.optJSONObject("levels")?.let { lv ->
                lv.keys().forEach { k -> levels[k] = lv.optInt(k) }
            }

            val skies = mutableListOf<String>()
            o.optJSONArray("ownsSky")?.let { arr ->
                for (i in 0 until arr.length()) skies.add(arr.optString(i))
            }
            if (skies.isEmpty()) skies.add("day")

            val trees = mutableListOf<String>()
            o.optJSONArray("ownsTree")?.let { arr ->
                for (i in 0 until arr.length()) trees.add(arr.optString(i))
            }
            if (trees.isEmpty()) trees.add("summer")

            val map = mutableMapOf<Int, String>()
            o.optJSONObject("map")?.let { mp ->
                mp.keys().forEach { k ->
                    val idx = k.toIntOrNull()
                    val owner = mp.optString(k)
                    if (idx != null && owner.isNotEmpty()) map[idx] = owner
                }
            }

            val apps = mutableListOf<CountryApp>()
            o.optJSONArray("apps")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val it = arr.optJSONObject(i) ?: continue
                    apps.add(CountryApp(it.optString("n"), it.optLong("t")))
                }
            }

            fun text(key: String): String? =
                o.optString(key).takeIf { it.isNotEmpty() && it != "null" }

            return Save(
                tutorial = o.optBoolean("tutorial", false),
                apples = o.optLong("apples", 0),
                lifetime = o.optLong("lifetime", 0),
                levels = levels,
                ring = o.optDouble("ring", 0.0).toFloat().coerceIn(0f, 1f),
                gardenLevel = o.optInt("gardenLevel", 1).coerceAtLeast(1),
                xp = o.optLong("xp", 0),
                sky = text("sky") ?: "day",
                tree = text("tree") ?: "summer",
                ownsSky = skies,
                ownsTree = trees,
                countryName = text("countryName"),
                countryColor = o.optInt("countryColor", 0),
                countryMotto = o.optString("countryMotto"),
                countryLeader = o.optBoolean("countryLeader", true),
                pending = text("pending"),
                pendingAt = o.optLong("pendingAt", 0),
                map = map,
                apps = apps,
                lastSeen = o.optLong("lastSeen", System.currentTimeMillis()),
                haptics = o.optBoolean("haptics", true),
                calm = o.optBoolean("calm", false),
                server = o.optString("server")
            )
        }
    }
}
