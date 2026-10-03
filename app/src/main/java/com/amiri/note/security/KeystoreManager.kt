package com.amiri.note.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Owns the master AES key used to encrypt vault blobs. The key is generated in
 * the Android Keystore (hardware-backed where available) and never leaves it.
 * Only this app can use it, and only on this device.
 */
object KeystoreManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "amiri_vault_master_key_v2"

    fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val gen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // The Keystore generates the IV itself (randomized encryption is on).
            // This key only wraps small per-file data keys; bulk data is encrypted
            // in chunks by CryptoManager. No setUserAuthenticationRequired: the app
            // gates access with its own PIN/biometric flow.
            .build()
        gen.init(spec)
        return gen.generateKey()
    }
}
