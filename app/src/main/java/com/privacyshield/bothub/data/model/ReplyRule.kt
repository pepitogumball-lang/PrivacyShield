package com.privacyshield.bothub.data.model

import org.json.JSONObject
import java.util.UUID

data class ReplyRule(
    val id: String = UUID.randomUUID().toString(),
    val personaId: String,
    val triggerKeyword: String,
    val response: String,
    val caseSensitive: Boolean = false,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("personaId", personaId)
        put("triggerKeyword", triggerKeyword)
        put("response", response)
        put("caseSensitive", caseSensitive)
        put("priority", priority)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): ReplyRule = ReplyRule(
            id = json.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            personaId = json.optString("personaId", ""),
            triggerKeyword = json.optString("triggerKeyword", ""),
            response = json.optString("response", ""),
            caseSensitive = json.optBoolean("caseSensitive", false),
            priority = json.optInt("priority", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
