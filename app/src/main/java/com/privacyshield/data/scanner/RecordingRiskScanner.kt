package com.privacyshield.data.scanner

import android.content.pm.PackageInfo

/**
 * Evaluates recording-related risk for a given package.
 *
 * On stock Android, a normal app cannot fully enumerate which apps are
 * actively capturing screen content (MediaProjection) at any moment.
 * What we CAN detect: permissions that indicate recording capability,
 * which is a meaningful risk signal without requiring system privileges.
 */
object RecordingRiskScanner {

    fun hasRecordingRisk(packageInfo: PackageInfo): Boolean {
        val permissions = packageInfo.requestedPermissions?.toList() ?: return false
        return PermissionScanner.hasRecordingRiskPermissions(permissions)
    }
}
