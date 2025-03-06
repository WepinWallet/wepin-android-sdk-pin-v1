package com.wepin.android.pinlib.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator

object KeyStoreHelper {
    private const val KEY_ALIAS = "wepin_keystore_key"

    fun initializeKeystore(context: Context): Boolean {
        return if (isKeystoreChanged()) {
            resetKeystore(context)
            true
        } else {
            false
        }
    }

    private fun isKeystoreChanged(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null) == null
        } catch (e: Exception) {
            true
        }
    }

    private fun resetKeystore(context: Context) {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
