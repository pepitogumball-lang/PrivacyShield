package com.privacyshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.privacyshield.bothub.data.model.BotPersona
import com.privacyshield.bothub.data.model.ContactEntry
import com.privacyshield.bothub.data.model.Conversation
import com.privacyshield.bothub.viewmodel.BotHubTab
import com.privacyshield.bothub.viewmodel.BotHubViewModel
import com.privacyshield.ui.components.BotCard
import com.privacyshield.ui.components.ContactCard
import com.privacyshield.ui.components.InitialsAvatar
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotHubScreen(
    navController: NavController,
    viewModel: BotHubViewModel
) {
    val state by viewModel.state.collectAsState()
    val filteredPersonas by viewModel.filteredPersonas.collectAsState()
    val filteredContacts by viewModel.filteredContacts.collectAsState()
    val filteredConversations by viewModel.filteredConversations.collectAsState()
    var showNewConvoDialog by remember { mutableStateOf(false) }
    val currentTab = state.activeTab

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column(modifier = Modifier.background(SurfaceDark)) {
                TopAppBar(
                    title = {
                        Text("Bot Hub", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                )
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search…", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = OutlineDark,
                        cursorColor = CyanAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    containerColor = SurfaceDark,
                    contentColor = CyanAccent
                ) {
                    BotHubTab.values().forEach { tab ->
                        Tab(
                            selected = currentTab == tab,
                            onClick = { viewModel.setActiveTab(tab) },
                            text = {
                                Text(
                                    text = tab.displayLabel,
                                    color = if (currentTab == tab) CyanAccent else TextSecondary
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (currentTab) {
                        BotHubTab.PERSONAS -> navController.navigate("persona_editor/new")
                        BotHubTab.CONTACTS -> navController.navigate("contact_editor/new")
                        BotHubTab.CONVERSATIONS -> {
                            if (state.personas.isNotEmpty() && state.contacts.isNotEmpty()) {
                                showNewConvoDialog = true
                            }
                        }
                    }
                },
                containerColor = CyanAccent,
                contentColor = Color(0xFF0D0D0D)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        when (currentTab) {
            BotHubTab.PERSONAS -> PersonasContent(
                personas = filteredPersonas,
                modifier = Modifier.padding(padding),
                onClick = { navController.navigate("persona_editor/${it.id}") },
                onDuplicate = { viewModel.duplicatePersona(it) },
                onDelete = { viewModel.deletePersona(it.id) }
            )

            BotHubTab.CONTACTS -> ContactsContent(
                contacts = filteredContacts,
                modifier = Modifier.padding(padding),
                onClick = { navController.navigate("contact_editor/${it.id}") },
                onDelete = { viewModel.deleteContact(it.id) },
                onArchive = { viewModel.archiveContact(it.id, !it.isArchived) }
            )

            BotHubTab.CONVERSATIONS -> ConversationsContent(
                conversations = filteredConversations,
                personas = state.personas,
                contacts = state.contacts,
                modifier = Modifier.padding(padding),
                onClick = { navController.navigate("chat/${it.id}") },
                onDelete = { viewModel.deleteConversation(it.id) },
                onArchive = { viewModel.archiveConversation(it.id, true) }
            )
        }
    }

    if (showNewConvoDialog) {
        NewConversationDialog(
            personas = state.personas,
            contacts = state.contacts,
            onConfirm = { personaId, contactId ->
                val id = viewModel.createConversation(personaId, contactId)
                showNewConvoDialog = false
                navController.navigate("chat/$id")
            },
            onDismiss = { showNewConvoDialog = false }
        )
    }
}

@Composable
private fun PersonasContent(
    personas: List<BotPersona>,
    modifier: Modifier,
    onClick: (BotPersona) -> Unit,
    onDuplicate: (BotPersona) -> Unit,
    onDelete: (BotPersona) -> Unit
) {
    if (personas.isEmpty()) {
        HubEmptyState("No personas yet", "Tap + to create your first bot persona", modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(personas, key = { it.id }) { persona ->
                BotCard(
                    persona = persona,
                    onClick = { onClick(persona) },
                    onDuplicate = { onDuplicate(persona) },
                    onDelete = { onDelete(persona) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ContactsContent(
    contacts: List<ContactEntry>,
    modifier: Modifier,
    onClick: (ContactEntry) -> Unit,
    onDelete: (ContactEntry) -> Unit,
    onArchive: (ContactEntry) -> Unit
) {
    if (contacts.isEmpty()) {
        HubEmptyState("No contacts yet", "Tap + to add your first contact", modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(contacts, key = { it.id }) { contact ->
                ContactCard(
                    contact = contact,
                    onClick = { onClick(contact) },
                    onDelete = { onDelete(contact) },
                    onArchive = { onArchive(contact) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ConversationsContent(
    conversations: List<Conversation>,
    personas: List<BotPersona>,
    contacts: List<ContactEntry>,
    modifier: Modifier,
    onClick: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
    onArchive: (Conversation) -> Unit
) {
    if (conversations.isEmpty()) {
        HubEmptyState(
            "No conversations yet",
            "Create a persona and a contact first, then tap + to start a chat",
            modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(conversations, key = { it.id }) { convo ->
                val personaName = personas.find { it.id == convo.personaId }?.name ?: "Unknown Bot"
                val contactName = contacts.find { it.id == convo.contactId }?.name ?: "Unknown Contact"
                ConversationListCard(
                    conversation = convo,
                    personaName = personaName,
                    contactName = contactName,
                    onClick = { onClick(convo) },
                    onDelete = { onDelete(convo) },
                    onArchive = { onArchive(convo) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ConversationListCard(
    conversation: Conversation,
    personaName: String,
    contactName: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(initials = contactName.take(1), name = contactName)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contactName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(" · $personaName", color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (conversation.lastMessagePreview.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = conversation.lastMessagePreview,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (conversation.lastMessageAt > 0L) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = relativeTime(conversation.lastMessageAt),
                        color = TextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = TextSecondary)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        leadingIcon = { Icon(Icons.Default.Forum, null) },
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

@Composable
private fun HubEmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Chat, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NewConversationDialog(
    personas: List<BotPersona>,
    contacts: List<ContactEntry>,
    onConfirm: (personaId: String, contactId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPersonaId by remember { mutableStateOf("") }
    var selectedContactId by remember { mutableStateOf("") }
    var personaExpanded by remember { mutableStateOf(false) }
    var contactExpanded by remember { mutableStateOf(false) }

    val selectedPersonaName = personas.find { it.id == selectedPersonaId }?.name ?: "Select persona…"
    val selectedContactName = contacts.find { it.id == selectedContactId }?.name ?: "Select contact…"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("New Conversation", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Persona", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Box {
                    OutlinedTextField(
                        value = selectedPersonaName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = OutlineDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { personaExpanded = true })
                    DropdownMenu(
                        expanded = personaExpanded,
                        onDismissRequest = { personaExpanded = false }
                    ) {
                        personas.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = { selectedPersonaId = p.id; personaExpanded = false }
                            )
                        }
                    }
                }
                Text("Contact", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Box {
                    OutlinedTextField(
                        value = selectedContactName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = OutlineDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { contactExpanded = true })
                    DropdownMenu(
                        expanded = contactExpanded,
                        onDismissRequest = { contactExpanded = false }
                    ) {
                        contacts.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = { selectedContactId = c.id; contactExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedPersonaId.isNotBlank() && selectedContactId.isNotBlank()) {
                        onConfirm(selectedPersonaId, selectedContactId)
                    }
                },
                enabled = selectedPersonaId.isNotBlank() && selectedContactId.isNotBlank()
            ) { Text("Create", color = CyanAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

private val BotHubTab.displayLabel: String
    get() = when (this) {
        BotHubTab.PERSONAS -> "Personas"
        BotHubTab.CONTACTS -> "Contacts"
        BotHubTab.CONVERSATIONS -> "Chats"
    }

private fun relativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
    }
}
