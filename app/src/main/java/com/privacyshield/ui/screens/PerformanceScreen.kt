package com.privacyshield.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacyshield.data.AppUiState
import com.privacyshield.data.AppViewModel
import com.privacyshield.data.ScanStats
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.ui.components.SectionHeader
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.ProtectedBlue
import com.privacyshield.ui.theme.RiskHigh
import com.privacyshield.ui.theme.RiskLow
import com.privacyshield.ui.theme.RiskMedium
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextSecondary
import com.privacyshield.util.PerformanceMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scanStats by viewModel.scanStats.collectAsState()
    val performanceMode by viewModel.performanceMode.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Performance Center",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Optimize & verify protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                actions = {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.padding(end = 16.dp).size(26.dp)
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
                SectionHeader(title = "Performance Mode")
                PerformanceModeSelector(
                    current = performanceMode,
                    onSelect = viewModel::setPerformanceMode
                )
            }

            item {
                SectionHeader(title = "Scan Statistics")
                ScanStatsCard(stats = scanStats, isScanning = state.isScanning)
            }

            item {
                SectionHeader(title = "Protection Verification")
                ProtectionVerificationCard(apps = state.apps)
            }

            item {
                SectionHeader(title = "Quick Actions")
                QuickActionsCard(
                    onClearCache = viewModel::clearIconCache,
                    onForceRescan = { viewModel.scanApps(force = true) },
                    isScanning = state.isScanning
                )
            }

            item {
                SectionHeader(title = "System Note")
                SystemNoteCard()
            }
        }
    }
}

@Composable
private fun PerformanceModeSelector(
    current: PerformanceMode,
    onSelect: (PerformanceMode) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PerformanceMode.entries.forEach { mode ->
            val isSelected = mode == current
            val accent = when (mode) {
                PerformanceMode.LIGHT -> RiskMedium
                PerformanceMode.BALANCED -> CyanAccent
                PerformanceMode.MAXIMUM -> RiskHigh
            }
            Card(
                onClick = { onSelect(mode) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SurfaceVariantDark else SurfaceDark
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.dp else 0.5.dp,
                    color = if (isSelected) accent else OutlineDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    } else {
                        Spacer(modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accent else TextSecondary
                            )
                        )
                        Text(
                            text = mode.subtitle,
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
private fun ScanStatsCard(stats: ScanStats, isScanning: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(0.5.dp, OutlineDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(
                label = "Apps",
                value = if (isScanning) "…" else stats.totalApps.toString(),
                accent = CyanAccent
            )
            StatItem(
                label = "Duration",
                value = if (isScanning) "…" else "${stats.lastDurationMs} ms",
                accent = RiskMedium
            )
            StatItem(
                label = "Icons cached",
                value = stats.iconCacheSize.toString(),
                accent = ProtectedBlue
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = accent
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ProtectionVerificationCard(apps: List<InstalledAppInfo>) {
    val protectedApps = apps.filter { it.isProtected }
    val riskyApps = apps.filter { it.hasAccessibilityService || it.hasOverlayPermission }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(0.5.dp, OutlineDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (protectedApps.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "No protected apps set. Add apps in the Protected tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                protectedApps.forEachIndexed { index, app ->
                    val riskyNeighbors = riskyApps.filter { it.packageName != app.packageName }
                    ProtectionRow(app = app, riskyNeighborCount = riskyNeighbors.size)
                    if (index < protectedApps.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = OutlineDark)
                    }
                }

                if (riskyApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = OutlineDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = RiskMedium,
                            modifier = Modifier.size(14.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = "${riskyApps.size} app(s) with accessibility or overlay permissions are co-installed. " +
                                    "These could potentially interact with your protected apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RiskMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = OutlineDark)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Text(
                    text = "Protection is advisory. Android does not grant regular apps enforcement " +
                            "powers over other apps. PrivacyShield surfaces risk information so you can act.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ProtectionRow(app: InstalledAppInfo, riskyNeighborCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (riskyNeighborCount == 0) Icons.Default.Shield else Icons.Default.Warning,
            contentDescription = null,
            tint = if (riskyNeighborCount == 0) RiskLow else RiskMedium,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.appName, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
            Text(
                text = if (riskyNeighborCount == 0) "Protected — no risky co-installed apps detected"
                else "$riskyNeighborCount risky co-installed app(s) detected",
                style = MaterialTheme.typography.bodySmall,
                color = if (riskyNeighborCount == 0) RiskLow else RiskMedium
            )
        }
        Icon(Icons.Default.Lock, contentDescription = null, tint = ProtectedBlue, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun QuickActionsCard(
    onClearCache: () -> Unit,
    onForceRescan: () -> Unit,
    isScanning: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(0.5.dp, OutlineDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(0.5.dp, OutlineDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Icons", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onForceRescan,
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = BackgroundDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Scanning…" else "Force Scan",
                        style = MaterialTheme.typography.labelLarge,
                        color = BackgroundDark
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemNoteCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(0.5.dp, OutlineDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = ProtectedBlue, modifier = Modifier.size(16.dp))
            Text(
                text = "Android does not expose APIs that allow regular apps to control the frame rate " +
                        "or CPU governor of other apps. Performance modes in PrivacyShield control " +
                        "how aggressively this app scans, caches, and loads resources — reducing its own " +
                        "resource use so your device runs smoother.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
