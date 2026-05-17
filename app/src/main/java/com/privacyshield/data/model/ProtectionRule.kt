package com.privacyshield.data.model

data class ProtectionRule(
    val packageName: String,
    val appName: String,
    val addedAt: Long = System.currentTimeMillis()
)
