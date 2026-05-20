package com.privacyshield.bothub.data.model

import org.json.JSONObject
import java.util.UUID

data class BotPersona(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bio: String = "",
    val avatarInitials: String = "",
    val tone: PersonaTone = PersonaTone.NEUTRAL,
    val formality: PersonaFormality = PersonaFormality.CASUAL,
    val emojiUsage: EmojiUsage = EmojiUsage.SPARSE,
    val replySpeed: ReplySpeed = ReplySpeed.NORMAL,
    val autoReplyEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val initials: String
        get() = avatarInitials.takeIf { it.isNotBlank() }
            ?: name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }.take(2).ifBlank { "?" }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("bio", bio)
        put("avatarInitials", avatarInitials)
        put("tone", tone.name)
        put("formality", formality.name)
        put("emojiUsage", emojiUsage.name)
        put("replySpeed", replySpeed.name)
        put("autoReplyEnabled", autoReplyEnabled)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): BotPersona = BotPersona(
            id = json.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            name = json.optString("name", "Bot"),
            bio = json.optString("bio", ""),
            avatarInitials = json.optString("avatarInitials", ""),
            tone = runCatching { PersonaTone.valueOf(json.optString("tone")) }.getOrDefault(PersonaTone.NEUTRAL),
            formality = runCatching { PersonaFormality.valueOf(json.optString("formality")) }.getOrDefault(PersonaFormality.CASUAL),
            emojiUsage = runCatching { EmojiUsage.valueOf(json.optString("emojiUsage")) }.getOrDefault(EmojiUsage.SPARSE),
            replySpeed = runCatching { ReplySpeed.valueOf(json.optString("replySpeed")) }.getOrDefault(ReplySpeed.NORMAL),
            autoReplyEnabled = json.optBoolean("autoReplyEnabled", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

enum class PersonaTone(val label: String) {
    FRIENDLY("Friendly"),
    NEUTRAL("Neutral"),
    PROFESSIONAL("Professional"),
    HUMOROUS("Humorous"),
    SARCASTIC("Sarcastic")
}

enum class PersonaFormality(val label: String) {
    FORMAL("Formal"),
    CASUAL("Casual"),
    STREET("Informal")
}

enum class EmojiUsage(val label: String) {
    NONE("None"),
    SPARSE("Sparse"),
    MODERATE("Moderate"),
    HEAVY("Heavy")
}

enum class ReplySpeed(val label: String, val delayMs: Long) {
    INSTANT("Instant", 300L),
    FAST("Fast", 800L),
    NORMAL("Normal", 2000L),
    SLOW("Slow", 4000L)
}
