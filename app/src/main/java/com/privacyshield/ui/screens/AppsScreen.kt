package com.privacyshield.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacyshield.data.AppViewModel
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.data.model.RiskLevel
import com.privacyshield.ui.components.AppCard
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextSecondary

enum class AppFilter(val label: String) {
    ALL("All"),
    HIGH_RISK("High Risk"),
    ACCESSIBILITY("Accessibility"),
    OVERLAY("Overlay"),
    CAMERA_MIC("Camera / Mic"),
    RECORDING("Recording Risk")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppsScreen(
    viewModel: AppViewModel = viewModel(),
    onAppClick: (InstalledAppInfo) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(AppFilter.ALL) }

    val filtered = remember(state.apps, query, activeFilter) {
        state.apps
            .filter { app ->
                val matchesQuery = query.isBlank() ||
                        app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
                val matchesFilter = when (activeFilter) {
                    AppFilter.ALL -> true
                    AppFilter.HIGH_RISK -> app.riskLevel == RiskLevel.HIGH || app.riskLevel == RiskLevel.CRITICAL
                    AppFilter.ACCESSIBILITY -> app.hasAccessibilityService
                    AppFilter.OVERLAY -> app.hasOverlayPermission
                    AppFilter.CAMERA_MIC -> app.permissions.any { p ->
                        p.contains("CAMERA", ignoreCase = true) || p.contains("RECORD_AUDIO", ignoreCase = true)
                    }
                    AppFilter.RECORDING -> app.hasRecordingRisk
                }
                matchesQuery && matchesFilter
            }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Installed Apps",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text("Search by name or package…", color = TextSecondary)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = OutlineDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = CyanAccent
                )
            )

            FlowRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = filter == activeFilter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent.copy(alpha = 0.15f),
                            selectedLabelColor = CyanAccent,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filter == activeFilter,
                            borderColor = OutlineDark,
                            selectedBorderColor = CyanAccent.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    text = "No apps match your filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppCard(app = app, onClick = { onAppClick(app) })
                    }
                }
            }
        }
    }
}
