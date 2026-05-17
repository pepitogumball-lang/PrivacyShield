package com.privacyshield.data.scanner

import android.content.pm.PackageInfo
import android.provider.Settings

/**
 * Detects whether an app requests the SYSTEM_ALERT_WINDOW permission,
 * which grants the ability to draw overlays on top of other apps.
 *
 * Settings.canDrawOverlays() only checks the *calling* app, so we rely on
 * the declared permission as the risk signal for third-party apps.
 */
object OverlayScanner {

    private const val OVERLAY_PERMISSION = android.Manifest.permission.SYSTEM_ALERT_WINDOW

    fun requestsOverlayPermission(packageInfo: PackageInfo): Boolean {
        val permissions = packageInfo.requestedPermissions ?: return false
        return OVERLAY_PERMISSION in permissions
    }
}
