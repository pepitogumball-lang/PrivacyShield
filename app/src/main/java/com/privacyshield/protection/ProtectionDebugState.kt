package com.privacyshield.protection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProtectionDebugSnapshot(
    val foregroundPackage: String = "-",
    val recordingRisk: Boolean = false,
    val overlayActive: Boolean = false,
    val reason: String = "idle",
    val recordingDetectedAtMs: Long? = null,
    val protectedDetectedAtMs: Long? = null,
    val overlayShownAtMs: Long? = null
)

object ProtectionDebugState {
    private val _state = MutableStateFlow(ProtectionDebugSnapshot())
    val state = _state.asStateFlow()

    fun update(transform: (ProtectionDebugSnapshot) -> ProtectionDebugSnapshot) {
        _state.value = transform(_state.value)
    }
}
