package com.privacyshield.bothub.domain

import com.privacyshield.bothub.data.model.BotPersona
import com.privacyshield.bothub.data.model.EmojiUsage
import com.privacyshield.bothub.data.model.PersonaFormality

object PersonaFormatter {

    fun applyStyle(text: String, persona: BotPersona): String {
        var result = applyFormality(text, persona.formality)
        result = applyEmoji(result, persona.emojiUsage)
        return result
    }

    private fun applyFormality(text: String, formality: PersonaFormality): String = when (formality) {
        PersonaFormality.CASUAL -> text
            .replace("I am ", "I'm ")
            .replace("You are ", "You're ")
            .replace("It is ", "It's ")
            .replace("Do not ", "Don't ")
            .replace("Cannot ", "Can't ")
            .replace("Will not ", "Won't ")
        PersonaFormality.FORMAL -> text
            .replace("I'm ", "I am ")
            .replace("You're ", "You are ")
            .replace("It's ", "It is ")
            .replace("Don't ", "Do not ")
            .replace("Can't ", "Cannot ")
            .replace("Won't ", "Will not ")
            .replace("yeah", "yes")
            .replace("Yeah", "Yes")
            .replace("nope", "no")
        PersonaFormality.STREET -> text
            .replace("I am ", "I'm ")
            .replace("going to", "gonna")
            .replace("want to", "wanna")
            .replace("have to", "hafta")
    }

    private fun applyEmoji(text: String, usage: EmojiUsage): String = when (usage) {
        EmojiUsage.NONE -> text
        EmojiUsage.SPARSE -> if (text.endsWith("!")) "$text ✓" else text
        EmojiUsage.MODERATE -> text + contextualEmoji(text)
        EmojiUsage.HEAVY -> text + contextualEmoji(text) + contextualEmoji(text)
    }

    private fun contextualEmoji(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("good") || lower.contains("great") || lower.contains("nice") -> " 👍"
            lower.contains("sorry") || lower.contains("apolog") -> " 😔"
            lower.contains("ok") || lower.contains("sure") || lower.contains("alright") -> " 👌"
            lower.contains("understand") || lower.contains("see") || lower.contains("noted") -> " 🤔"
            lower.contains("yes") || lower.contains("absolutely") || lower.contains("correct") -> " ✅"
            lower.contains("no") || lower.contains("disagree") -> " ❌"
            lower.contains("help") || lower.contains("assist") -> " 🤝"
            else -> " 💬"
        }
    }
}
