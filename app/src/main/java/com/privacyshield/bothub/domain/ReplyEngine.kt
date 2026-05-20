package com.privacyshield.bothub.domain

import com.privacyshield.bothub.data.model.BotPersona
import com.privacyshield.bothub.data.model.ReplyRule

object ReplyEngine {

    fun findReply(
        incomingText: String,
        rules: List<ReplyRule>,
        persona: BotPersona
    ): String? {
        if (incomingText.isBlank() || rules.isEmpty()) return null
        val matched = rules
            .filter { it.triggerKeyword.isNotBlank() && it.response.isNotBlank() }
            .sortedByDescending { it.priority }
            .firstOrNull { rule ->
                val haystack = if (rule.caseSensitive) incomingText else incomingText.lowercase()
                val needle = if (rule.caseSensitive) rule.triggerKeyword else rule.triggerKeyword.lowercase()
                haystack.contains(needle)
            }
        return matched?.response?.let { PersonaFormatter.applyStyle(it, persona) }
    }

    fun fallbackReply(persona: BotPersona): String =
        PersonaFormatter.applyStyle(FALLBACK_REPLIES.random(), persona)

    private val FALLBACK_REPLIES = listOf(
        "I see.",
        "Got it.",
        "Interesting.",
        "Tell me more.",
        "I'm listening.",
        "Noted.",
        "Sure.",
        "Understood.",
        "Right.",
        "Makes sense."
    )
}
