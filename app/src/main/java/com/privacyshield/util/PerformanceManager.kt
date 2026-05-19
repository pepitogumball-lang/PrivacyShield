package com.privacyshield.util

enum class PerformanceMode(
    val label: String,
    val subtitle: String,
    val minScanIntervalMs: Long,
    val preloadIcons: Boolean,
    val showIcons: Boolean
) {
    LIGHT(
        label = "Light",
        subtitle = "Minimal scanning, icons off, longer cache TTL",
        minScanIntervalMs = 120_000L,
        preloadIcons = false,
        showIcons = false
    ),
    BALANCED(
        label = "Balanced",
        subtitle = "Smart caching, icons loaded on demand",
        minScanIntervalMs = 60_000L,
        preloadIcons = false,
        showIcons = true
    ),
    MAXIMUM(
        label = "Maximum",
        subtitle = "Aggressive caching, icons preloaded after scan",
        minScanIntervalMs = 30_000L,
        preloadIcons = true,
        showIcons = true
    );

    companion object {
        fun fromOrdinal(ordinal: Int): PerformanceMode =
            entries.getOrElse(ordinal) { BALANCED }
    }
}
