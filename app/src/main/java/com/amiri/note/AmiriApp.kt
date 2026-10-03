package com.amiri.note

import android.app.Application
import com.amiri.note.data.db.AppDatabase
import com.amiri.note.data.repo.AppRepository
import com.amiri.note.security.SecurePrefs
import com.amiri.note.security.VaultManager
import com.amiri.note.util.CrashLogger

/** App-wide singletons. Kept simple and manual — no DI framework needed. */
class AmiriApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: AppRepository
        private set
    lateinit var vaultManager: VaultManager
        private set
    lateinit var securePrefs: SecurePrefs
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        // Never leave decrypted temp files behind from a previous session.
        runCatching { java.io.File(cacheDir, "export").listFiles()?.forEach { it.delete() } }
        database = AppDatabase.get(this)
        repository = AppRepository(database)
        vaultManager = VaultManager(this, database.vaultDao())
        securePrefs = SecurePrefs.get(this)
    }

    companion object {
        fun from(app: Application): AmiriApp = app as AmiriApp
    }
}
