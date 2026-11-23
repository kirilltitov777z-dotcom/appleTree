package com.narezany.appletree.game

import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

/** Страна или садовое сообщество. */
data class Country(
    val id: String,
    val name: String,
    val color: Int,
    val leader: String,
    val members: Int,
    val motto: String,
    val bot: Boolean = true
)

/** Заявка на вступление в страну (видит лидер). */
data class CountryApp(val name: String, val at: Long)

data class Sky(
    val id: String,
    val title: String,
    val price: Long,
    val top: Int,
    val bottom: Int,
    val accent: Int,
    val note: String
)

data class TreeLook(
    val id: String,
    val title: String,
    val price: Long,
    val light: Int,
    val mid: Int,
    val deep: Int,
    val apple: Int,
    val note: String
)

object Skins {
    val skies = listOf(
        Sky("day", "Ясный день", 0, 0xFFA9D8F2.toInt(), 0xFFEAF6E4.toInt(), 0xFFFFF1C6.toInt(), "Голубое небо и мягкое солнце"),
        Sky("morning", "Утро", 900, 0xFFFBE4CB.toInt(), 0xFFDDEFF5.toInt(), 0xFFFFE3B0.toInt(), "Тёплый рассвет над садом"),
        Sky("sunset", "Закат", 2200, 0xFFF6CAB6.toInt(), 0xFFDBE7F3.toInt(), 0xFFFFDCB0.toInt(), "Розовое небо и тишина"),
        Sky("autumn", "Бабье лето", 4200, 0xFFEFE1C6.toInt(), 0xFFE9F1E0.toInt(), 0xFFF6D9A0.toInt(), "Золотой воздух сентября"),
        Sky("night", "Тихая ночь", 7500, 0xFF485A72.toInt(), 0xFF8FA0B5.toInt(), 0xFFF2E7C8.toInt(), "Луна, звёзды и покой")
    )

    val trees = listOf(
        TreeLook("summer", "Летняя яблоня", 0, 0xFF9CC985.toInt(), 0xFF7FAE6B.toInt(), 0xFF5F8B51.toInt(), 0xFFD2665A.toInt(), "Зелёная листва и спелые плоды"),
        TreeLook("bloom", "Цветение", 1800, 0xFFF3CBD0.toInt(), 0xFFE6AFB7.toInt(), 0xFFC98F9A.toInt(), 0xFFE08C7E.toInt(), "Нежные цветы вместо листвы"),
        TreeLook("autumn", "Осень", 3400, 0xFFEBCB8B.toInt(), 0xFFD9A85F.toInt(), 0xFFB07F3F.toInt(), 0xFFC75B50.toInt(), "Медовые листья и урожай")
    )

    fun sky(id: String): Sky = skies.firstOrNull { it.id == id } ?: skies.first()
    fun tree(id: String): TreeLook = trees.firstOrNull { it.id == id } ?: trees.first()
}

data class Upgrade(
    val id: String,
    val title: String,
    val about: String,
    val base: Long,
    val growth: Double,
    val max: Int,
    val icon: String,
    val effect: (Int) -> String
)

object Upgrades {
    val all = listOf(
        Upgrade("power", "Крепкая корзина", "Больше яблок с каждого нажатия", 25, 1.55, 60, "🧺") { lvl ->
            "+$lvl к сбору"
        },
        Upgrade("agility", "Ловкие руки", "Короче пауза между нажатиями", 70, 1.6, 13, "✋") { lvl ->
            "пауза ${(340 - lvl * 22).coerceAtLeast(60)} мс"
        },
        Upgrade("ring", "Созревание", "Кольцо урожая заполняется быстрее", 90, 1.5, 40, "🌱") { lvl ->
            "+${lvl * 6}% скорости"
        },
        Upgrade("harvest", "Богатый урожай", "Больше яблок за полное кольцо", 130, 1.55, 50, "🍎") { lvl ->
            "+${lvl * 9} за круг"
        },
        Upgrade("gardener", "Садовник", "Яблоки капают сами собой", 160, 1.5, 60, "🧑‍🌾") { lvl ->
            "${oneDecimal(lvl * 0.4)} в секунду"
        }
    )

    fun byId(id: String): Upgrade? = all.firstOrNull { it.id == id }

    fun cost(u: Upgrade, level: Int): Long =
        (u.base * u.growth.pow(level.toDouble())).roundToLong().coerceAtLeast(u.base)

    private fun oneDecimal(v: Double): String = String.format(Locale.US, "%.1f", v)
}

/** Сетка лугов 7x7: true — суша, false — вода. */
object MapData {
    const val SIZE = 7

    private val land = booleanArrayOf(
        false, false, true, true, true, false, false,
        false, true, true, true, true, true, false,
        true, true, true, true, true, true, true,
        true, true, true, true, true, true, true,
        false, true, true, true, true, true, false,
        false, false, true, true, true, false, false,
        false, false, false, true, false, false, false
    )

    fun isLand(i: Int): Boolean = i in land.indices && land[i]

    fun index(x: Int, y: Int): Int = y * SIZE + x

    fun neighbors(i: Int): List<Int> {
        val x = i % SIZE
        val y = i / SIZE
        val out = mutableListOf<Int>()
        if (x > 0) out.add(index(x - 1, y))
        if (x < SIZE - 1) out.add(index(x + 1, y))
        if (y > 0) out.add(index(x, y - 1))
        if (y < SIZE - 1) out.add(index(x, y + 1))
        return out.filter { isLand(it) }
    }

    fun landTiles(): List<Int> = land.indices.filter { land[it] }

    fun capturable(owned: Set<Int>): Set<Int> {
        if (owned.isEmpty()) return landTiles().toSet()
        return owned.flatMap { neighbors(it) }.toSet() - owned
    }

    fun startTile(occupied: Set<Int>): Int? {
        val order = listOf(24, 17, 31, 23, 25, 16, 18, 30, 32, 10, 11, 12, 38, 39, 40, 3, 4, 5, 21, 27)
        return order.firstOrNull { isLand(it) && it !in occupied } ?: landTiles().firstOrNull { it !in occupied }
    }
}

object Bots {
    val countries = listOf(
        Country("b1", "Яблоневая долина", 0xFF7FAE6B.toInt(), "Мира", 14, "Сад, который поёт"),
        Country("b2", "Тихие холмы", 0xFF8FB9C9.toInt(), "Ян", 9, "Слушаем ветер"),
        Country("b3", "Сад ветров", 0xFFD9A85F.toInt(), "Лада", 11, "Лёгкие, как листья"),
        Country("b4", "Медовые луга", 0xFFC99A55.toInt(), "Тимур", 7, "Тёплый мёд и яблоки"),
        Country("b5", "Утренний берег", 0xFFB79FC9.toInt(), "Аня", 5, "Встаём вместе с солнцем"),
        Country("b6", "Речная заводь", 0xFFA8C3A0.toInt(), "Илья", 6, "Тихая вода и яблони")
    )

    val memberNames = listOf(
        "Вера", "Миша", "Соня", "Артём", "Лиза", "Кирилл", "Даша", "Рома",
        "Ника", "Егор", "Полина", "Савва", "Юля", "Марк", "Ася", "Гриша"
    )

    fun byId(id: String): Country? = countries.firstOrNull { it.id == id }
}
