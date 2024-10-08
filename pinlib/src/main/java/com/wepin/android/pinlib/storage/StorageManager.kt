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

internal object StorageManager {
    private val TAG = this.javaClass.name
    private var _appId: String = ""
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val PREFERENCE_NAME = "wepin_encrypted_preferences"
    private lateinit var encDataPair: Pair<ByteArray, ByteArray>
    private lateinit var sharedPreferences: EncryptedSharedPreferences

    fun init(context: Context, appId: String) {
        Log.i(TAG, "init")
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            this._appId = appId
            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFERENCE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences

            val key = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keySpec = KeyGenParameterSpec.Builder(
                PREFERENCE_NAME + appId,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(false)
                .build()
            key.init(keySpec)
            key.generateKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Set EncryptedSharedPreferences
    fun setStorage(key: String, data: Any) {
        Log.i(TAG, "setStorage")
        if (this::sharedPreferences.isInitialized) {
            val appSpecificKey = "${_appId}_$key"
            when (data) {
                is StorageDataType -> {
                    val stringData = convertLocalStorageDataToJson(data)
                    sharedPreferences.edit().putString(appSpecificKey, stringData)?.apply()
                    encDataPair = getEncryptedDataPair(stringData)
                    sharedPreferences.edit().putString(appSpecificKey, stringData)?.apply()
                    encDataPair = getEncryptedDataPair(stringData)
                    encDataPair.second.toString(UTF_8)
                }
                is String -> {
                    sharedPreferences.edit().putString(appSpecificKey, data)?.apply()
                    encDataPair = getEncryptedDataPair(data)
                    sharedPreferences.edit().putString(appSpecificKey, data)?.apply()
                    encDataPair = getEncryptedDataPair(data)
                    encDataPair.second.toString(UTF_8)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported data type")
                }
            }
        }
    }

    // Get EncryptedSharedPreferences
    fun getStorage(key: String): Any? {
        Log.i(TAG, "getStorage")
        var stringData: String? = null
        try {
            if (this::sharedPreferences.isInitialized) {
                val appSpecificKey = "${_appId}_$key"
                val stringRes = sharedPreferences.getString(appSpecificKey, null) ?: return null
                val encryptedPairData = getEncryptedDataPair(stringRes)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val keySpec = IvParameterSpec(encryptedPairData.first)
                cipher.init(Cipher.DECRYPT_MODE, getKey(), keySpec)
                stringData = cipher.doFinal(encryptedPairData.second).toString(UTF_8)
                return convertJsonToLocalStorageData(stringData)
            }
        } catch (e: Exception) {
//            e.printStackTrace()
            return stringData
        }
        return null
    }

    // Delete EncryptedSharedPreferences
    fun deleteStorage(key: String) {
        Log.i(TAG, "deleteStorage")
        val appSpecificKey = "${_appId}_$key"
        sharedPreferences.edit().remove(appSpecificKey).apply()
    }

    // Delete All EncryptedSharedPreferences for a specific appId
    fun deleteAllStorageWithAppId() {
        Log.i(TAG, "deleteAllStorageWithAppId")
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            if (it.key.startsWith(_appId)) {
                sharedPreferences.edit().remove(it.key).apply()
            }
        }
    }

    // Delete All EncryptedSharedPreferences regardless of appId
    fun deleteAllStorage() {
        Log.i(TAG, "deleteAllStorage")
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            sharedPreferences.edit().remove(it.key).apply()
        }
    }

    // Check if appId related data exists
    private fun isAppIdDataExists(): Boolean {
        Log.i(TAG, "isAppIdDataExists")
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            if (it.key.startsWith(_appId)) {
                return true
            }
        }
        return false
    }

    // Delete all data if appId data does not exist
    fun deleteAllIfAppIdDataNotExists() {
        Log.i(TAG, "deleteAllIfAppIdDataNotExists")
        if (!isAppIdDataExists()) {
            deleteAllStorage()
        }
    }

    // Get all EncryptedSharedPreferences
    fun getAllStorage(): Map<String, Any?> {
        Log.i(TAG, "getAllStorage")
        val allData = mutableMapOf<String, Any?>()
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            if (it.key.startsWith(_appId)) {
                val key = it.key.removePrefix("${_appId}_")
                allData[key] = getStorage(key)
            }
        }
        return allData
    }

    // Set all EncryptedSharedPreferences
    fun setAllStorage(data: Map<String, Any>) {
        Log.i(TAG, "setAllStorage")
        data.forEach { (key, value) ->
            setStorage(key, value)
        }
    }

    private fun getEncryptedDataPair(data: String): Pair<ByteArray, ByteArray> {
        Log.i(TAG, "getEncryptedDataPair")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val iv: ByteArray = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Pair(iv, encryptedData)
    }

    private fun getKey(): SecretKey {
        Log.i(TAG, "getKey")
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secreteKeyEntry: KeyStore.SecretKeyEntry =
            keyStore.getEntry(PREFERENCE_NAME + this._appId, null) as KeyStore.SecretKeyEntry
        return secreteKeyEntry.secretKey
    }

}
