package com.narezany.appletree.util

import kotlin.math.abs

fun fmt(n: Long): String {
    val a = abs(n)
    return when {
        a < 10_000 -> n.toString()
        a < 1_000_000 -> String.format("%,d", n).replace(',', ' ')
        a < 1_000_000_000 -> String.format("%.1f", n / 1_000_000.0).replace(".0", "") + "М"
        else -> String.format("%.1f", n / 1_000_000_000.0).replace(".0", "") + "Б"
    }
}

fun fmtPercent(p: Float): String = "${(p * 100).toInt()}%"

fun stageName(level: Int): String = when {
    level < 3 -> "Росток"
    level < 6 -> "Молодая яблоня"
    level < 10 -> "Крепкая яблоня"
    level < 15 -> "Старый сад"
    level < 22 -> "Большой сад"
    else -> "Легендарная роща"
}

fun timeAgo(ms: Long): String {
    val diff = (System.currentTimeMillis() - ms) / 1000
    return when {
        diff < 60 -> "только что"
        diff < 3600 -> "${diff / 60} мин назад"
        diff < 86400 -> "${diff / 3600} ч назад"
        else -> "${diff / 86400} дн назад"
    }
}

fun boostLeft(until: Long): String {
    val ms = until - System.currentTimeMillis()
    if (ms <= 0) return ""
    val m = ms / 60000
    return if (m >= 60) "${m / 60} ч ${m % 60} мин" else "$m мин"
}

fun parseColor(hex: String?, fallback: Long = 0xFF7FB069): Long {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleaned = hex.removePrefix("#")
        when (cleaned.length) {
            6 -> 0xFF000000 or cleaned.toLong(16)
            8 -> cleaned.toLong(16)
            else -> fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
