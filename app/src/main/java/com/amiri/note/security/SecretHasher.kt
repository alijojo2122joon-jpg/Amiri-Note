package com.amiri.note.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-based hashing for the Vault PIN and the secret phrase.
 * Neither the PIN nor the phrase is ever stored in cleartext.
 *
 * Stored string format: "<iterations>:<base64 salt>:<base64 hash>".
 */
object SecretHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LEN_BITS = 256
    private const val SALT_LEN = 16
    private val rng = SecureRandom()

    fun hash(secret: String): String {
        val salt = ByteArray(SALT_LEN).also { rng.nextBytes(it) }
        val hash = pbkdf2(secret.toCharArray(), salt, ITERATIONS)
        return "$ITERATIONS:${b64(salt)}:${b64(hash)}"
    }

    fun verify(secret: String, stored: String): Boolean {
        return try {
            val parts = stored.split(":")
            if (parts.size != 3) return false
            val iterations = parts[0].toInt()
            val salt = unb64(parts[1])
            val expected = unb64(parts[2])
            val actual = pbkdf2(secret.toCharArray(), salt, iterations)
            constantTimeEquals(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LEN_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    /**
     * Fast SHA-256 hash for the secret *phrase* trigger (not a password).
     * The phrase only opens the vault gate; the PIN/biometric is the real guard.
     * A fast hash is required because detection runs across many word-windows
     * on every keystroke — PBKDF2 here would freeze the editor.
     */
    fun fastHash(text: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(text.toByteArray(Charsets.UTF_8))
        return b64(digest)
    }

    fun fastVerify(text: String, storedFast: String): Boolean =
        fastHash(text) == storedFast
}
