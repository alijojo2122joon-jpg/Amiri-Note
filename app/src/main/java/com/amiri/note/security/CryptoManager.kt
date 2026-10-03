package com.amiri.note.security

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Chunked AES-256-GCM file encryption.
 *
 * Every file gets its own random 256-bit data key. That key is wrapped
 * (encrypted) by the hardware-backed Android Keystore master key and stored in
 * the file header. The body is split into 256 KiB chunks, each sealed with
 * AES-GCM using a unique IV (8-byte per-file nonce + 4-byte counter) and an
 * "is-last-chunk" flag as AAD, so reordering and truncation are detected.
 *
 * Chunking keeps memory flat for any file size (large videos included) and
 * avoids the Keystore limitation of buffering a whole GCM stream in memory.
 *
 * Layout: [version=2][wrapIvLen][wrapIv][wrappedLen][wrappedKey][8B nonce]
 *         then repeated: [int ctLen][ciphertext+tag]
 */
object CryptoManager {

    private const val VERSION = 2
    private const val CHUNK = 256 * 1024
    private const val TAG_BITS = 128
    private const val TAG_BYTES = 16
    private const val NONCE_LEN = 8
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private val rng = SecureRandom()

    // ---------- key wrapping (Keystore) ----------

    private fun wrap(dataKey: ByteArray): Pair<ByteArray, ByteArray> {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, KeystoreManager.getOrCreateKey())
        val iv = c.iv
        return iv to c.doFinal(dataKey)
    }

    private fun unwrap(iv: ByteArray, wrapped: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, KeystoreManager.getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return c.doFinal(wrapped)
    }

    private fun chunkIv(nonce: ByteArray, counter: Int): ByteArray {
        val iv = ByteArray(12)
        System.arraycopy(nonce, 0, iv, 0, NONCE_LEN)
        iv[8] = (counter ushr 24).toByte()
        iv[9] = (counter ushr 16).toByte()
        iv[10] = (counter ushr 8).toByte()
        iv[11] = counter.toByte()
        return iv
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var n = 0
        while (n < buf.size) {
            val r = input.read(buf, n, buf.size - n)
            if (r < 0) break
            n += r
        }
        return n
    }

    // ---------- encrypt ----------

    /** Encrypt [input] into file [dest]. The caller still owns (closes) [input]. */
    fun encryptStreamToFile(input: InputStream, dest: File) {
        val dataKey = ByteArray(32).also { rng.nextBytes(it) }
        val nonce = ByteArray(NONCE_LEN).also { rng.nextBytes(it) }
        val (wrapIv, wrapped) = wrap(dataKey)
        val keySpec = SecretKeySpec(dataKey, "AES")

        DataOutputStream(dest.outputStream().buffered()).use { out ->
            out.writeByte(VERSION)
            out.writeByte(wrapIv.size); out.write(wrapIv)
            out.writeByte(wrapped.size); out.write(wrapped)
            out.write(nonce)

            var cur = ByteArray(CHUNK)
            var next = ByteArray(CHUNK)
            var curLen = readFully(input, cur)
            var counter = 0
            while (true) {
                val nextLen = if (curLen == CHUNK) readFully(input, next) else 0
                val last = nextLen <= 0
                val cipher = Cipher.getInstance(TRANSFORM)
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(TAG_BITS, chunkIv(nonce, counter++)))
                cipher.updateAAD(byteArrayOf(if (last) 1 else 0))
                val ct = cipher.doFinal(cur, 0, curLen)
                out.writeInt(ct.size)
                out.write(ct)
                if (last) break
                val tmp = cur; cur = next; next = tmp
                curLen = nextLen
            }
        }
    }

    fun encryptBytesToFile(data: ByteArray, dest: File) {
        data.inputStream().use { encryptStreamToFile(it, dest) }
    }

    // ---------- decrypt ----------

    /** Decrypt [src] and write the plaintext to [dest]. Does not close [dest]. */
    fun decryptToFile(src: File, dest: OutputStream) {
        val bis = BufferedInputStream(src.inputStream(), 64 * 1024)
        bis.use {
            val din = DataInputStream(bis)
            val version = din.readUnsignedByte()
            if (version != VERSION) throw IllegalStateException("Unsupported vault blob version: $version")
            val wrapIv = ByteArray(din.readUnsignedByte()).also { din.readFully(it) }
            val wrapped = ByteArray(din.readUnsignedByte()).also { din.readFully(it) }
            val nonce = ByteArray(NONCE_LEN).also { din.readFully(it) }
            val keySpec = SecretKeySpec(unwrap(wrapIv, wrapped), "AES")

            var counter = 0
            while (true) {
                val ctLen = try { din.readInt() } catch (e: EOFException) {
                    throw IllegalStateException("Corrupt vault blob (truncated)")
                }
                if (ctLen < TAG_BYTES || ctLen > CHUNK + TAG_BYTES) {
                    throw IllegalStateException("Corrupt vault blob (bad chunk size)")
                }
                val ct = ByteArray(ctLen)
                din.readFully(ct)

                bis.mark(1)
                val peek = bis.read()
                val last = peek < 0
                if (!last) bis.reset()

                val cipher = Cipher.getInstance(TRANSFORM)
                cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(TAG_BITS, chunkIv(nonce, counter++)))
                cipher.updateAAD(byteArrayOf(if (last) 1 else 0))
                dest.write(cipher.doFinal(ct))
                if (last) break
            }
        }
    }

    fun decryptToBytes(src: File): ByteArray {
        val bos = ByteArrayOutputStream()
        decryptToFile(src, bos)
        return bos.toByteArray()
    }
}
