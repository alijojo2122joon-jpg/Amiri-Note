package com.amiri.note.backup

import com.amiri.note.data.db.AppDatabase
import com.amiri.note.data.entity.*
import com.amiri.note.security.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Fully offline backup/restore of the normal app data (not the encrypted vault
 * blobs). Produces a single JSON document the user can export via SAF and import
 * later. Uses org.json (bundled in Android) — no external libraries, no network.
 */
class BackupManager(
    private val db: AppDatabase,
    private val prefs: SecurePrefs
) {
    companion object { const val FORMAT_VERSION = 1 }

    fun databaseFileBytes(): Long {
        return try {
            val path = db.openHelper.readableDatabase.path ?: return 0
            File(path).length()
        } catch (_: Exception) { 0 }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("format", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("defaultCurrency", prefs.defaultCurrency)

        val notes = JSONArray()
        db.noteDao().getAll().forEach { n ->
            notes.put(JSONObject().apply {
                put("id", n.id); put("title", n.title); put("body", n.body)
                put("checklist", n.checklist); put("category", n.category)
                put("priority", n.priority); put("isPinned", n.isPinned)
                put("isArchived", n.isArchived); put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt); put("dueAt", n.dueAt)
            })
        }
        root.put("notes", notes)

        val tasks = JSONArray()
        db.taskDao().getAll().forEach { t ->
            tasks.put(JSONObject().apply {
                put("id", t.id); put("title", t.title); put("status", t.status)
                put("dayKey", t.dayKey); put("priority", t.priority)
                put("sortOrder", t.sortOrder); put("createdAt", t.createdAt); put("updatedAt", t.updatedAt)
            })
        }
        root.put("tasks", tasks)

        val finance = JSONArray()
        db.financeDao().getAll().forEach { f ->
            finance.put(JSONObject().apply {
                put("id", f.id); put("type", f.type); put("amount", f.amount)
                put("currency", f.currency); put("description", f.description)
                put("category", f.category); put("dayKey", f.dayKey); put("createdAt", f.createdAt)
            })
        }
        root.put("finance", finance)

        val reviews = JSONArray()
        db.weeklyReviewDao().getAll().forEach { r ->
            reviews.put(JSONObject().apply {
                put("weekKey", r.weekKey); put("text", r.text); put("updatedAt", r.updatedAt)
            })
        }
        root.put("reviews", reviews)

        root.toString(2)
    }

    /** Restore from a JSON document. Replaces existing rows by id. Returns item count. */
    suspend fun importJson(json: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        if (!root.has("notes") && !root.has("tasks") && !root.has("finance")) {
            throw IllegalArgumentException("Not a valid Amiri Note backup.")
        }
        var count = 0

        root.optString("defaultCurrency").takeIf { it.isNotBlank() }?.let {
            prefs.defaultCurrency = it
        }

        root.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.noteDao().upsert(
                    Note(
                        id = o.optLong("id"),
                        title = o.optString("title"),
                        body = o.optString("body"),
                        checklist = o.optString("checklist"),
                        category = o.optString("category", "General"),
                        priority = o.optInt("priority"),
                        isPinned = o.optBoolean("isPinned"),
                        isArchived = o.optBoolean("isArchived"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                        dueAt = o.optLong("dueAt")
                    )
                ); count++
            }
        }
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.taskDao().upsert(
                    TaskItem(
                        id = o.optLong("id"), title = o.optString("title"),
                        status = o.optInt("status"), dayKey = o.optString("dayKey"),
                        priority = o.optInt("priority"), sortOrder = o.optInt("sortOrder"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                ); count++
            }
        }
        root.optJSONArray("finance")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.financeDao().upsert(
                    FinanceRecord(
                        id = o.optLong("id"), type = o.optInt("type"),
                        amount = o.optDouble("amount"), currency = o.optString("currency", "AFN"),
                        description = o.optString("description"), category = o.optString("category", "General"),
                        dayKey = o.optString("dayKey"), createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                ); count++
            }
        }
        root.optJSONArray("reviews")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.weeklyReviewDao().upsert(
                    WeeklyReview(o.getString("weekKey"), o.optString("text"), o.optLong("updatedAt", System.currentTimeMillis()))
                ); count++
            }
        }
        count
    }
}
