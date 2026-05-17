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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacyshield.data.AppUiState
import com.privacyshield.data.AppViewModel
import com.privacyshield.data.model.RiskLevel
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.ProtectedBlue
import com.privacyshield.ui.theme.RiskCritical
import com.privacyshield.ui.theme.RiskHigh
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PrivacyShield",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        )
                        Text(
                            text = "Privacy Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                actions = {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = CyanAccent,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(28.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScanStatusBar(state = state, onScanClick = viewModel::scanApps) }
            item { DashboardGrid(state = state) }
        }
    }
}

@Composable
private fun ScanStatusBar(state: AppUiState, onScanClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, OutlineDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (state.isScanning) "Scanning…" else "Last scan",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                val timeLabel = state.lastScanTime?.let {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                } ?: "Never"
                Text(
                    text = if (state.isScanning) "" else timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = CyanAccent,
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "  Scan Now",
                        style = MaterialTheme.typography.labelLarge,
                        color = BackgroundDark
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardGrid(state: AppUiState) {
    val apps = state.apps
    val totalApps = apps.size
    val dangerousCount = apps.count { it.hasDangerousPermissions }
    val accessibilityCount = apps.count { it.hasAccessibilityService }
    val overlayCount = apps.count { it.hasOverlayPermission }
    val recordingCount = apps.count { it.hasRecordingRisk }
    val protectedCount = apps.count { it.isProtected }
    val criticalCount = apps.count { it.riskLevel == RiskLevel.CRITICAL }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Top summary card
        StatCard(
            icon = Icons.Default.Apps,
            label = "Total Apps Scanned",
            value = totalApps.toString(),
            accent = CyanAccent,
            modifier = Modifier.fillMaxWidth()
        )

        // 2-column grid
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Default.Warning,
                label = "Dangerous\nPermissions",
                value = dangerousCount.toString(),
                accent = if (dangerousCount > 0) RiskHigh else CyanAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Shield,
                label = "Critical\nRisk Apps",
                value = criticalCount.toString(),
                accent = if (criticalCount > 0) RiskCritical else CyanAccent,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Default.Accessibility,
                label = "Accessibility\nServices",
                value = accessibilityCount.toString(),
                accent = if (accessibilityCount > 0) RiskHigh else CyanAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Layers,
                label = "Overlay\nPermission",
                value = overlayCount.toString(),
                accent = if (overlayCount > 0) RiskHigh else CyanAccent,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Default.Videocam,
                label = "Recording\nRisk",
                value = recordingCount.toString(),
                accent = if (recordingCount > 0) RiskHigh else CyanAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Lock,
                label = "Protected\nApps",
                value = protectedCount.toString(),
                accent = ProtectedBlue,
                modifier = Modifier.weight(1f)
            )
        }
        if (state.error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scan error: ${state.error}",
                style = MaterialTheme.typography.bodySmall,
                color = RiskHigh
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, OutlineDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
