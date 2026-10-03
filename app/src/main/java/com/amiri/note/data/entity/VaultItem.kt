package com.amiri.note.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Vault item kinds. */
object VaultKind {
    const val PHOTO = 0
    const val VIDEO = 1
    const val FILE = 2
}

/**
 * A single encrypted item inside the Vault. The real bytes live in the app's
 * private storage at [encPath], AES-GCM encrypted. Nothing here is world-readable.
 */
@Entity(
    tableName = "vault_items",
    indices = [Index("folderId"), Index("kind"), Index("inTrash"), Index("addedAt")]
)
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String = "",
    val kind: Int = VaultKind.FILE,
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0,
    /** absolute path of the encrypted blob in app-private storage */
    val encPath: String = "",
    /** absolute path of an encrypted thumbnail blob, or empty for none */
    val thumbPath: String = "",
    val folderId: Long = 0,          // 0 = root
    val inTrash: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val trashedAt: Long = 0,
    /** duration in ms for videos, 0 otherwise */
    val durationMs: Long = 0
)
