package com.privacyshield.bothub.storage

import android.content.Context
import com.privacyshield.bothub.data.model.BotPersona
import com.privacyshield.bothub.data.model.ContactEntry
import com.privacyshield.bothub.data.model.Conversation
import com.privacyshield.bothub.data.model.MessageEntry
import com.privacyshield.bothub.data.model.ReplyRule
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * File-based storage for BotHub data using Android's bundled org.json.
 * All data lives in filesDir/bothub/ as JSON files.
 * Writes are atomic: write to .tmp then rename.
 */
class BotHubStorage(context: Context) {

    private val dir = File(context.filesDir, "bothub").also { it.mkdirs() }

    private fun <T> loadList(filename: String, fromJson: (JSONObject) -> T): List<T> = try {
        val file = File(dir, filename)
        if (!file.exists()) emptyList()
        else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull {
                try { fromJson(arr.getJSONObject(it)) } catch (_: Exception) { null }
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun <T> saveList(filename: String, items: List<T>, toJson: (T) -> JSONObject) {
        val arr = JSONArray().also { a -> items.forEach { a.put(toJson(it)) } }
        val tmp = File(dir, "$filename.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(File(dir, filename))
    }

    fun loadPersonas(): List<BotPersona> = loadList("personas.json") { BotPersona.fromJson(it) }
    fun savePersonas(list: List<BotPersona>) = saveList("personas.json", list) { it.toJson() }

    fun loadContacts(): List<ContactEntry> = loadList("contacts.json") { ContactEntry.fromJson(it) }
    fun saveContacts(list: List<ContactEntry>) = saveList("contacts.json", list) { it.toJson() }

    fun loadConversations(): List<Conversation> = loadList("conversations.json") { Conversation.fromJson(it) }
    fun saveConversations(list: List<Conversation>) = saveList("conversations.json", list) { it.toJson() }

    fun loadMessages(conversationId: String): List<MessageEntry> =
        loadList("messages_$conversationId.json") { MessageEntry.fromJson(it) }

    fun saveMessages(conversationId: String, messages: List<MessageEntry>) =
        saveList("messages_$conversationId.json", messages) { it.toJson() }

    fun deleteConversationMessages(conversationId: String) {
        File(dir, "messages_$conversationId.json").delete()
    }

    fun loadReplyRules(personaId: String): List<ReplyRule> =
        loadList("rules_$personaId.json") { ReplyRule.fromJson(it) }

    fun saveReplyRules(personaId: String, rules: List<ReplyRule>) =
        saveList("rules_$personaId.json", rules) { it.toJson() }

    fun deletePersonaFiles(personaId: String) {
        File(dir, "rules_$personaId.json").delete()
    }
}
