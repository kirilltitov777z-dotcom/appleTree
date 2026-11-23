package com.narezany.appletree.data

import android.content.Context
import com.narezany.appletree.game.Save
import org.json.JSONObject

/**
 * Простое локальное хранилище: аккаунт + сохранение сада + токен онлайна.
 */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("appletree_store", Context.MODE_PRIVATE)

    fun loadSave(): Save {
        val raw = prefs.getString(KEY_SAVE, null) ?: return Save()
        return runCatching { Save.fromJson(JSONObject(raw)) }.getOrElse { Save() }
    }

    fun writeSave(save: Save) {
        runCatching { prefs.edit().putString(KEY_SAVE, save.toJson().toString()).apply() }
    }

    fun loadAccount(): Account? {
        val raw = prefs.getString(KEY_ACCOUNT, null) ?: return null
        return runCatching { Account.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun writeAccount(account: Account) {
        prefs.edit().putString(KEY_ACCOUNT, account.toJson().toString()).apply()
    }

    fun deleteAccount() {
        prefs.edit().remove(KEY_ACCOUNT).apply()
    }

    fun wipe() {
        prefs.edit().clear().apply()
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    companion object {
        private const val KEY_SAVE = "save_v1"
        private const val KEY_ACCOUNT = "account_v1"
        private const val KEY_TOKEN = "token"
    }
}
