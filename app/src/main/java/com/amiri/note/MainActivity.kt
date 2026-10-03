package com.amiri.note

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.amiri.note.security.SecurePrefs
import com.amiri.note.ui.AmiriAppRoot
import com.amiri.note.ui.applock.AppLockScreen
import com.amiri.note.ui.common.GlassBackground
import com.amiri.note.ui.theme.AmiriTheme

/**
 * Single activity. FragmentActivity is required for androidx BiometricPrompt.
 * Hosts the app-lock gate, then the Compose app.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = SecurePrefs.get(this)

        setContent {
            AmiriTheme(darkTheme = true) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                Box(Modifier.fillMaxSize()) {
                    GlassBackground()
                    // App Lock gate. If enabled, show lock until unlocked.
                    var locked by remember { mutableStateOf(prefs.appLockEnabled) }
                    if (locked) {
                        AppLockScreen(prefs = prefs, onUnlocked = { locked = false })
                    } else {
                        AmiriAppRoot()
                    }
                }
                }
            }
        }
    }
}
