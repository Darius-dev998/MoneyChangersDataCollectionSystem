# ─────────────────────────────────────────────────────────────────────────────
#  Money Changers Data Collection System — proguard-rules.pro
#
#  CONTEXT: isMinifyEnabled is now TRUE in release builds (fixed in build.gradle).
#  These rules are therefore ACTIVE and critical.
#
#  Stack: Room + SQLCipher, Hilt, Kotlinx Serialization, Supabase, Ktor,
#         WorkManager, Coil, Paging 3, play-services-location
# ─────────────────────────────────────────────────────────────────────────────

# ── STACK TRACES ─────────────────────────────────────────────────────────────
# Keep source file names and line numbers so crash reports are readable.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses

# ── ROOM + SQLCIPHER ─────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep interface net.sqlcipher.** { *; }

# Keep Room database class
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Keep Room @Entity fields — R8 treats them as "unused" since Room reads them
# via generated code. Without this, columns are stripped → SQLiteException.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Fts4 class * { *; }

# Keep Room @Dao implementations
-keep @androidx.room.Dao interface * { *; }

# Keep Room @TypeConverters
-keep @androidx.room.TypeConverters class * { *; }

# ── HILT / DAGGER ─────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class dagger.hilt.android.internal.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-dontwarn dagger.hilt.**

# ── KOTLINX SERIALIZATION ─────────────────────────────────────────────────────
# Supabase uses kotlinx.serialization for all data model encode/decode.
# Without these rules, @Serializable data classes are stripped and every
# Supabase insert/select silently fails or throws SerializationException.
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ── SUPABASE + KTOR ────────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

# ── WORKMANAGER ───────────────────────────────────────────────────────────────
# Worker classes are instantiated by class name at runtime — R8 would
# strip them without these rules, causing "Could not instantiate Worker".
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ── KOTLIN COROUTINES ─────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── COIL ──────────────────────────────────────────────────────────────────────
# R8 strips Coil's ComponentRegistry — images silently fail to load in release.
-keep class coil.** { *; }
-dontwarn coil.**

# ── PLAY SERVICES LOCATION ────────────────────────────────────────────────────
# Transitive dependency on google-crypto-tink references classes not on
# the classpath. These warnings cause R8 to fail without dontwarn.
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.android.gms.internal.**