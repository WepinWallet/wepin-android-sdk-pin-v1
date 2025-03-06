package com.wepin.android.pinlib.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal class StorageManager(context: Context) {
    companion object {
        private const val PREFERENCE_NAME = "wepin_encrypted_preferences_v1"
    }

    private lateinit var sharedPreferences: EncryptedSharedPreferences

    init {
        if (KeyStoreHelper.initializeKeystore(context)) {
            // KeyStore 변경 시, 기존 데이터 삭제
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

        try {
            initializeStorage(context)
        } catch(error: Exception) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            initializeStorage(context)
        }

        runCatching {
            sharedPreferences.all
        }.recoverCatching { throwable ->
            if (isCorruptError(throwable)) {
                deleteAll()
            }
        }
    }

    private fun initializeStorage(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFERENCE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    private fun isCorruptError(throwable: Throwable): Boolean {
        val message = throwable.message ?: return false
        return throwable::class.java.simpleName == "InvalidProtocolBufferException" ||
                message.contains("InvalidProtocolBufferException", ignoreCase = true) ||
                message.contains("SharedPreferences corruption detected", ignoreCase = true) ||
                message.contains("corrupt", ignoreCase = true)
    }

    private fun formatKey(appId: String, key: String?): String {
        return "${appId}_${key ?: ""}"
    }

    fun write(appId: String, key: String?, data: Any) {
        val formattedKey = formatKey(appId, key)
        sharedPreferences.edit().putString(formattedKey, data.toString()).apply()
    }

    fun read(appId: String, key: String?): String? {
        return runCatching {
            sharedPreferences.getString(formatKey(appId, key), null)
        }.recoverCatching { throwable ->
            if (isCorruptError(throwable)) {
                deleteAll()
            }
            null
        }.getOrNull()
    }

    fun delete(appId: String, key: String?) {
        sharedPreferences.edit().remove(formatKey(appId, key)).apply()
    }

    fun deleteAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun readAll(appId: String): Map<String, String?> {
        return runCatching {
            val all = mutableMapOf<String, String?>()

            for (entry in sharedPreferences.all.entries) {
                val keyWithPrefix = entry.key
                if (keyWithPrefix.startsWith(appId)) {
                    val key = keyWithPrefix.replaceFirst("${appId}_", "")
                    all[key] = entry.value?.toString()
                }
            }
            all
        }.recoverCatching { throwable ->
            if (isCorruptError(throwable)) {
                deleteAll()
            }
            emptyMap()
        }.getOrElse { emptyMap() }
    }

    fun writeAll(appId: String, data: Map<String, Any>) {
        val editor = sharedPreferences.edit()
        data.forEach { (key, value) ->
            editor.putString(formatKey(appId, key), value.toString())
        }
        editor.apply()
    }
}