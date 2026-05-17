package com.privacyshield.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    val protectedPackages = repository.protectedPackagesFlow

    init {
        scanApps()
    }

    fun scanApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            try {
                val apps = repository.scanInstalledApps()
                _uiState.update {
                    it.copy(
                        apps = apps,
                        isScanning = false,
                        lastScanTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isScanning = false, error = e.localizedMessage)
                }
            }
        }
    }

    fun addProtectedApp(packageName: String) {
        viewModelScope.launch {
            repository.addProtectedApp(packageName)
            // Re-scan so the isProtected flag reflects the change immediately
            scanApps()
        }
    }

    fun removeProtectedApp(packageName: String) {
        viewModelScope.launch {
            repository.removeProtectedApp(packageName)
            scanApps()
        }
    }
}
