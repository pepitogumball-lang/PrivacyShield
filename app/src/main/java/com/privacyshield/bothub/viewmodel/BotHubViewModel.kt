package com.privacyshield.bothub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.bothub.data.model.BotPersona
import com.privacyshield.bothub.data.model.ContactEntry
import com.privacyshield.bothub.data.model.Conversation
import com.privacyshield.bothub.data.model.MessageEntry
import com.privacyshield.bothub.data.model.ReplyRule
import com.privacyshield.bothub.data.model.SenderType
import com.privacyshield.bothub.domain.ReplyEngine
import com.privacyshield.bothub.repository.BotHubRepository
import com.privacyshield.bothub.storage.BotHubStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BotHubUiState(
    val personas: List<BotPersona> = emptyList(),
    val contacts: List<ContactEntry> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val replyRulesMap: Map<String, List<ReplyRule>> = emptyMap(),
    val activeTab: BotHubTab = BotHubTab.PERSONAS,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ConversationState(
    val conversation: Conversation? = null,
    val messages: List<MessageEntry> = emptyList(),
    val persona: BotPersona? = null,
    val contact: ContactEntry? = null,
    val isTyping: Boolean = false,
    val autoReplyEnabled: Boolean = false,
    val isLoading: Boolean = true
)

enum class BotHubTab { PERSONAS, CONTACTS, CONVERSATIONS }

class BotHubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BotHubRepository(BotHubStorage(application))

    private val _state = MutableStateFlow(BotHubUiState())
    val state: StateFlow<BotHubUiState> = _state.asStateFlow()

    private val _convoState = MutableStateFlow(ConversationState())
    val convoState: StateFlow<ConversationState> = _convoState.asStateFlow()

    val filteredPersonas: StateFlow<List<BotPersona>> = _state.map { s ->
        val q = s.searchQuery
        if (q.isBlank()) s.personas
        else s.personas.filter {
            it.name.contains(q, ignoreCase = true) || it.bio.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredContacts: StateFlow<List<ContactEntry>> = _state.map { s ->
        val q = s.searchQuery
        s.contacts.filter { !it.isArchived }.let { list ->
            if (q.isBlank()) list
            else list.filter {
                it.name.contains(q, ignoreCase = true) ||
                        it.alias.contains(q, ignoreCase = true) ||
                        it.tags.any { t -> t.contains(q, ignoreCase = true) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredConversations: StateFlow<List<Conversation>> = _state.map { s ->
        val q = s.searchQuery
        s.conversations.filter { !it.isArchived }
            .sortedByDescending { it.lastMessageAt }
            .let { list ->
                if (q.isBlank()) list
                else list.filter {
                    it.title.contains(q, ignoreCase = true) ||
                            it.lastMessagePreview.contains(q, ignoreCase = true)
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val personas = repository.getPersonas()
            val contacts = repository.getContacts()
            val conversations = repository.getConversations()
            val rules = personas.associate { p -> p.id to repository.getReplyRules(p.id) }
            _state.update {
                it.copy(
                    personas = personas,
                    contacts = contacts,
                    conversations = conversations,
                    replyRulesMap = rules,
                    isLoading = false
                )
            }
        }
    }

    // ── UI state ──────────────────────────────────────────────────────────

    fun setActiveTab(tab: BotHubTab) = _state.update { it.copy(activeTab = tab) }
    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    // ── Persona operations ────────────────────────────────────────────────

    fun savePersona(persona: BotPersona) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _state.value.personas.toMutableList()
            val idx = list.indexOfFirst { it.id == persona.id }
            val updated = if (idx >= 0) {
                list[idx] = persona.copy(updatedAt = System.currentTimeMillis()); list
            } else {
                list.add(0, persona); list
            }
            repository.savePersonas(updated)
            _state.update { it.copy(personas = updated) }
        }
    }

    fun deletePersona(personaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPersonas = _state.value.personas.filter { it.id != personaId }
            repository.savePersonas(updatedPersonas)
            repository.deletePersonaData(personaId)
            val convosToRemove = _state.value.conversations.filter { it.personaId == personaId }
            convosToRemove.forEach { repository.deleteConversationData(it.id) }
            val updatedConvos = _state.value.conversations.filter { it.personaId != personaId }
            repository.saveConversations(updatedConvos)
            _state.update {
                it.copy(
                    personas = updatedPersonas,
                    conversations = updatedConvos,
                    replyRulesMap = it.replyRulesMap - personaId
                )
            }
        }
    }

    fun duplicatePersona(persona: BotPersona) {
        savePersona(
            persona.copy(
                id = UUID.randomUUID().toString(),
                name = "${persona.name} (copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun saveReplyRules(personaId: String, rules: List<ReplyRule>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveReplyRules(personaId, rules)
            _state.update { it.copy(replyRulesMap = it.replyRulesMap + (personaId to rules)) }
        }
    }

    // ── Contact operations ────────────────────────────────────────────────

    fun saveContact(contact: ContactEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _state.value.contacts.toMutableList()
            val idx = list.indexOfFirst { it.id == contact.id }
            val updated = if (idx >= 0) {
                list[idx] = contact.copy(updatedAt = System.currentTimeMillis()); list
            } else {
                list.add(0, contact); list
            }
            repository.saveContacts(updated)
            _state.update { it.copy(contacts = updated) }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedContacts = _state.value.contacts.filter { it.id != contactId }
            repository.saveContacts(updatedContacts)
            val convosToRemove = _state.value.conversations.filter { it.contactId == contactId }
            convosToRemove.forEach { repository.deleteConversationData(it.id) }
            val updatedConvos = _state.value.conversations.filter { it.contactId != contactId }
            repository.saveConversations(updatedConvos)
            _state.update { it.copy(contacts = updatedContacts, conversations = updatedConvos) }
        }
    }

    fun archiveContact(contactId: String, archived: Boolean) {
        val contact = _state.value.contacts.find { it.id == contactId } ?: return
        saveContact(contact.copy(isArchived = archived))
    }

    // ── Conversation operations ───────────────────────────────────────────

    fun createConversation(personaId: String, contactId: String): String {
        val convo = Conversation(personaId = personaId, contactId = contactId)
        // Update state immediately so openConversation() finds it right away
        val updated = listOf(convo) + _state.value.conversations
        _state.update { it.copy(conversations = updated) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveConversations(updated)
        }
        return convo.id
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _state.value.conversations.filter { it.id != conversationId }
            repository.saveConversations(updated)
            repository.deleteConversationData(conversationId)
            _state.update { it.copy(conversations = updated) }
        }
    }

    fun archiveConversation(conversationId: String, archived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _state.value.conversations.map {
                if (it.id == conversationId) it.copy(isArchived = archived) else it
            }
            repository.saveConversations(updated)
            _state.update { it.copy(conversations = updated) }
        }
    }

    // ── Open conversation (chat screen) ───────────────────────────────────

    fun openConversation(conversationId: String) {
        _convoState.value = ConversationState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value
            val convo = s.conversations.firstOrNull { it.id == conversationId }
            val persona = convo?.personaId?.let { id -> s.personas.firstOrNull { it.id == id } }
            val contact = convo?.contactId?.let { id -> s.contacts.firstOrNull { it.id == id } }
            val messages = repository.getMessages(conversationId)
            _convoState.value = ConversationState(
                conversation = convo,
                messages = messages,
                persona = persona,
                contact = contact,
                isLoading = false,
                autoReplyEnabled = persona?.autoReplyEnabled ?: false
            )
        }
    }

    fun setAutoReply(enabled: Boolean) = _convoState.update { it.copy(autoReplyEnabled = enabled) }

    // ── Messaging ─────────────────────────────────────────────────────────

    fun sendContactMessage(content: String) {
        val state = _convoState.value
        val conversationId = state.conversation?.id ?: return
        if (content.isBlank()) return
        val message = MessageEntry(
            conversationId = conversationId,
            content = content.trim(),
            senderType = SenderType.CONTACT
        )
        appendMessage(message, conversationId)
        if (state.autoReplyEnabled) {
            val persona = state.persona ?: return
            triggerAutoReply(conversationId, message, persona)
        }
    }

    fun sendBotMessage(content: String) {
        val state = _convoState.value
        val conversationId = state.conversation?.id ?: return
        if (content.isBlank()) return
        val message = MessageEntry(
            conversationId = conversationId,
            content = content.trim(),
            senderType = SenderType.BOT
        )
        appendMessage(message, conversationId)
    }

    private fun appendMessage(message: MessageEntry, conversationId: String) {
        _convoState.update { it.copy(messages = it.messages + message) }
        val messages = _convoState.value.messages
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMessages(conversationId, messages)
            val updated = _state.value.conversations.map { c ->
                if (c.id == conversationId) c.copy(
                    lastMessagePreview = message.content.take(80),
                    lastMessageAt = message.timestamp,
                    messageCount = messages.size
                ) else c
            }
            repository.saveConversations(updated)
            _state.update { it.copy(conversations = updated) }
        }
    }

    private fun triggerAutoReply(conversationId: String, incoming: MessageEntry, persona: BotPersona) {
        viewModelScope.launch {
            _convoState.update { it.copy(isTyping = true) }
            delay(persona.replySpeed.delayMs)
            val rules = _state.value.replyRulesMap[persona.id] ?: emptyList()
            val reply = ReplyEngine.findReply(incoming.content, rules, persona)
                ?: ReplyEngine.fallbackReply(persona)
            _convoState.update { it.copy(isTyping = false) }
            val botMsg = MessageEntry(
                conversationId = conversationId,
                content = reply,
                senderType = SenderType.BOT,
                isAutoGenerated = true
            )
            appendMessage(botMsg, conversationId)
        }
    }
}
