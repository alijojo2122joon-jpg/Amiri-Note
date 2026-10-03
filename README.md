# Amiri Note

A premium, minimal, **100% offline** notes + daily planner for Android — with a hidden, encrypted **Private Vault** behind a secret phrase and PIN/biometric lock.

Built for **realme GT3** (and any Android 8.0+ device). Kotlin · Jetpack Compose · Material 3 · Room · Android Keystore · BiometricPrompt · Storage Access Framework.

- **Application ID:** `com.amiri.note`
- **minSdk:** 26 (Android 8.0) · **targetSdk/compileSdk:** 34
- **No INTERNET permission.** No Firebase, ads, analytics, tracking, cloud, or external API. Works in airplane mode.

---

## What it does

**Normal app (what anyone sees):**
- Home dashboard: greeting, today's progress bar, quick note, today's tasks, today's money, recent notes
- Notes: create / edit / delete / pin / archive / duplicate / search / categorize; title, body, checklist, category, priority; **auto-save**
- Daily tasks with three states: ✓ completed, × failed, ○ pending
- Daily finance ledger: income / expense / debt / credit, multi-currency (default **AFN**, plus USD/EUR/custom)
- Calendar with per-day notes/tasks/finance and data indicators
- Statistics: weekly bars, weekly/monthly totals, completion %, **My Weekly Review** (you write it)
- Settings: app lock, biometric, PIN, secret phrase, default currency, storage usage, offline backup
- Bottom nav: Home · Notes · Calendar · Statistics · Settings — **no "Vault" anywhere**

**Hidden Private Vault:**
- Entered by typing your **secret phrase** inside any note. Default phrase: `where you hided` (changeable in Settings → Security).
- Flow: **Secret phrase → Vault unlock → PIN / Fingerprint → Vault**. The phrase is only the trigger; the PIN/biometric is the real security.
- Photos, Videos, Files, Folders, Trash, Apps
- Import from the phone via the Storage Access Framework; files are **AES-256-GCM encrypted** into the app's private storage (not just renamed, not a `.hidden` folder)
- Encrypted thumbnails, lazy grid gallery, offline video playback (Media3/ExoPlayer)
- Multi-select (long-press): move to trash, restore, delete, select all
- Trash with restore / delete permanently / empty trash
- Restore any item back out to the device (SAF)

---

## Security

- **File encryption:** AES-256-GCM in 256 KiB authenticated chunks (constant memory for large videos). Each file has its own random data key, wrapped by a master key held in the **Android Keystore** (hardware-backed where available) that never leaves the device.
- **PIN:** never stored in plaintext — PBKDF2-HMAC-SHA256, 120k iterations, random salt, constant-time compare.
- **Secret phrase:** stored only as a PBKDF2 hash, never in cleartext. Detected in a note by hashing candidate word-windows and comparing.
- **Settings secrets** (PIN hash, phrase hash, flags) live in **EncryptedSharedPreferences** (Keystore-backed).
- **App Lock** optionally gates the whole app on launch (PIN / biometric).
- Cloud/auto backup of app data is disabled in the manifest.

## Honest note on "Hidden Apps"

Android does **not** let an ordinary app hide other apps from the launcher. This app does the officially-supported thing instead: a private, PIN-gated **shortcut list** of your installed apps (e.g. WhatsApp) that you can open from inside the vault. It never fakes real hiding. (Uses `queryIntentActivities` + `getLaunchIntentForPackage` — standard APIs.)

---

## Build the APK

The project is build-ready. You do **not** need Android Studio — the easiest path from a phone is GitHub Actions (already included).

### Option A — GitHub Actions (recommended, builds in the cloud)

1. Create a new **GitHub repository** and upload this whole project (keep the folder structure, including `.github/workflows/build.yml`).
2. GitHub → your repo → **Actions** tab → enable workflows if prompted.
3. The workflow runs automatically on push. Or run it manually: **Actions → "Build Amiri Note APK" → Run workflow**.
4. When it finishes (green check), open the run → **Artifacts** → download **`AmiriNote-debug-apk`**.
5. Unzip it → `app-debug.apk`.

The workflow installs the Android SDK, builds with Gradle, and uploads the APK. A release APK is also attempted; it uploads as `AmiriNote-release-apk` if a signing keystore is present (see below), otherwise just use the debug APK.

### Option B — Local machine with Android SDK

```bash
# from the project root
./gradlew assembleDebug          # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # -> app/build/outputs/apk/release/  (needs a keystore for signing)
```

Requires JDK 17 and the Android SDK (`ANDROID_HOME` set). The Gradle wrapper downloads Gradle 8.9 automatically.

### Install on realme GT3

1. Copy `app-debug.apk` to the phone (USB, or download from GitHub on the phone directly).
2. Open it with the Files app → tap **Install**.
3. Allow "Install unknown apps" for the app you opened it from, if prompted.
4. Launch **Amiri Note**.

(`adb install app-debug.apk` also works if you use a computer.)

---

## First run

1. Open a note and type your secret phrase (`where you hided`) anywhere in it.
2. The vault gate appears. First time, you'll **create a PIN** (and can use fingerprint afterward).
3. You're in the Private Vault.
4. Change the phrase or PIN anytime in **Settings → Security**.

---

## Optional: signed release APK

To get a signed `app-release.apk`, add a `keystore.properties` file in the project root:

```
storeFile=/absolute/path/to/your.keystore
storePassword=****
keyAlias=****
keyPassword=****
```

Then `./gradlew assembleRelease`. Without this file, release signing is skipped and the debug APK is the installable one. `keystore.properties` and `*.keystore` are git-ignored.

---

## Project structure

```
AmiriNote/
├─ settings.gradle.kts, build.gradle.kts, gradle.properties
├─ gradlew, gradle/wrapper/…                     # Gradle wrapper (8.9)
├─ .github/workflows/build.yml                   # cloud APK build
└─ app/
   ├─ build.gradle.kts, proguard-rules.pro
   └─ src/main/
      ├─ AndroidManifest.xml                      # no INTERNET permission
      ├─ res/…                                    # theme, icon, strings, file_paths
      └─ java/com/amiri/note/
         ├─ AmiriApp.kt, MainActivity.kt
         ├─ data/{entity,dao,db,repo}/…           # Room
         ├─ security/…                            # Keystore, crypto, PIN, biometric, vault
         ├─ backup/BackupManager.kt               # offline JSON backup
         ├─ util/…
         └─ ui/{home,notes,calendar,stats,settings,vault,applock,theme,common}/…
```

## Architecture

`UI (Compose) → ViewModel → Repository → Room / Keystore-encrypted storage`.
Vault: `Vault UI → VaultViewModel → VaultManager → CryptoManager (Keystore) → app-private storage`.


## v1.1 changes
- Liquid-glass theme (frosted translucent panels over a soft animated gradient).
- Fixed vault import (Keystore IV handling) and made adding photos/videos/files robust (system photo picker + file browser, progress, messages).
- Apps: add apps to a private PIN-protected list (real icons, search, remove, app info).
- Folders now open and can receive items ("Move to folder" from any vault list).
- Viewer: pinch-zoom, EXIF rotation, "Open with…" for files, temp files wiped on close.
- Vault auto-locks after 30 s in the background.
- Settings → Diagnostics shows the last crash report (stored locally only).
