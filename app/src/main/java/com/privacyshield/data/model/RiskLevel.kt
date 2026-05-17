package com.privacyshield.data.model

enum class RiskLevel(val score: Int, val label: String) {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High"),
    CRITICAL(3, "Critical");

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score >= 3 -> CRITICAL
            score == 2 -> HIGH
            score == 1 -> MEDIUM
            else -> LOW
        }
    }
}
