package com.amiri.note.ui

/** All navigation routes. Vault routes are reachable only after unlock. */
object Routes {
    const val HOME = "home"
    const val NOTES = "notes"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    const val NOTE_EDITOR = "note_editor"          // ?id={id}
    const val SEARCH = "search"

    // Vault
    const val VAULT_UNLOCK = "vault_unlock"
    const val VAULT_HOME = "vault_home"
    const val VAULT_GALLERY = "vault_gallery"      // /{kind}
    const val VAULT_PICK = "vault_pick"            // /{kind}
    const val VAULT_FILES = "vault_files"
    const val VAULT_FOLDERS = "vault_folders"
    const val VAULT_FOLDER = "vault_folder"        // /{id}
    const val VAULT_TRASH = "vault_trash"
    const val VAULT_APPS = "vault_apps"
    const val VAULT_VIEWER = "vault_viewer"        // /{id}

    // App lock gate
    const val APP_LOCK = "app_lock"
}
