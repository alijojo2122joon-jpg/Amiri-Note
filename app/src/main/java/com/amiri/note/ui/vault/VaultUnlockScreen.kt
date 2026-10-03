package com.amiri.note.ui.vault

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.amiri.note.security.BiometricHelper
import com.amiri.note.util.findActivity

/**
 * Vault gate. Triggered by the secret phrase; the vault only actually opens
 * after PIN or biometric auth. If no PIN exists yet, the user sets one here.
 */
@Composable
fun VaultUnlockScreen(
    vm: VaultViewModel,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    val activity = LocalContext.current.findActivity()
    val needsSetup = !vm.hasPin()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Offer biometric immediately when available and a PIN is set.
    LaunchedEffect(Unit) {
        if (!needsSetup && vm.biometricEnabled() && activity != null &&
            BiometricHelper.isAvailable(activity)
        ) {
            BiometricHelper.authenticate(
                activity,
                title = "Unlock Vault",
                subtitle = "Verify to open your private vault",
                onSuccess = { vm.unlock(); onUnlocked() },
                onFail = { /* stay on PIN entry */ }
            )
        }
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null,
            modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(if (needsSetup) "Set up your Vault" else "Private Vault",
            style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(if (needsSetup) "Create a PIN to protect your private files."
            else "Enter your PIN to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
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
        if (needsSetup) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.filter(Char::isDigit); error = null },
                label = { Text("Confirm PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (needsSetup) {
                    when {
                        pin.length < 4 -> error = "PIN must be at least 4 digits"
                        pin != confirm -> error = "PINs don't match"
                        else -> { vm.setPin(pin); vm.unlock(); onUnlocked() }
                    }
                } else {
                    if (vm.verifyPin(pin)) { vm.unlock(); onUnlocked() }
                    else error = "Incorrect PIN"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (needsSetup) "Create & open" else "Unlock") }

        if (!needsSetup && activity != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    BiometricHelper.authenticate(
                        activity, "Unlock Vault", "Verify to open your private vault",
                        onSuccess = { vm.unlock(); onUnlocked() },
                        onFail = {}
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Use fingerprint")
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
