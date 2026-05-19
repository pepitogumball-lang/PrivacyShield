package com.privacyshield.remote.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.remote.discovery.NetworkDiscovery
import com.privacyshield.remote.model.DeviceType
import com.privacyshield.remote.model.RemoteCommand
import com.privacyshield.remote.model.RemoteDevice
import com.privacyshield.remote.repository.RemoteRepository
import com.privacyshield.remote.transport.AndroidTvTransport
import com.privacyshield.remote.transport.CommandResult
import com.privacyshield.remote.transport.ConnectionResult
import com.privacyshield.remote.transport.IrTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemoteUiState(
    val discoveredDevices: List<RemoteDevice> = emptyList(),
    val selectedDevice: RemoteDevice? = null,
    val connectionState: RemoteConnectionState = RemoteConnectionState.Disconnected,
    val discoveryState: DiscoveryState = DiscoveryState.Idle,
    val irAvailable: Boolean = false,
    val lastError: String? = null,
    val pairingHint: String? = null
)

sealed class RemoteConnectionState {
    object Disconnected : RemoteConnectionState()
    object Connecting : RemoteConnectionState()
    data class PairingRequired(val hint: String) : RemoteConnectionState()
    object Connected : RemoteConnectionState()
    data class Error(val message: String) : RemoteConnectionState()
}

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Scanning : DiscoveryState()
    data class Done(val count: Int) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RemoteRepository(application)
    private val discovery = NetworkDiscovery(application)
    private val irTransport = IrTransport(application)
    private var activeTvTransport: AndroidTvTransport? = null
    private var discoveryJob: Job? = null

    private val _uiState = MutableStateFlow(
        RemoteUiState(
            irAvailable = application.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
                    && irTransport.isAvailable
        )
    )
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    val savedDevices: StateFlow<List<RemoteDevice>> = repository.savedDevicesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Restore last-used device selection
        viewModelScope.launch {
            val last = repository.getLastDevice()
            if (last != null) {
                _uiState.update { it.copy(selectedDevice = last) }
            }
        }
    }

    // ── Discovery ─────────────────────────────────────────────────────────

    fun startDiscovery() {
        if (_uiState.value.discoveryState == DiscoveryState.Scanning) return
        _uiState.update { it.copy(discoveryState = DiscoveryState.Scanning, discoveredDevices = emptyList()) }

        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            val found = mutableListOf<RemoteDevice>()
            try {
                discovery.discoverAndroidTvDevices().collect { device ->
                    if (found.none { it.id == device.id }) {
                        found.add(device)
                        _uiState.update { it.copy(discoveredDevices = found.toList()) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(discoveryState = DiscoveryState.Error(e.message ?: "Discovery failed"))
                }
                return@launch
            }
            _uiState.update { it.copy(discoveryState = DiscoveryState.Done(found.size)) }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        val cur = _uiState.value.discoveryState
        if (cur == DiscoveryState.Scanning) {
            _uiState.update { it.copy(discoveryState = DiscoveryState.Done(it.discoveredDevices.size)) }
        }
    }

    // ── Connection ────────────────────────────────────────────────────────

    fun selectDevice(device: RemoteDevice) {
        _uiState.update { it.copy(selectedDevice = device, connectionState = RemoteConnectionState.Disconnected) }
        viewModelScope.launch { repository.setLastDeviceId(device.id) }
    }

    fun connect() {
        val device = _uiState.value.selectedDevice ?: return
        if (_uiState.value.connectionState == RemoteConnectionState.Connecting) return

        _uiState.update { it.copy(connectionState = RemoteConnectionState.Connecting, lastError = null) }

        viewModelScope.launch {
            val transport = when (device.type) {
                DeviceType.IR_ONLY -> irTransport
                else -> {
                    activeTvTransport?.disconnect()
                    AndroidTvTransport().also { activeTvTransport = it }
                }
            }

            when (val result = transport.connect(device)) {
                is ConnectionResult.Connected -> {
                    repository.saveDevice(device.copy(isPaired = true))
                    _uiState.update {
                        it.copy(connectionState = RemoteConnectionState.Connected, pairingHint = null)
                    }
                }
                is ConnectionResult.PairingRequired -> {
                    _uiState.update {
                        it.copy(
                            connectionState = RemoteConnectionState.PairingRequired(result.hint),
                            pairingHint = result.hint
                        )
                    }
                }
                is ConnectionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            connectionState = RemoteConnectionState.Error(result.message),
                            lastError = result.message
                        )
                    }
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            activeTvTransport?.disconnect()
            activeTvTransport = null
            _uiState.update {
                it.copy(connectionState = RemoteConnectionState.Disconnected, lastError = null)
            }
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────

    fun sendCommand(command: RemoteCommand) {
        val device = _uiState.value.selectedDevice ?: return
        if (_uiState.value.connectionState != RemoteConnectionState.Connected) return

        viewModelScope.launch {
            val transport = when (device.type) {
                DeviceType.IR_ONLY -> irTransport
                else -> activeTvTransport ?: return@launch
            }
            when (val result = transport.sendCommand(command)) {
                is CommandResult.Sent -> {}
                is CommandResult.Error -> {
                    _uiState.update { it.copy(lastError = result.message) }
                }
            }
        }
    }

    // ── Device management ─────────────────────────────────────────────────

    fun addManualDevice(host: String, port: Int, name: String) {
        if (host.isBlank()) return
        val realPort = if (port <= 0) DEFAULT_ATV_PORT else port
        val device = RemoteDevice(
            id = RemoteDevice.manualId(host, realPort),
            name = name.ifBlank { host },
            host = host.trim(),
            port = realPort,
            type = DeviceType.ANDROID_TV
        )
        viewModelScope.launch { repository.saveDevice(device) }
        _uiState.update { it.copy(selectedDevice = device) }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            repository.removeDevice(deviceId)
            if (_uiState.value.selectedDevice?.id == deviceId) {
                disconnect()
                _uiState.update { it.copy(selectedDevice = null) }
            }
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renameDevice(deviceId, newName) }
    }

    fun clearError() {
        _uiState.update { it.copy(lastError = null) }
    }

    override fun onCleared() {
        discoveryJob?.cancel()
        viewModelScope.launch { activeTvTransport?.disconnect() }
        super.onCleared()
    }

    companion object {
        const val DEFAULT_ATV_PORT = 6466
    }
}
