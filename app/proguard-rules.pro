# PrivacyShield ProGuard Rules

# Keep data models
-keep class com.privacyshield.data.model.** { *; }

# Keep ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep Services and Receivers
-keep class com.privacyshield.protection.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep enum names used in UI labels
-keepclassmembers enum com.privacyshield.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
