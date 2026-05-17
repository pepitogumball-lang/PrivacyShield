package com.privacyshield.data.model

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val permissions: List<String>,
    val riskLevel: RiskLevel,
    val hasAccessibilityService: Boolean,
    val hasOverlayPermission: Boolean,
    val hasRecordingRisk: Boolean,
    val hasDangerousPermissions: Boolean,
    val isSystemApp: Boolean,
    val isProtected: Boolean = false
)
