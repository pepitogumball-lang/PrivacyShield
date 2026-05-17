package com.privacyshield.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.data.model.ProtectionRule
import com.privacyshield.data.scanner.AccessibilityScanner
import com.privacyshield.data.scanner.OverlayScanner
import com.privacyshield.data.scanner.PermissionScanner
import com.privacyshield.data.scanner.RecordingRiskScanner
import com.privacyshield.util.PackageUtils
import com.privacyshield.util.RiskCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_shield_prefs")

class AppRepository(private val context: Context) {

    companion object {
        private val PROTECTED_PACKAGES_KEY = stringSetPreferencesKey("protected_packages")
    }

    // ── Protected apps persistence ─────────────────────────────────────────

    val protectedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PROTECTED_PACKAGES_KEY] ?: emptySet()
    }

    suspend fun addProtectedApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PROTECTED_PACKAGES_KEY] ?: emptySet()
            prefs[PROTECTED_PACKAGES_KEY] = current + packageName
        }
    }

    suspend fun removeProtectedApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PROTECTED_PACKAGES_KEY] ?: emptySet()
            prefs[PROTECTED_PACKAGES_KEY] = current - packageName
        }
    }

    // ── App scanning ───────────────────────────────────────────────────────

    suspend fun scanInstalledApps(): List<InstalledAppInfo> {
        val packages: List<PackageInfo> = PackageUtils.getAllInstalledPackages(context)
        val accessibilityPackages = AccessibilityScanner.getAccessibilityPackages(context)
        val protectedPackages = protectedPackagesFlow.first()

        return packages.mapNotNull { packageInfo ->
            buildAppInfo(packageInfo, accessibilityPackages, protectedPackages)
        }.sortedByDescending { it.riskLevel.score }
    }

    private fun buildAppInfo(
        packageInfo: PackageInfo,
        accessibilityPackages: Set<String>,
        protectedPackages: Set<String>
    ): InstalledAppInfo? {
        return try {
            val pkg = packageInfo.packageName ?: return null
            val permissions = PackageUtils.getRequestedPermissions(packageInfo)
            val appName = PackageUtils.getAppLabel(context, pkg)
            val isSystem = PackageUtils.isSystemApp(packageInfo.applicationInfo ?: return null)

            val hasDangerous = PermissionScanner.hasDangerousPermissions(permissions)
            val hasAccessibility = pkg in accessibilityPackages
            val hasOverlay = OverlayScanner.requestsOverlayPermission(packageInfo)
            val hasRecording = RecordingRiskScanner.hasRecordingRisk(packageInfo)

            val risk = RiskCalculator.calculate(
                hasDangerousPermissions = hasDangerous,
                hasAccessibilityService = hasAccessibility,
                hasOverlayPermission = hasOverlay,
                hasRecordingRisk = hasRecording
            )

            InstalledAppInfo(
                packageName = pkg,
                appName = appName,
                permissions = permissions,
                riskLevel = risk,
                hasAccessibilityService = hasAccessibility,
                hasOverlayPermission = hasOverlay,
                hasRecordingRisk = hasRecording,
                hasDangerousPermissions = hasDangerous,
                isSystemApp = isSystem,
                isProtected = pkg in protectedPackages
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getProtectionRules(apps: List<InstalledAppInfo>): List<ProtectionRule> =
        apps.filter { it.isProtected }.map { ProtectionRule(it.packageName, it.appName) }
}
