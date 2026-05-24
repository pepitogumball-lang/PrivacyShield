package com.privacyshield.protection

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android 14 compatible best-effort detector for screen sharing / projection risk.
 *
 * Limitations:
 * - Third-party apps cannot directly inspect other apps' MediaProjection sessions.
 * - We infer risk from route/display state and known capture app foreground activity.
 */
class MediaProjectionRiskDetector(
    private val context: Context,
    private val scope: CoroutineScope,
    private val usageFallback: UsageStatsForegroundDetector
) {
    companion object { private const val TAG = "MediaProjectionRisk" }
    private val mediaRouter = context.getSystemService(MediaRouter::class.java)
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    private val _isCaptureRiskActive = MutableStateFlow(false)
    val isCaptureRiskActive = _isCaptureRiskActive.asStateFlow()

    private var pollJob: Job? = null

    private val knownScreenShareKeywords = listOf(
        "zoom", "meet", "teams", "webex", "anydesk", "teamviewer", "rustdesk", "airdroid", "vysor"
    )

    fun start() {
        if (pollJob != null) return
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val risk = computeRisk()
                if (_isCaptureRiskActive.value != risk) {
                    Log.i(TAG, "Capture risk changed=$risk")
                }
                _isCaptureRiskActive.value = risk
                delay(500L)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        _isCaptureRiskActive.value = false
    }

    private fun computeRisk(): Boolean {
        val routeRisk = runCatching {
            val selected = mediaRouter?.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
            val defaultRoute = mediaRouter?.defaultRoute
            selected != null && defaultRoute != null && selected != defaultRoute
        }.getOrDefault(false)

        val multiDisplayRisk = runCatching {
            (displayManager?.displays?.size ?: 1) > 1
        }.getOrDefault(false)

        val foregroundPkg = ForegroundAppSignal.packageFlow.value ?: usageFallback.detectWithLogging()
        val knownCaptureAppForeground = foregroundPkg?.let { pkg ->
            val value = pkg.lowercase()
            knownScreenShareKeywords.any { value.contains(it) }
        } ?: false

        val result = routeRisk || multiDisplayRisk || knownCaptureAppForeground
        if (result) {
            Log.d(TAG, "risk route=$routeRisk display=$multiDisplayRisk knownApp=$knownCaptureAppForeground fg=$foregroundPkg")
        }
        return result
    }
}
