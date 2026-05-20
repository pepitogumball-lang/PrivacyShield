package com.privacyshield.bothub.repository

import com.privacyshield.bothub.data.model.BotPersona
import com.privacyshield.bothub.data.model.ContactEntry
import com.privacyshield.bothub.data.model.Conversation
import com.privacyshield.bothub.data.model.MessageEntry
import com.privacyshield.bothub.data.model.ReplyRule
import com.privacyshield.bothub.storage.BotHubStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BotHubRepository(private val storage: BotHubStorage) {

    suspend fun getPersonas(): List<BotPersona> = withContext(Dispatchers.IO) { storage.loadPersonas() }
    suspend fun savePersonas(list: List<BotPersona>) = withContext(Dispatchers.IO) { storage.savePersonas(list) }
    suspend fun deletePersonaData(id: String) = withContext(Dispatchers.IO) { storage.deletePersonaFiles(id) }

    suspend fun getContacts(): List<ContactEntry> = withContext(Dispatchers.IO) { storage.loadContacts() }
    suspend fun saveContacts(list: List<ContactEntry>) = withContext(Dispatchers.IO) { storage.saveContacts(list) }

    suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) { storage.loadConversations() }
    suspend fun saveConversations(list: List<Conversation>) = withContext(Dispatchers.IO) { storage.saveConversations(list) }

    suspend fun getMessages(conversationId: String): List<MessageEntry> =
        withContext(Dispatchers.IO) { storage.loadMessages(conversationId) }

    suspend fun saveMessages(conversationId: String, messages: List<MessageEntry>) =
        withContext(Dispatchers.IO) { storage.saveMessages(conversationId, messages) }

    suspend fun deleteConversationData(id: String) = withContext(Dispatchers.IO) { storage.deleteConversationMessages(id) }

    suspend fun getReplyRules(personaId: String): List<ReplyRule> =
        withContext(Dispatchers.IO) { storage.loadReplyRules(personaId) }

    suspend fun saveReplyRules(personaId: String, rules: List<ReplyRule>) =
        withContext(Dispatchers.IO) { storage.saveReplyRules(personaId, rules) }
}
