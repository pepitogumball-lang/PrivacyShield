package com.privacyshield.protection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ForegroundAppTracker"

object ForegroundAppSignal {
    private val _packageFlow = MutableStateFlow<String?>(null)
    val packageFlow = _packageFlow.asStateFlow()

    @Volatile var lastAccessibilityEventAtMs: Long = 0L
        private set

    fun updateFromAccessibility(pkg: String) {
        lastAccessibilityEventAtMs = System.currentTimeMillis()
        _packageFlow.value = pkg
    }

    fun updateFromFallback(pkg: String?) {
        if (!pkg.isNullOrBlank()) _packageFlow.value = pkg
    }
}

class UsageStatsForegroundDetector(private val context: Context) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun detectForegroundPackage(nowMs: Long = System.currentTimeMillis()): String? {
        if (!hasUsageAccess()) return null
        val windowStart = nowMs - 15_000L
        val events = usageStatsManager.queryEvents(windowStart, nowMs)
        val event = UsageEvents.Event()
        var candidate: String? = null
        var latestTs = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkg = event.packageName ?: continue
                if (event.timeStamp >= latestTs) {
                    latestTs = event.timeStamp
                    candidate = pkg
                }
            }
        }
        return candidate
    }

    fun detectWithLogging(): String? {
        return runCatching { detectForegroundPackage() }
            .onFailure { Log.w(TAG, "UsageStats fallback failed", it) }
            .getOrNull()
    }
}
