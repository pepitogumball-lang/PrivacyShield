package com.privacyshield.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.privacyshield.bothub.data.model.BotPersona
import com.privacyshield.bothub.data.model.EmojiUsage
import com.privacyshield.bothub.data.model.PersonaFormality
import com.privacyshield.bothub.data.model.PersonaTone
import com.privacyshield.bothub.data.model.ReplyRule
import com.privacyshield.bothub.data.model.ReplySpeed
import com.privacyshield.bothub.viewmodel.BotHubViewModel
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotPersonaEditorScreen(
    personaId: String,
    navController: NavController,
    viewModel: BotHubViewModel
) {
    val state by viewModel.state.collectAsState()
    val isNew = personaId == "new"
    val existingPersona = if (isNew) null else state.personas.find { it.id == personaId }
    val existingRules = if (isNew) emptyList() else state.replyRulesMap[personaId] ?: emptyList()

    var name by remember(personaId) { mutableStateOf(existingPersona?.name ?: "") }
    var bio by remember(personaId) { mutableStateOf(existingPersona?.bio ?: "") }
    var tone by remember(personaId) { mutableStateOf(existingPersona?.tone ?: PersonaTone.NEUTRAL) }
    var formality by remember(personaId) { mutableStateOf(existingPersona?.formality ?: PersonaFormality.CASUAL) }
    var emojiUsage by remember(personaId) { mutableStateOf(existingPersona?.emojiUsage ?: EmojiUsage.SPARSE) }
    var replySpeed by remember(personaId) { mutableStateOf(existingPersona?.replySpeed ?: ReplySpeed.NORMAL) }
    var autoReply by remember(personaId) { mutableStateOf(existingPersona?.autoReplyEnabled ?: false) }
    var localRules by remember(personaId) { mutableStateOf(existingRules) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    fun onSave() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val savedId = if (isNew) UUID.randomUUID().toString() else personaId
        val persona = BotPersona(
            id = savedId,
            name = name.trim(),
            bio = bio.trim(),
            avatarInitials = name.trim().split(" ").take(2).joinToString("") { it.take(1).uppercase() }.take(2),
            tone = tone,
            formality = formality,
            emojiUsage = emojiUsage,
            replySpeed = replySpeed,
            autoReplyEnabled = autoReply,
            createdAt = existingPersona?.createdAt ?: System.currentTimeMillis()
        )
        viewModel.savePersona(persona)
        viewModel.saveReplyRules(savedId, localRules)
        navController.popBackStack()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "New Persona" else "Edit Persona",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name *", color = TextSecondary) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name is required", color = Color(0xFFFF5722)) }
                    } else null,
                    singleLine = true,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Description", color = TextSecondary) },
                    minLines = 2,
                    maxLines = 4,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                ChipSelector(
                    label = "Tone",
                    options = PersonaTone.values().map { it.label },
                    selectedIndex = PersonaTone.values().indexOf(tone),
                    onSelect = { tone = PersonaTone.values()[it] }
                )
            }

            item {
                ChipSelector(
                    label = "Formality",
                    options = PersonaFormality.values().map { it.label },
                    selectedIndex = PersonaFormality.values().indexOf(formality),
                    onSelect = { formality = PersonaFormality.values()[it] }
                )
            }

            item {
                ChipSelector(
                    label = "Emoji Usage",
                    options = EmojiUsage.values().map { it.label },
                    selectedIndex = EmojiUsage.values().indexOf(emojiUsage),
                    onSelect = { emojiUsage = EmojiUsage.values()[it] }
                )
            }

            item {
                ChipSelector(
                    label = "Reply Speed",
                    options = ReplySpeed.values().map { it.label },
                    selectedIndex = ReplySpeed.values().indexOf(replySpeed),
                    onSelect = { replySpeed = ReplySpeed.values()[it] }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto-Reply", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("Bot replies automatically to contact messages", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = autoReply,
                        onCheckedChange = { autoReply = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanAccent,
                            checkedTrackColor = CyanAccent.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceVariantDark
                        )
                    )
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
            item { HorizontalDivider(color = OutlineDark) }
            item { Spacer(Modifier.height(12.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reply Rules", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showAddRuleDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = CyanAccent)
                        Text("Add Rule", color = CyanAccent)
                    }
                }
                Text("Define keyword triggers and bot responses", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
            }

            if (localRules.isEmpty()) {
                item {
                    Text(
                        "No rules yet. Add one to enable keyword-based replies.",
                        color = TextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(localRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onDelete = { localRules = localRules.filter { r -> r.id != rule.id } }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (!isNew) {
                item { Spacer(Modifier.height(24.dp)) }
                item { HorizontalDivider(color = OutlineDark) }
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5722))
                        Text("  Delete Persona", color = Color(0xFFFF5722))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            personaId = if (isNew) "new" else personaId,
            onConfirm = { rule ->
                localRules = localRules + rule
                showAddRuleDialog = false
            },
            onDismiss = { showAddRuleDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SurfaceDark,
            title = { Text("Delete Persona?", color = TextPrimary) },
            text = { Text("This will also remove all conversations and rules for this persona.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePersona(personaId)
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
private fun RuleCard(rule: ReplyRule, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariantDark
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text("Trigger: ", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("\"${rule.triggerKeyword}\"", color = CyanAccent, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(2.dp))
                Text(rule.response, color = TextPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5722).copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AddRuleDialog(
    personaId: String,
    onConfirm: (ReplyRule) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Add Reply Rule", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Keyword trigger", color = TextSecondary) },
                    singleLine = true,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    label = { Text("Bot response", color = TextSecondary) },
                    minLines = 2,
                    maxLines = 4,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (keyword.isNotBlank() && response.isNotBlank()) {
                        onConfirm(ReplyRule(personaId = personaId, triggerKeyword = keyword.trim(), response = response.trim()))
                    }
                },
                enabled = keyword.isNotBlank() && response.isNotBlank()
            ) { Text("Add", color = CyanAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEachIndexed { i, opt ->
                FilterChip(
                    selected = i == selectedIndex,
                    onClick = { onSelect(i) },
                    label = { Text(opt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent.copy(alpha = 0.15f),
                        selectedLabelColor = CyanAccent,
                        containerColor = SurfaceVariantDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = i == selectedIndex,
                        borderColor = OutlineDark,
                        selectedBorderColor = CyanAccent.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = OutlineDark,
    cursorColor = CyanAccent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = TextSecondary
)
