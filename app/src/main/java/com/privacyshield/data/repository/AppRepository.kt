package com.privacyshield.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.data.model.ProtectionRule
import com.privacyshield.data.scanner.AccessibilityScanner
import com.privacyshield.data.scanner.OverlayScanner
import com.privacyshield.data.scanner.PermissionScanner
import com.privacyshield.data.scanner.RecordingRiskScanner
import com.privacyshield.util.PackageUtils
import com.privacyshield.util.PerformanceMode
import com.privacyshield.util.RiskCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_shield_prefs")

data class ScanSettings(
    val scanAccessibility: Boolean = true,
    val scanOverlay: Boolean = true,
    val scanRecording: Boolean = true,
    val scanDangerous: Boolean = true,
    val notifyHighRisk: Boolean = false
)

class AppRepository(private val context: Context) {

    companion object {
        private val PROTECTED_PACKAGES_KEY = stringSetPreferencesKey("protected_packages")
        private val PERFORMANCE_MODE_KEY = intPreferencesKey("performance_mode")
        private val SCAN_ACCESSIBILITY_KEY = booleanPreferencesKey("scan_accessibility")
        private val SCAN_OVERLAY_KEY = booleanPreferencesKey("scan_overlay")
        private val SCAN_RECORDING_KEY = booleanPreferencesKey("scan_recording")
        private val SCAN_DANGEROUS_KEY = booleanPreferencesKey("scan_dangerous")
        private val NOTIFY_HIGH_RISK_KEY = booleanPreferencesKey("notify_high_risk")
        private val ACTIVE_FILTER_KEY = stringPreferencesKey("active_filter")
    }

    // ── In-memory scan cache ───────────────────────────────────────────────

    @Volatile private var cachedBaseApps: List<InstalledAppInfo> = emptyList()
    @Volatile private var cacheTimestamp: Long = 0L

    fun invalidateCache() {
        cacheTimestamp = 0L
    }

    // ── Performance mode ───────────────────────────────────────────────────

    val performanceModeFlow: Flow<PerformanceMode> = context.dataStore.data.map { prefs ->
        PerformanceMode.fromOrdinal(prefs[PERFORMANCE_MODE_KEY] ?: PerformanceMode.BALANCED.ordinal)
    }

    suspend fun setPerformanceMode(mode: PerformanceMode) {
        context.dataStore.edit { prefs ->
            prefs[PERFORMANCE_MODE_KEY] = mode.ordinal
        }
    }

    // ── Scan settings ──────────────────────────────────────────────────────

    val scanSettingsFlow: Flow<ScanSettings> = context.dataStore.data.map { prefs ->
        ScanSettings(
            scanAccessibility = prefs[SCAN_ACCESSIBILITY_KEY] ?: true,
            scanOverlay = prefs[SCAN_OVERLAY_KEY] ?: true,
            scanRecording = prefs[SCAN_RECORDING_KEY] ?: true,
            scanDangerous = prefs[SCAN_DANGEROUS_KEY] ?: true,
            notifyHighRisk = prefs[NOTIFY_HIGH_RISK_KEY] ?: false
        )
    }

    suspend fun updateScanSettings(settings: ScanSettings) {
        context.dataStore.edit { prefs ->
            prefs[SCAN_ACCESSIBILITY_KEY] = settings.scanAccessibility
            prefs[SCAN_OVERLAY_KEY] = settings.scanOverlay
            prefs[SCAN_RECORDING_KEY] = settings.scanRecording
            prefs[SCAN_DANGEROUS_KEY] = settings.scanDangerous
            prefs[NOTIFY_HIGH_RISK_KEY] = settings.notifyHighRisk
        }
    }

    // ── Active filter ──────────────────────────────────────────────────────

    val activeFilterFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_FILTER_KEY] ?: "ALL"
    }

    suspend fun setActiveFilter(filterName: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_FILTER_KEY] = filterName
        }
    }

    // ── Protected apps ─────────────────────────────────────────────────────

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

    suspend fun scanInstalledApps(
        forceRefresh: Boolean = false,
        cacheTtlMs: Long = 60_000L,
        settings: ScanSettings = ScanSettings()
    ): List<InstalledAppInfo> {
        val protectedPackages = protectedPackagesFlow.first()
        val now = System.currentTimeMillis()

        if (!forceRefresh && cachedBaseApps.isNotEmpty() && (now - cacheTimestamp) < cacheTtlMs) {
            return cachedBaseApps.map { it.copy(isProtected = it.packageName in protectedPackages) }
        }

        val freshApps = withContext(Dispatchers.Default) {
            val packages: List<PackageInfo> = PackageUtils.getAllInstalledPackages(context)
            val accessibilityPackages = if (settings.scanAccessibility) {
                AccessibilityScanner.getAccessibilityPackages(context)
            } else emptySet()

            packages.mapNotNull { packageInfo ->
                buildAppInfo(packageInfo, accessibilityPackages, protectedPackages, settings)
            }.sortedByDescending { it.riskLevel.score }
        }

        cachedBaseApps = freshApps.map { it.copy(isProtected = false) }
        cacheTimestamp = now

        return freshApps
    }

    private fun buildAppInfo(
        packageInfo: PackageInfo,
        accessibilityPackages: Set<String>,
        protectedPackages: Set<String>,
        settings: ScanSettings
    ): InstalledAppInfo? {
        return try {
            val pkg = packageInfo.packageName ?: return null
            val permissions = PackageUtils.getRequestedPermissions(packageInfo)
            val appName = PackageUtils.getAppLabel(context, pkg)
            val isSystem = PackageUtils.isSystemApp(packageInfo.applicationInfo ?: return null)

            val hasDangerous = settings.scanDangerous && PermissionScanner.hasDangerousPermissions(permissions)
            val hasAccessibility = settings.scanAccessibility && pkg in accessibilityPackages
            val hasOverlay = settings.scanOverlay && OverlayScanner.requestsOverlayPermission(packageInfo)
            val hasRecording = settings.scanRecording && RecordingRiskScanner.hasRecordingRisk(packageInfo)

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
        } catch (_: Exception) {
            null
        }
    }

    fun getProtectionRules(apps: List<InstalledAppInfo>): List<ProtectionRule> =
        apps.filter { it.isProtected }.map { ProtectionRule(it.packageName, it.appName) }
}
