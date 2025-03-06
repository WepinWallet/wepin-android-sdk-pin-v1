package com.wepin.android.pinlib.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wepin.android.pinlib.types.StorageDataType
import com.wepin.android.pinlib.utils.Log
import com.wepin.android.pinlib.utils.convertJsonToLocalStorageData
import com.wepin.android.pinlib.utils.convertLocalStorageDataToJson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.text.Charsets.UTF_8

internal object WepinStorageManager {
    private val TAG = this.javaClass.name
    private var _appId: String = ""
    private const val PREV_PREFERENCE_NAME = "wepin_encrypted_preferences"
    private lateinit var _prevStorage: EncryptedSharedPreferences
    private lateinit var _storage: StorageManager

    fun init(context: Context, appId: String) {
        Log.i(TAG, "init")
        this._appId = appId

        try {
            initializePrevStorage(context)
        } catch(error: Exception) {
            context.getSharedPreferences(PREV_PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            initializePrevStorage(context)
        }
        _storage = StorageManager(context)
        migrationOldStorage()
    }

    private fun initializePrevStorage(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        _prevStorage = EncryptedSharedPreferences.create(
            context,
            PREV_PREFERENCE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    private fun migrationOldStorage() {
        try {
            val migrationState = getStorage<Boolean>("migration")
            if (migrationState == true) return

            val oldStorage = _prevStorageReadAll()
            oldStorage?.forEach { (key, value) ->
                setStorage(key, value)
            }
        } catch(e: Exception) {
//            println("Migration failed with an unexpected error - $e")
        } finally {
            setStorage("migration", true)
            _prevDeleteAll()
        }
    }

    fun <T> _encodeValue(value: T): String {
        return when (value) {
            is String,
            is Int,
            is Double,
            is Boolean -> {
                value.toString()
            }

            is StorageDataType -> {
                convertLocalStorageDataToJson(value)
            }

            else -> throw IllegalArgumentException("Unsupported data type")
        }
    }

    fun <T> _parseValue(value: String): T? {
        val primitiveValue: Any? = when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            else -> null
        }

        @Suppress("UNCHECKED_CAST")
        return try {
            if (primitiveValue != null) {
                primitiveValue as? T
            } else {
                convertJsonToLocalStorageData(value) as? T ?: value as? T
            }
        } catch (e: Exception) {
            //String
            value as? T
        }
    }

    // Set EncryptedSharedPreferences
    fun <T> setStorage(key: String, data: T) {
        try {
            val stringValue = _encodeValue(data)
            _storage.write(_appId, key, stringValue)
        } catch(e: Exception) {
            if (!e.message.toString().contains("already exists")){
                throw e
            }
        }
    }

    // Get EncryptedSharedPreferences
    fun <T> getStorage(key: String): T? {
        return try {
            _storage.read(_appId, key)?.let { _parseValue(it) }
        } catch (e: Exception) {
            null
        }
    }

    // Delete EncryptedSharedPreferences
//    fun deleteStorage(key: String) {
//        Log.i(TAG, "deleteStorage")
//        _storage.delete(_appId, key)
//    }

    // Delete All EncryptedSharedPreferences for a specific appId
//    fun deleteAllStorageWithAppId() {
//        Log.i(TAG, "deleteAllStorageWithAppId")
//        val sharedPreferenceIds = sharedPreferences.all
//        sharedPreferenceIds.forEach {
//            if (it.key.startsWith(_appId)) {
//                sharedPreferences.edit().remove(it.key).apply()
//            }
//        }
//    }

    // Delete All EncryptedSharedPreferences regardless of appId
    fun deleteAllStorage() {
        Log.i(TAG, "deleteAllStorage")
        _storage.deleteAll()

        setStorage("migration", true);
    }

    // Check if appId related data exists
//    private fun isAppIdDataExists(): Boolean {
//        Log.i(TAG, "isAppIdDataExists")
//        val sharedPreferenceIds = sharedPreferences.all
//        sharedPreferenceIds.forEach {
//            if (it.key.startsWith(_appId)) {
//                return true
//            }
//        }
//        return false
//    }

    // Delete all data if appId data does not exist
//    fun deleteAllIfAppIdDataNotExists() {
//        Log.i(TAG, "deleteAllIfAppIdDataNotExists")
//        if (!isAppIdDataExists()) {
//            deleteAllStorage()
//        }
//    }

    // Get all EncryptedSharedPreferences
    fun getAllStorage(): Map<String, Any?>? {
        try {
            val allData = _storage.readAll(_appId)

            val filteredData = mutableMapOf<String, Any?>()
            for (key in allData.keys) {
                val storageKey = key.replaceFirst("${_appId}_", "")
                try {
                    val jsonValue = _parseValue<StorageDataType>(allData[key] ?: "")
                    filteredData[storageKey] = jsonValue
                } catch (e: Exception) {
                    filteredData[storageKey] = allData[key]
                }
            }
            return if (filteredData.isEmpty()) null else filteredData

        } catch (e: Exception) {
            return null
        }
    }

    // Set all EncryptedSharedPreferences
    fun setAllStorage(data: Map<String, Any>) {
        Log.i(TAG, "setAllStorage")
        data.forEach { (key, value) ->
            setStorage(key, value)
        }
    }

//    private fun getEncryptedDataPair(data: String): Pair<ByteArray, ByteArray> {
//        Log.i(TAG, "getEncryptedDataPair")
//        val cipher = Cipher.getInstance(TRANSFORMATION)
//        cipher.init(Cipher.ENCRYPT_MODE, getKey())
//
//        val iv: ByteArray = cipher.iv
//        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
//        return Pair(iv, encryptedData)
//    }

//    private fun getKey(): SecretKey {
//        Log.i(TAG, "getKey")
//        val keyStore = KeyStore.getInstance("AndroidKeyStore")
//        keyStore.load(null)
//        val secreteKeyEntry: KeyStore.SecretKeyEntry =
//            keyStore.getEntry(PREV_PREFERENCE_NAME + this._appId, null) as KeyStore.SecretKeyEntry
//        return secreteKeyEntry.secretKey
//    }

    private fun _prevStorageReadAll(): Map<String, Any?>? {
        return try {
            _prevStorage.all
                .filterKeys { it.startsWith(_appId) }
                .mapKeys { it.key.removePrefix("${_appId}_") }
        } catch (e: Exception) {
            _prevDeleteAll()
            null
        }
    }

    private fun _prevDeleteAll() {
        try {
            _prevStorage.edit().clear().apply()
        } catch(e: Exception) {
            return
        }
    }

}
