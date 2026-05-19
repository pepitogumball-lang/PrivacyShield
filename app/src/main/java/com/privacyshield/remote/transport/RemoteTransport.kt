package com.privacyshield.remote.transport

import com.privacyshield.remote.model.RemoteCommand
import com.privacyshield.remote.model.RemoteDevice

interface RemoteTransport {
    val isConnected: Boolean
    suspend fun connect(device: RemoteDevice): ConnectionResult
    suspend fun sendCommand(command: RemoteCommand): CommandResult
    suspend fun disconnect()
}

sealed class ConnectionResult {
    object Connected : ConnectionResult()
    data class PairingRequired(val hint: String = "") : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}

sealed class CommandResult {
    object Sent : CommandResult()
    data class Error(val message: String) : CommandResult()
}
