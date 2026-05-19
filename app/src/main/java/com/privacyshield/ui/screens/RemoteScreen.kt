package com.privacyshield.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacyshield.remote.model.DeviceType
import com.privacyshield.remote.model.RemoteDevice
import com.privacyshield.remote.viewmodel.DiscoveryState
import com.privacyshield.remote.viewmodel.RemoteConnectionState
import com.privacyshield.remote.viewmodel.RemoteViewModel
import com.privacyshield.ui.components.ConnectionStatusChip
import com.privacyshield.ui.components.RemoteDeviceCard
import com.privacyshield.ui.components.RemotePad
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.RiskHigh
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showDeviceList by remember { mutableStateOf(true) }

    // Surface errors via snackbar
    LaunchedEffect(uiState.lastError) {
        val err = uiState.lastError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        viewModel.clearError()
    }

    if (showAddDeviceDialog) {
        AddDeviceDialog(
            onConfirm = { host, port, name ->
                viewModel.addManualDevice(host, port, name)
                showAddDeviceDialog = false
            },
            onDismiss = { showAddDeviceDialog = false }
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "TV Remote",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDeviceDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add device manually", tint = CyanAccent)
                    }
                    val isScanning = uiState.discoveryState == DiscoveryState.Scanning
                    IconButton(
                        onClick = {
                            if (isScanning) viewModel.stopDiscovery() else viewModel.startDiscovery()
                        }
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan for TVs", tint = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceVariantDark,
                    contentColor = RiskHigh
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Connection banner ───────────────────────────────────────
            item {
                ConnectionBanner(
                    selectedDevice = uiState.selectedDevice,
                    connectionState = uiState.connectionState,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() }
                )
            }

            // ── Pairing hint ────────────────────────────────────────────
            val hint = uiState.pairingHint
            if (hint != null && uiState.connectionState is RemoteConnectionState.PairingRequired) {
                item {
                    PairingBanner(hint = hint, onRetry = { viewModel.connect() })
                }
            }

            // ── Remote controls ─────────────────────────────────────────
            item {
                RemoteControlSection(
                    connected = uiState.connectionState == RemoteConnectionState.Connected,
                    onCommand = { viewModel.sendCommand(it) }
                )
            }

            // ── Device list header ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Devices",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    TextButton(onClick = { showDeviceList = !showDeviceList }) {
                        Text(
                            if (showDeviceList) "Hide" else "Show",
                            color = CyanAccent,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // ── Discovered + saved devices ──────────────────────────────
            if (showDeviceList) {
                val discoveredNotSaved = uiState.discoveredDevices.filter { d ->
                    savedDevices.none { it.id == d.id }
                }

                if (discoveredNotSaved.isNotEmpty()) {
                    item {
                        Text(
                            "Found on network",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    items(discoveredNotSaved, key = { it.id }) { device ->
                        RemoteDeviceCard(
                            device = device,
                            isSelected = uiState.selectedDevice?.id == device.id,
                            onSelect = { viewModel.selectDevice(device) },
                            onRemove = { viewModel.removeDevice(device.id) },
                            onRename = { viewModel.renameDevice(device.id, it) }
                        )
                    }
                }

                if (savedDevices.isNotEmpty()) {
                    item {
                        Text(
                            "Saved devices",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    items(savedDevices, key = { it.id }) { device ->
                        RemoteDeviceCard(
                            device = device,
                            isSelected = uiState.selectedDevice?.id == device.id,
                            onSelect = { viewModel.selectDevice(device) },
                            onRemove = { viewModel.removeDevice(device.id) },
                            onRename = { viewModel.renameDevice(device.id, it) }
                        )
                    }
                }

                if (discoveredNotSaved.isEmpty() && savedDevices.isEmpty()) {
                    item {
                        EmptyDeviceState(
                            isScanning = uiState.discoveryState == DiscoveryState.Scanning,
                            onScan = { viewModel.startDiscovery() },
                            onAddManual = { showAddDeviceDialog = true }
                        )
                    }
                }

                val discoveryErr = (uiState.discoveryState as? DiscoveryState.Error)?.message
                if (discoveryErr != null) {
                    item {
                        DiscoveryErrorBanner(message = discoveryErr)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Connection banner ─────────────────────────────────────────────────────

@Composable
private fun ConnectionBanner(
    selectedDevice: RemoteDevice?,
    connectionState: RemoteConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedDevice?.displayName ?: "No device selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedDevice != null) TextPrimary else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedDevice != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${selectedDevice.host}:${selectedDevice.port}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            ConnectionStatusChip(state = connectionState)
            Spacer(Modifier.width(10.dp))
            AnimatedContent(
                targetState = connectionState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "conn_btn"
            ) { state ->
                when (state) {
                    is RemoteConnectionState.Disconnected,
                    is RemoteConnectionState.Error -> {
                        FilledTonalButton(
                            onClick = onConnect,
                            enabled = selectedDevice != null,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Connect", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    is RemoteConnectionState.Connecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CyanAccent,
                            strokeWidth = 2.dp
                        )
                    }
                    is RemoteConnectionState.Connected -> {
                        OutlinedButton(
                            onClick = onDisconnect,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskHigh)
                        ) {
                            Text("Disconnect", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    is RemoteConnectionState.PairingRequired -> {
                        OutlinedButton(
                            onClick = onDisconnect,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// ── Pairing banner ────────────────────────────────────────────────────────

@Composable
private fun PairingBanner(hint: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CyanAccent.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Tv,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Pairing required",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ── Remote control section ────────────────────────────────────────────────

@Composable
private fun RemoteControlSection(
    connected: Boolean,
    onCommand: (com.privacyshield.remote.model.RemoteCommand) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!connected) {
                Text(
                    "Connect to a TV to enable controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            RemotePad(
                enabled = connected,
                onCommand = onCommand
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────

@Composable
private fun EmptyDeviceState(
    isScanning: Boolean,
    onScan: () -> Unit,
    onAddManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.WifiOff,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = if (isScanning) "Scanning for TVs…" else "No devices found",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Make sure your Android TV / Google TV is on the same Wi-Fi network.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (!isScanning) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onScan) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Network")
                }
                OutlinedButton(onClick = onAddManual) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add by IP")
                }
            }
        }
    }
}

// ── Discovery error banner ────────────────────────────────────────────────

@Composable
private fun DiscoveryErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = RiskHigh.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Discovery unavailable: $message",
            style = MaterialTheme.typography.bodySmall,
            color = RiskHigh.copy(alpha = 0.8f),
            modifier = Modifier.padding(12.dp)
        )
    }
}

// ── Add device dialog ─────────────────────────────────────────────────────

@Composable
private fun AddDeviceDialog(
    onConfirm: (host: String, port: Int, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf(RemoteViewModel.DEFAULT_ATV_PORT.toString()) }
    var name by remember { mutableStateOf("") }
    var hostError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Device Manually", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter the IP address of your Android TV / Google TV.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it; hostError = false },
                    label = { Text("IP address (e.g. 192.168.1.100)") },
                    singleLine = true,
                    isError = hostError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = if (hostError) {
                        { Text("Enter a valid IP address", color = RiskHigh) }
                    } else null
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port (default 6466)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display name (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (host.isBlank()) { hostError = true; return@TextButton }
                    val port = portText.toIntOrNull() ?: RemoteViewModel.DEFAULT_ATV_PORT
                    onConfirm(host.trim(), port, name.trim())
                }
            ) {
                Text("Add", color = CyanAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceVariantDark
    )
}
