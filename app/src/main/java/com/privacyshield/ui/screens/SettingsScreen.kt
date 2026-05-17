package com.privacyshield.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.privacyshield.ui.components.SectionHeader
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.ProtectedBlue
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    // Settings state — persisted via DataStore in a future iteration
    var scanAccessibility by remember { mutableStateOf(true) }
    var scanOverlay by remember { mutableStateOf(true) }
    var scanRecording by remember { mutableStateOf(true) }
    var scanDangerous by remember { mutableStateOf(true) }
    var notifyHighRisk by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                actions = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.padding(end = 16.dp).size(24.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SectionHeader(title = "Scan Options")
                SettingsCard {
                    ToggleRow(
                        icon = Icons.Default.Accessibility,
                        title = "Detect accessibility services",
                        subtitle = "Find apps registered as accessibility services.",
                        checked = scanAccessibility,
                        onCheckedChange = { scanAccessibility = it }
                    )
                    ToggleRow(
                        icon = Icons.Default.Layers,
                        title = "Detect overlay permission",
                        subtitle = "Flag apps that can draw over other apps.",
                        checked = scanOverlay,
                        onCheckedChange = { scanOverlay = it }
                    )
                    ToggleRow(
                        icon = Icons.Default.Videocam,
                        title = "Detect recording risk",
                        subtitle = "Identify apps with capture-related permissions.",
                        checked = scanRecording,
                        onCheckedChange = { scanRecording = it }
                    )
                    ToggleRow(
                        icon = Icons.Default.Warning,
                        title = "Detect dangerous permissions",
                        subtitle = "Flag apps requesting camera, mic, location, etc.",
                        checked = scanDangerous,
                        onCheckedChange = { scanDangerous = it },
                        showDivider = false
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(title = "Alerts")
                SettingsCard {
                    ToggleRow(
                        icon = Icons.Default.Notifications,
                        title = "Notify on high-risk apps",
                        subtitle = "Send a notification when a new high-risk app is found.",
                        checked = notifyHighRisk,
                        onCheckedChange = { notifyHighRisk = it },
                        showDivider = false
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(title = "Privacy Note")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, OutlineDark)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = ProtectedBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "PrivacyShield works fully offline. No data leaves your device. No analytics. No telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(title = "About")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, OutlineDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "com.privacyshield",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, OutlineDark)
    ) {
        Column { content() }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = CyanAccent,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = OutlineDark
                )
            )
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = OutlineDark
            )
        }
    }
}
