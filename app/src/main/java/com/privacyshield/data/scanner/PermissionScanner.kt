package com.privacyshield.data.scanner

import android.Manifest

/**
 * Classifies requested permissions into risk categories.
 * All permission strings are platform-defined constants — no invented APIs.
 */
object PermissionScanner {

    private val DANGEROUS_PERMISSIONS = setOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.USE_BIOMETRIC,
        Manifest.permission.USE_FINGERPRINT,
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.CALL_PHONE,
    )

    /** Permissions associated with screen/audio capture risk. */
    private val RECORDING_RISK_PERMISSIONS = setOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAPTURE_AUDIO_OUTPUT,
        // CAPTURE_VIDEO_OUTPUT is a signature permission but still a risk signal
        "android.permission.CAPTURE_VIDEO_OUTPUT",
        "android.permission.READ_FRAME_BUFFER",
        Manifest.permission.FOREGROUND_SERVICE,
    )

    fun hasDangerousPermissions(permissions: List<String>): Boolean =
        permissions.any { it in DANGEROUS_PERMISSIONS }

    fun hasRecordingRiskPermissions(permissions: List<String>): Boolean =
        permissions.any { it in RECORDING_RISK_PERMISSIONS }

    fun dangerousPermissionsIn(permissions: List<String>): List<String> =
        permissions.filter { it in DANGEROUS_PERMISSIONS }
}
