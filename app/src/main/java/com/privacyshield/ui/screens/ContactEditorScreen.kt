package com.privacyshield.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.privacyshield.bothub.data.model.ContactEntry
import com.privacyshield.bothub.viewmodel.BotHubViewModel
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditorScreen(
    contactId: String,
    navController: NavController,
    viewModel: BotHubViewModel
) {
    val state by viewModel.state.collectAsState()
    val isNew = contactId == "new"
    val existing = if (isNew) null else state.contacts.find { it.id == contactId }

    var name by remember(contactId) { mutableStateOf(existing?.name ?: "") }
    var alias by remember(contactId) { mutableStateOf(existing?.alias ?: "") }
    var notes by remember(contactId) { mutableStateOf(existing?.notes ?: "") }
    var tagsText by remember(contactId) { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun onSave() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val contact = ContactEntry(
            id = if (isNew) java.util.UUID.randomUUID().toString() else contactId,
            name = name.trim(),
            alias = alias.trim(),
            notes = notes.trim(),
            tags = tags,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        viewModel.saveContact(contact)
        navController.popBackStack()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "New Contact" else "Edit Contact",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onSave() }) {
                        Icon(Icons.Default.Check, "Save", tint = CyanAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Name *", color = TextSecondary) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Name is required", color = Color(0xFFFF5722)) }
                } else null,
                singleLine = true,
                colors = contactFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Alias / Username", color = TextSecondary) },
                placeholder = { Text("@handle", color = TextSecondary.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = contactFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("Tags", color = TextSecondary) },
                placeholder = { Text("work, vip, test  (comma separated)", color = TextSecondary.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = contactFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes", color = TextSecondary) },
                minLines = 3,
                maxLines = 6,
                colors = contactFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isNew) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = OutlineDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5722))
                        Text("  Delete Contact", color = Color(0xFFFF5722))
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SurfaceDark,
            title = { Text("Delete Contact?", color = TextPrimary) },
            text = { Text("This will also remove all conversations with this contact.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(contactId)
                    navController.popBackStack()
                }) { Text("Delete", color = Color(0xFFFF5722)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun contactFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = OutlineDark,
    cursorColor = CyanAccent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = TextSecondary
)
