package com.privacyshield.bothub.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val personaId: String,
    val contactId: String,
    val title: String = "",
    val lastMessagePreview: String = "",
    val lastMessageAt: Long = 0L,
    val messageCount: Int = 0,
    val isArchived: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("personaId", personaId)
        put("contactId", contactId)
        put("title", title)
        put("lastMessagePreview", lastMessagePreview)
        put("lastMessageAt", lastMessageAt)
        put("messageCount", messageCount)
        put("isArchived", isArchived)
        put("tags", JSONArray().also { arr -> tags.forEach { arr.put(it) } })
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): Conversation {
            val tagsArr = json.optJSONArray("tags") ?: JSONArray()
            val tags = (0 until tagsArr.length()).map { tagsArr.optString(it) }.filter { it.isNotBlank() }
            return Conversation(
                id = json.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                personaId = json.optString("personaId", ""),
                contactId = json.optString("contactId", ""),
                title = json.optString("title", ""),
                lastMessagePreview = json.optString("lastMessagePreview", ""),
                lastMessageAt = json.optLong("lastMessageAt", 0L),
                messageCount = json.optInt("messageCount", 0),
                isArchived = json.optBoolean("isArchived", false),
                tags = tags,
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
