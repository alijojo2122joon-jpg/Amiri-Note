package com.amiri.note.ui.applock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.amiri.note.security.BiometricHelper
import com.amiri.note.security.SecurePrefs
import com.amiri.note.util.findActivity

/** Full-screen lock shown on app start when App Lock is enabled. */
@Composable
fun AppLockScreen(prefs: SecurePrefs, onUnlocked: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (prefs.biometricEnabled && activity != null && BiometricHelper.isAvailable(activity)) {
            BiometricHelper.authenticate(
                activity, "Unlock Amiri Note", "Verify to continue",
                onSuccess = onUnlocked, onFail = {}
            )
        }
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Amiri Note", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit); error = null },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            if (prefs.verifyPin(pin)) onUnlocked() else error = "Incorrect PIN"
        }, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }

        if (activity != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                BiometricHelper.authenticate(activity, "Unlock Amiri Note", "Verify to continue",
                    onSuccess = onUnlocked, onFail = {})
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Use fingerprint")
            }
        }
    }
}
