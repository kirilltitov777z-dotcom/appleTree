package com.narezany.appletree.data

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class Account(
    val name: String,
    val salt: String,
    val hash: String,
    val created: Long
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("salt", salt)
        .put("hash", hash)
        .put("created", created)

    companion object {
        fun fromJson(o: JSONObject): Account = Account(
            name = o.optString("name", "Садовник"),
            salt = o.optString("salt", ""),
            hash = o.optString("hash", ""),
            created = o.optLong("created", System.currentTimeMillis())
        )
    }
}

/**
 * Локальный вход по паролю. Пароль не хранится — только PBKDF2-хеш с солью.
 */
object Auth {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun create(name: String, password: String): Account {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Account(
            name = name.trim(),
            salt = b64(salt),
            hash = hash(password, salt),
            created = System.currentTimeMillis()
        )
    }

    fun check(account: Account, password: String): Boolean {
        val salt = runCatching { Base64.decode(account.salt, Base64.NO_WRAP) }.getOrNull() ?: return false
        val candidate = hash(password, salt)
        return candidate.isNotEmpty() && candidate == account.hash
    }

    private fun hash(password: String, salt: ByteArray): String {
        return runCatching {
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            b64(factory.generateSecret(spec).encoded)
        }.getOrDefault("")
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
