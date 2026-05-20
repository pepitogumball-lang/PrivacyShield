package com.privacyshield.bothub.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ContactEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val alias: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val initials: String
        get() = name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }.take(2).ifBlank { "?" }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("alias", alias)
        put("notes", notes)
        put("tags", JSONArray().also { arr -> tags.forEach { arr.put(it) } })
        put("isArchived", isArchived)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): ContactEntry {
            val tagsArr = json.optJSONArray("tags") ?: JSONArray()
            val tags = (0 until tagsArr.length()).map { tagsArr.optString(it) }.filter { it.isNotBlank() }
            return ContactEntry(
                id = json.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                name = json.optString("name", "Contact"),
                alias = json.optString("alias", ""),
                notes = json.optString("notes", ""),
                tags = tags,
                isArchived = json.optBoolean("isArchived", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}
