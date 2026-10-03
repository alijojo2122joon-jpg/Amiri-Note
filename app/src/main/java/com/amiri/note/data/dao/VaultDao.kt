package com.amiri.note.data.dao

import androidx.room.*
import com.amiri.note.data.entity.VaultFolder
import com.amiri.note.data.entity.VaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    // ---- Items ----
    @Query("SELECT * FROM vault_items WHERE inTrash = 0 AND folderId = :folderId AND kind = :kind ORDER BY addedAt DESC")
    fun observeByKindInFolder(kind: Int, folderId: Long): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE inTrash = 0 AND kind = :kind ORDER BY addedAt DESC")
    fun observeByKind(kind: Int): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE inTrash = 0 AND folderId = :folderId ORDER BY addedAt DESC")
    fun observeInFolder(folderId: Long): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE inTrash = 1 ORDER BY trashedAt DESC")
    fun observeTrash(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItem(id: Long): VaultItem?

    @Query("SELECT * FROM vault_items")
    suspend fun getAllItems(): List<VaultItem>

    @Query("SELECT COALESCE(SUM(sizeBytes),0) FROM vault_items")
    suspend fun totalBytes(): Long

    @Insert suspend fun insertItem(item: VaultItem): Long
    @Update suspend fun updateItem(item: VaultItem)
    @Delete suspend fun deleteItem(item: VaultItem)

    @Query("UPDATE vault_items SET inTrash = 1, trashedAt = :now WHERE id IN (:ids)")
    suspend fun moveToTrash(ids: List<Long>, now: Long)

    @Query("UPDATE vault_items SET inTrash = 0, trashedAt = 0 WHERE id IN (:ids)")
    suspend fun restoreFromTrash(ids: List<Long>)

    @Query("UPDATE vault_items SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<Long>, folderId: Long)

    @Query("SELECT * FROM vault_items WHERE id IN (:ids)")
    suspend fun getItems(ids: List<Long>): List<VaultItem>

    // ---- Folders ----
    @Query("SELECT * FROM vault_folders ORDER BY name")
    fun observeFolders(): Flow<List<VaultFolder>>

    @Query("SELECT * FROM vault_folders")
    suspend fun getAllFolders(): List<VaultFolder>

    @Insert suspend fun insertFolder(folder: VaultFolder): Long
    @Update suspend fun updateFolder(folder: VaultFolder)

    @Query("DELETE FROM vault_folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)
}
