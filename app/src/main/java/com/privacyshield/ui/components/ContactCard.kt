package com.privacyshield.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyshield.bothub.data.model.ContactEntry
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary

@Composable
fun ContactCard(
    contact: ContactEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = BorderStroke(1.dp, OutlineDark),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(initials = contact.initials, name = contact.name)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (contact.alias.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "@${contact.alias}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (contact.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        contact.tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccent.copy(alpha = 0.08f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = CyanAccent.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                if (contact.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = contact.notes,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (contact.isArchived) "Unarchive" else "Archive") },
                        leadingIcon = {
                            Icon(
                                if (contact.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                null
                            )
                        },
                        onClick = { menuExpanded = false; onArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFFF5722)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5722)) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}
