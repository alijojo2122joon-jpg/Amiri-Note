package com.amiri.note.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted settings store (keys and values encrypted at rest, Keystore-backed).
 * Holds all security-sensitive settings: the PIN hash, the secret-phrase hash,
 * the app-lock flag and the default currency.
 */
class SecurePrefs private constructor(private val prefs: SharedPreferences) {

    companion object {
        private const val FILE = "amiri_secure_prefs"

        private const val KEY_PIN = "pin_hash"
        private const val KEY_PHRASE = "phrase_hash"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_VAULT_INIT = "vault_initialized"
        private const val KEY_HIDDEN_APPS = "vault_app_list"

        // Default secret phrase, as specified by the owner. Stored hashed on first run.
        const val DEFAULT_PHRASE = "where you hided"

        @Volatile private var INSTANCE: SecurePrefs? = null

        fun get(context: Context): SecurePrefs =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): SecurePrefs {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val sp = EncryptedSharedPreferences.create(
                context.applicationContext,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val self = SecurePrefs(sp)
            // Seed the default secret phrase hash once, if nothing is set yet.
            if (!self.hasPhrase()) {
                self.setPhrase(DEFAULT_PHRASE)
            }
            return self
        }
    }

    // ---- PIN ----
    fun hasPin(): Boolean = prefs.contains(KEY_PIN)
    fun setPin(pin: String) = prefs.edit().putString(KEY_PIN, SecretHasher.hash(pin)).apply()
    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN, null) ?: return false
        return SecretHasher.verify(pin, stored)
    }

    // ---- Secret phrase ----
    // Stored as a fast SHA-256 hash (the phrase is a trigger, not a password;
    // the PIN/biometric is the real guard). This keeps per-keystroke detection
    // in the note editor instant.
    fun hasPhrase(): Boolean = prefs.contains(KEY_PHRASE)
    fun setPhrase(phrase: String) =
        prefs.edit().putString(KEY_PHRASE, SecretHasher.fastHash(phrase.trim().lowercase())).apply()
    fun matchesPhrase(candidate: String): Boolean {
        val stored = prefs.getString(KEY_PHRASE, null) ?: return false
        return SecretHasher.fastVerify(candidate.trim().lowercase(), stored)
    }

    // ---- Flags ----
    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(v) = prefs.edit().putBoolean(KEY_APP_LOCK, v).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(v) = prefs.edit().putBoolean(KEY_BIOMETRIC, v).apply()

    var defaultCurrency: String
        get() = prefs.getString(KEY_DEFAULT_CURRENCY, "AFN") ?: "AFN"
        set(v) = prefs.edit().putString(KEY_DEFAULT_CURRENCY, v).apply()

    var vaultInitialized: Boolean
        get() = prefs.getBoolean(KEY_VAULT_INIT, false)
        set(v) = prefs.edit().putBoolean(KEY_VAULT_INIT, v).apply()

    /** Package names the user added to the private "Apps" list in the vault. */
    var hiddenApps: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN_APPS, emptySet())?.toSet() ?: emptySet()
        set(v) = prefs.edit().putStringSet(KEY_HIDDEN_APPS, v.toSet()).apply()
}
