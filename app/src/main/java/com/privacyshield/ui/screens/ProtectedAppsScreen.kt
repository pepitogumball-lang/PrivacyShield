package com.privacyshield.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacyshield.R
import com.privacyshield.data.AppViewModel
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.ui.components.EmptyState
import com.privacyshield.ui.components.RiskBadge
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.ProtectedBlue
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedAppsScreen(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    val protectedApps = state.apps.filter { it.isProtected }
    val unprotectedApps = state.apps.filter { !it.isProtected && !it.isSystemApp }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Protected Apps",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Mark apps as sensitive",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                actions = {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = ProtectedBlue,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(26.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                InfoBanner()
            }

            if (protectedApps.isNotEmpty()) {
                item {
                    Text(
                        text = "PROTECTED (${protectedApps.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ProtectedBlue
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(protectedApps, key = { "p_${it.packageName}" }) { app ->
                    ProtectedAppRow(
                        app = app,
                        isProtected = true,
                        onToggle = { viewModel.removeProtectedApp(app.packageName) }
                    )
                }
            }

            if (unprotectedApps.isNotEmpty()) {
                item {
                    Text(
                        text = "ALL USER APPS (${unprotectedApps.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(unprotectedApps, key = { "u_${it.packageName}" }) { app ->
                    ProtectedAppRow(
                        app = app,
                        isProtected = false,
                        onToggle = { viewModel.addProtectedApp(app.packageName) }
                    )
                }
            }

            if (protectedApps.isEmpty() && unprotectedApps.isEmpty()) {
                item {
                    EmptyState(
                        imageRes = R.drawable.empty_no_protected,
                        title = "No apps found",
                        subtitle = "Run a scan first to see installed apps here."
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProtectedBlue.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, ProtectedBlue.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = ProtectedBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Protected apps are flagged as sensitive. PrivacyShield highlights other apps that may interact with them.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ProtectedAppRow(
    app: InstalledAppInfo,
    isProtected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isProtected) SurfaceVariantDark else SurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isProtected) 1.dp else 0.5.dp,
            color = if (isProtected) ProtectedBlue.copy(alpha = 0.6f) else OutlineDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiskBadge(riskLevel = app.riskLevel)
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isProtected) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isProtected) "Remove protection" else "Add protection",
                        tint = if (isProtected) ProtectedBlue else CyanAccent
                    )
                }
            }
        }
    }
}
