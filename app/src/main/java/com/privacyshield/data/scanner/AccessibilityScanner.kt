package com.privacyshield.data.scanner

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

/**
 * Detects which packages have registered an enabled accessibility service.
 *
 * Note: On Android 11+ the list of enabled accessibility services is not
 * directly enumerable via PackageManager alone from a normal app.
 * The Settings.Secure key "enabled_accessibility_services" is readable
 * without special permissions and returns a colon-separated list of
 * ComponentName strings (package/class). This is the safest real approach.
 */
object AccessibilityScanner {

    fun getAccessibilityPackages(context: Context): Set<String> {
        return try {
            val raw = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return emptySet()

            if (TextUtils.isEmpty(raw)) return emptySet()

            raw.split(":")
                .mapNotNull { component ->
                    component.trim().split("/").firstOrNull()?.takeIf { it.isNotBlank() }
                }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun hasAccessibilityService(context: Context, packageName: String): Boolean =
        packageName in getAccessibilityPackages(context)
}
