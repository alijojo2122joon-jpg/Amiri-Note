# Keep Room schema
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Keep Kotlin metadata / coroutines
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Coil
-dontwarn coil.**

# Keep data classes used for backup (de)serialization via reflection-free manual JSON, safe defaults
-keepclassmembers class com.amiri.note.data.entity.** { *; }
-keepclassmembers class com.amiri.note.backup.** { *; }

# BiometricPrompt
-keep class androidx.biometric.** { *; }
