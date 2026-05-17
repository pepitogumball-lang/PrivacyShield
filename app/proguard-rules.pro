# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep data model classes (used by DataStore)
-keep class com.privacyshield.data.model.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep enum names used in UI labels
-keepclassmembers enum com.privacyshield.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
