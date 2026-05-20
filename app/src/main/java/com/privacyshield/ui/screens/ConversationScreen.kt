package com.privacyshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.privacyshield.bothub.viewmodel.BotHubViewModel
import com.privacyshield.ui.components.ChatBubble
import com.privacyshield.ui.components.TypingIndicator
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    navController: NavController,
    viewModel: BotHubViewModel
) {
    val convoState by viewModel.convoState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var sendAsBot by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.openConversation(conversationId)
    }

    LaunchedEffect(convoState.messages.size) {
        if (convoState.messages.isNotEmpty()) {
            listState.animateScrollToItem(convoState.messages.size - 1)
        }
    }

    val contactName = convoState.contact?.name ?: "Chat"
    val personaName = convoState.persona?.name ?: ""

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(contactName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        if (personaName.isNotBlank()) {
                            Text(
                                "via $personaName",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setAutoReply(!convoState.autoReplyEnabled) }) {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = "Auto-reply",
                            tint = if (convoState.autoReplyEnabled) CyanAccent else TextSecondary
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = { Icon(Icons.Default.Forum, null) },
                                onClick = {
                                    viewModel.archiveConversation(conversationId, true)
                                    showMenu = false
                                    navController.popBackStack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color(0xFFFF5722)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5722)) },
                                onClick = {
                                    viewModel.deleteConversation(conversationId)
                                    showMenu = false
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        when {
            convoState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanAccent)
                }
            }

            convoState.conversation == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Conversation not found", color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Please go back and try again.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                ) {
                    // Messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }

                        if (convoState.messages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "No messages yet",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Type a message below to start the conversation",
                                            color = TextSecondary.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (convoState.autoReplyEnabled) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Auto-reply is ON · ${convoState.persona?.name} will respond",
                                                color = CyanAccent.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(convoState.messages, key = { it.id }) { message ->
                                ChatBubble(message = message)
                            }
                        }

                        item {
                            TypingIndicator(
                                personaName = personaName,
                                visible = convoState.isTyping,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    // Input area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                    ) {
                        // Auto-reply status bar
                        if (convoState.autoReplyEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyanAccent.copy(alpha = 0.06f))
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Auto-reply active · ${convoState.persona?.replySpeed?.label ?: ""} speed",
                                    color = CyanAccent,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Tap 🤖 to disable",
                                    color = CyanAccent.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { sendAsBot = !sendAsBot }) {
                                Icon(
                                    imageVector = if (sendAsBot) Icons.Default.Android else Icons.Default.Person,
                                    contentDescription = if (sendAsBot) "Sending as Bot" else "Sending as Contact",
                                    tint = if (sendAsBot) CyanAccent else TextSecondary
                                )
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        if (sendAsBot) "Bot says…" else "Contact says…",
                                        color = TextSecondary.copy(alpha = 0.6f)
                                    )
                                },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = OutlineDark,
                                    cursorColor = CyanAccent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        if (sendAsBot) {
                                            viewModel.sendBotMessage(inputText)
                                        } else {
                                            viewModel.sendContactMessage(inputText)
                                        }
                                        inputText = ""
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) CyanAccent else TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
