package com.privacyshield.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.data.repository.AppRepository
import com.privacyshield.util.IconCache
import com.privacyshield.util.PerformanceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanStats(
    val lastDurationMs: Long = 0L,
    val totalApps: Int = 0,
    val iconCacheSize: Int = 0
)

data class AppUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val isScanning: Boolean = false,
    val lastScanTime: Long? = null,
    val error: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _scanStats = MutableStateFlow(ScanStats())
    val scanStats: StateFlow<ScanStats> = _scanStats.asStateFlow()

    val protectedPackages = repository.protectedPackagesFlow

    val performanceMode: StateFlow<PerformanceMode> = repository.performanceModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PerformanceMode.BALANCED
        )

    private var scanJob: Job? = null
    private var lastScanStartedAt: Long = 0L

    init {
        scanApps()
    }

    fun scanApps(force: Boolean = false) {
        // Debounce: don't start a new scan if one is already running
        if (_uiState.value.isScanning) return

        val mode = performanceMode.value
        val now = System.currentTimeMillis()
        val minInterval = if (force) 0L else mode.minScanIntervalMs

        if (!force && (now - lastScanStartedAt) < minInterval) return

        lastScanStartedAt = now
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            val startMs = System.currentTimeMillis()
            try {
                val cacheTtl = mode.minScanIntervalMs
                val apps = repository.scanInstalledApps(
                    forceRefresh = force,
                    cacheTtlMs = cacheTtl
                )
                val durationMs = System.currentTimeMillis() - startMs
                _uiState.update {
                    it.copy(
                        apps = apps,
                        isScanning = false,
                        lastScanTime = System.currentTimeMillis()
                    )
                }
                _scanStats.update {
                    it.copy(
                        lastDurationMs = durationMs,
                        totalApps = apps.size,
                        iconCacheSize = IconCache.size()
                    )
                }
                // Preload icons in background if MAXIMUM mode
                if (mode.preloadIcons) {
                    preloadIcons(apps)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isScanning = false, error = e.localizedMessage)
                }
            }
        }
    }

    private fun preloadIcons(apps: List<InstalledAppInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            apps.forEach { app ->
                IconCache.loadIcon(ctx, app.packageName)
            }
            _scanStats.update { it.copy(iconCacheSize = IconCache.size()) }
        }
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        viewModelScope.launch {
            repository.setPerformanceMode(mode)
            // Immediately invalidate cache so next scan reflects new interval
            repository.invalidateCache()
        }
    }

    fun clearIconCache() {
        IconCache.clear()
        _scanStats.update { it.copy(iconCacheSize = 0) }
    }

    fun addProtectedApp(packageName: String) {
        viewModelScope.launch {
            repository.addProtectedApp(packageName)
            // Update in-place without full rescan
            _uiState.update { state ->
                state.copy(apps = state.apps.map { app ->
                    if (app.packageName == packageName) app.copy(isProtected = true) else app
                })
            }
        }
    }

    fun removeProtectedApp(packageName: String) {
        viewModelScope.launch {
            repository.removeProtectedApp(packageName)
            _uiState.update { state ->
                state.copy(apps = state.apps.map { app ->
                    if (app.packageName == packageName) app.copy(isProtected = false) else app
                })
            }
        }
    }
}
