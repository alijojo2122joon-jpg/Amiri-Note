package com.amiri.note.security

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.amiri.note.data.dao.VaultDao
import com.amiri.note.data.entity.VaultItem
import com.amiri.note.data.entity.VaultKind
import com.amiri.note.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Moves files from the device into the encrypted Vault and back out.
 *
 * Encrypted blobs live in the app's private files dir (filesDir/vault), which is
 * not world-readable and not indexed by the Media gallery. Only ciphertext is
 * stored; the plaintext never touches shared storage once imported.
 */
class VaultManager(
    private val context: Context,
    private val dao: VaultDao
) {
    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { mkdirs() }
    }
    private val thumbDir: File by lazy {
        File(context.filesDir, "vault_thumbs").apply { mkdirs() }
    }
    private val exportDir: File by lazy {
        File(context.cacheDir, "export").apply { mkdirs() }
    }

    data class SourceInfo(val name: String, val size: Long, val mime: String)
    data class ImportResult(val id: Long, val sourceDeleted: Boolean)

    private fun queryInfo(uri: Uri): SourceInfo {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (nameIdx >= 0) c.getString(nameIdx)?.let { name = it }
                if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
            }
        }
        return SourceInfo(name, size, mime)
    }

    private fun kindFor(mime: String): Int = when {
        mime.startsWith("image/") -> VaultKind.PHOTO
        mime.startsWith("video/") -> VaultKind.VIDEO
        else -> VaultKind.FILE
    }

    /**
     * Import a content Uri into the vault. Returns the new item's id.
     * [deleteSource] attempts to remove the original from shared storage/MediaStore.
     */
    suspend fun importUri(uri: Uri, folderId: Long, deleteSource: Boolean): ImportResult =
        withContext(Dispatchers.IO) {
            val info = queryInfo(uri)
            val kind = kindFor(info.mime)
            val stamp = System.currentTimeMillis()
            val encFile = File(vaultDir, "v_${stamp}_${(0..99999).random()}.bin")

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    CryptoManager.encryptStreamToFile(input, encFile)
                } ?: throw IllegalStateException("Cannot open source file")
            } catch (t: Throwable) {
                runCatching { encFile.delete() }
                throw if (t is Exception) t else IllegalStateException(t.message ?: "Import failed", t)
            }

            // Thumbnail (best-effort; never fatal, even on out-of-memory)
            var thumbPath = ""
            var durationMs = 0L
            try {
                when (kind) {
                    VaultKind.PHOTO -> makeImageThumb(uri)?.let { thumbPath = it }
                    VaultKind.VIDEO -> {
                        val pair = makeVideoThumbAndDuration(uri)
                        pair.first?.let { thumbPath = it }
                        durationMs = pair.second
                    }
                }
            } catch (_: Throwable) { /* ignore thumb errors */ }

            val item = VaultItem(
                displayName = info.name,
                kind = kind,
                mimeType = info.mime,
                sizeBytes = if (info.size > 0) info.size else encFile.length(),
                encPath = encFile.absolutePath,
                thumbPath = thumbPath,
                folderId = folderId,
                addedAt = stamp,
                durationMs = durationMs
            )
            val id = dao.insertItem(item)

            ImportResult(id, deleteSource && tryDeleteSource(uri))
        }

    /** Try to remove the original from shared storage. Returns true if it is gone. */
    private fun tryDeleteSource(uri: Uri): Boolean {
        try {
            if (android.provider.DocumentsContract.isDocumentUri(context, uri) &&
                android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
            ) return true
        } catch (_: Throwable) { }
        return try { context.contentResolver.delete(uri, null, null) > 0 } catch (_: Throwable) { false }
    }

    private fun makeImageThumb(uri: Uri): String? {
        val bmp = ImageUtil.decodeSampled(320) { context.contentResolver.openInputStream(uri) } ?: return null
        return writeThumb(bmp)
    }

    private fun makeVideoThumbAndDuration(uri: Uri): Pair<String?, Long> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val path = frame?.let { writeThumb(it) }
            path to durationMs
        } catch (e: Exception) {
            null to 0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun writeThumb(bmp: Bitmap): String {
        val baos = ByteArrayOutputStream()
        val scaled = scaleDown(bmp, 320)
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val thumbFile = File(thumbDir, "t_${System.currentTimeMillis()}_${(0..9999).random()}.bin")
        CryptoManager.encryptBytesToFile(baos.toByteArray(), thumbFile)
        return thumbFile.absolutePath
    }

    private fun scaleDown(bmp: Bitmap, maxDim: Int): Bitmap {
        val w = bmp.width; val h = bmp.height
        if (w <= maxDim && h <= maxDim) return bmp
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(bmp, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    /** Decrypt a thumbnail blob into bytes for display. */
    suspend fun loadThumbBytes(item: VaultItem): ByteArray? = withContext(Dispatchers.IO) {
        if (item.thumbPath.isBlank()) return@withContext null
        val f = File(item.thumbPath)
        if (!f.exists()) return@withContext null
        try { CryptoManager.decryptToBytes(f) } catch (_: Exception) { null }
    }

    /** Decrypt the full blob into a temporary plaintext file for viewing/playback. */
    suspend fun decryptToTemp(item: VaultItem): File = withContext(Dispatchers.IO) {
        val safeName = item.displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(exportDir, "tmp_${item.id}_${System.currentTimeMillis()}_$safeName")
        out.outputStream().use { os ->
            CryptoManager.decryptToFile(File(item.encPath), os)
        }
        out.deleteOnExit()
        out
    }

    /** Restore an item's plaintext to a destination Uri (chosen via SAF) and keep or remove the vault copy. */
    suspend fun restoreToUri(item: VaultItem, dest: Uri, removeFromVault: Boolean) =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(dest)?.use { os ->
                CryptoManager.decryptToFile(File(item.encPath), os)
            } ?: throw IllegalStateException("Cannot open destination")
            if (removeFromVault) permanentlyDelete(listOf(item.id))
        }

    /** Permanently delete items: removes encrypted blobs, thumbnails and rows. */
    suspend fun permanentlyDelete(ids: List<Long>) = withContext(Dispatchers.IO) {
        val items = dao.getItems(ids)
        for (it in items) {
            runCatching { File(it.encPath).delete() }
            if (it.thumbPath.isNotBlank()) runCatching { File(it.thumbPath).delete() }
            dao.deleteItem(it)
        }
    }

    suspend fun clearExportCache() = withContext(Dispatchers.IO) {
        exportDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    suspend fun vaultBytesOnDisk(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        vaultDir.listFiles()?.forEach { total += it.length() }
        thumbDir.listFiles()?.forEach { total += it.length() }
        total
    }
}
