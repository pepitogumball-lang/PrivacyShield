package com.privacyshield.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.ProtectedBlue
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextSecondary

@Composable
fun AppCard(
    app: InstalledAppInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val borderColor = if (app.isProtected) ProtectedBlue.copy(alpha = 0.6f) else OutlineDark

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isProtected) SurfaceVariantDark else SurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (app.isProtected) 1.dp else 0.5.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.width(8.dp))
                RiskBadge(riskLevel = app.riskLevel)
            }

            if (app.hasAccessibilityService || app.hasOverlayPermission ||
                app.hasRecordingRisk || app.isProtected
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (app.isProtected) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protected",
                            tint = ProtectedBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (app.hasAccessibilityService) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = "Accessibility",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (app.hasOverlayPermission) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Overlay",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (app.hasRecordingRisk) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Recording risk",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
