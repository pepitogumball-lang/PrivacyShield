package com.privacyshield.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyshield.remote.model.RemoteDevice
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary

@Composable
fun RemoteDeviceCard(
    device: RemoteDevice,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenameDeviceDialog(
            currentName = device.displayName,
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) CyanAccent.copy(alpha = 0.7f) else OutlineDark
        ),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) SurfaceVariantDark else SurfaceDark
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = if (isSelected) CyanAccent else TextSecondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    color = if (isSelected) CyanAccent else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${device.host}:${device.port}  ·  ${device.type.label}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                if (device.isPaired) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Paired",
                        color = CyanAccent.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = isSelected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 54.dp, end = 14.dp, bottom = 10.dp)
            ) {
                FilledTonalButton(
                    onClick = onSelect,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        "Use this TV",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDeviceDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Device", color = TextPrimary) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Device name") },
                singleLine = true
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) {
                Text("Save", color = CyanAccent)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceVariantDark
    )
}
