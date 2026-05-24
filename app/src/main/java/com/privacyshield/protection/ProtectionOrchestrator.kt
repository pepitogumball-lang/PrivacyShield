package com.privacyshield.protection

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.privacyshield.MainActivity
import com.privacyshield.R
import com.privacyshield.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProtectionOrchestratorService : Service() {
    companion object {
        private const val TAG = "ProtectionOrchestrator"
        const val ACTION_START = "com.privacyshield.protection.START"
        const val ACTION_STOP = "com.privacyshield.protection.STOP"
        private const val CHANNEL_ID = "protection_orchestrator"
        private const val NOTIFICATION_ID = 3001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: AppRepository
    private lateinit var windowManager: WindowManager
    private lateinit var usageFallback: UsageStatsForegroundDetector
    private lateinit var overlayController: BlackOverlayController
    private lateinit var mediaProjectionRiskDetector: MediaProjectionRiskDetector
    private lateinit var runtimePrefs: ProtectionRuntimePrefs
    private var recordingDetectedAtMs: Long? = null
    private var protectedDetectedAtMs: Long? = null

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(applicationContext)
        windowManager = getSystemService(WindowManager::class.java)
        usageFallback = UsageStatsForegroundDetector(applicationContext)
        overlayController = BlackOverlayController(this, windowManager, scope)
        overlayController.onOverlayVisibilityChanged = { active, shownAt ->
            ProtectionDebugState.update { it.copy(overlayActive = active, overlayShownAtMs = if (active) shownAt else null) }
            if (active) {
                val rec = recordingDetectedAtMs
                val prot = protectedDetectedAtMs
                Log.i(TAG, "Overlay active. latency(record->overlay)=${rec?.let { shownAt - it }}ms latency(protected->overlay)=${prot?.let { shownAt - it }}ms")
            }
            updateNotificationDebug()
        }
        mediaProjectionRiskDetector = MediaProjectionRiskDetector(this, scope, usageFallback)
        runtimePrefs = ProtectionRuntimePrefs(applicationContext)
        mediaProjectionRiskDetector.start()
        createNotificationChannel()

        scope.launch(Dispatchers.IO) { runtimePrefs.setLiveProtectionEnabled(true) }

        scope.launch {
            while (isActive) {
                val stale = System.currentTimeMillis() - ForegroundAppSignal.lastAccessibilityEventAtMs > 1200L
                if (stale) {
                    val fallbackPkg = usageFallback.detectWithLogging()
                    if (!fallbackPkg.isNullOrBlank()) {
                        Log.d(TAG, "Foreground fallback package=$fallbackPkg")
                    }
                    ForegroundAppSignal.updateFromFallback(fallbackPkg)
                }
                runtimePrefs.updateHeartbeat(System.currentTimeMillis())
                delay(400L)
            }
        }

        scope.launch {
            combine(
                repository.protectedPackagesFlow,
                ForegroundAppSignal.packageFlow,
                mediaProjectionRiskDetector.isCaptureRiskActive
            ) { protected, foreground, mediaProjectionRisk ->
                val protectedOpen = protected.contains(foreground)
                val suspicious = mediaProjectionRisk || hasSuspiciousAccessibilityServices() || isSuspiciousCaptureActive()
                if (mediaProjectionRisk && recordingDetectedAtMs == null) recordingDetectedAtMs = SystemClock.elapsedRealtime()
                if (protectedOpen && protectedDetectedAtMs == null) protectedDetectedAtMs = SystemClock.elapsedRealtime()
                val reason = when {
                    !protectedOpen -> "not_protected_app"
                    mediaProjectionRisk -> "media_projection_risk"
                    hasSuspiciousAccessibilityServices() -> "suspicious_accessibility"
                    isSuspiciousCaptureActive() -> "route_or_display_capture"
                    else -> "none"
                }
                ProtectionDebugState.update {
                    it.copy(
                        foregroundPackage = foreground ?: "-",
                        recordingRisk = suspicious,
                        reason = reason,
                        recordingDetectedAtMs = recordingDetectedAtMs,
                        protectedDetectedAtMs = protectedDetectedAtMs
                    )
                }
                Log.d(TAG, "decision fg=$foreground protected=$protectedOpen suspicious=$suspicious reason=$reason")
                protectedOpen && suspicious
            }.distinctUntilChanged().collect { shouldShow ->
                overlayController.setDesiredVisible(shouldShow)
                runtimePrefs.setOverlayVisible(shouldShow)
                updateNotificationDebug()
                if (!shouldShow) {
                    recordingDetectedAtMs = null
                    protectedDetectedAtMs = null
                }
            }
        }
    }

    override fun onDestroy() {
        mediaProjectionRiskDetector.stop()
        overlayController.forceHide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch(Dispatchers.IO) {
                runtimePrefs.setLiveProtectionEnabled(false)
                runtimePrefs.setOverlayVisible(false)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null


    override fun onTaskRemoved(rootIntent: Intent?) {
        // One UI may swipe-kill task aggressively; keep protection service resilient.
        val restartIntent = Intent(applicationContext, ProtectionOrchestratorService::class.java).apply {
            action = ACTION_START
        }
androidx.core.content.ContextCompat.startForegroundService(applicationContext, restartIntent)
        super.onTaskRemoved(rootIntent)
    }

    private fun isSuspiciousCaptureActive(): Boolean {
        val mediaRouter = getSystemService(MediaRouter::class.java)
        val selectedRoute = mediaRouter?.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
        val defaultRoute = mediaRouter?.defaultRoute
        val routeEnabled = selectedRoute != null && defaultRoute != null && selectedRoute != defaultRoute
        val isMirroring = (getSystemService(DisplayManager::class.java)?.displays?.size ?: 1) > 1
        return routeEnabled || isMirroring
    }

    private fun hasSuspiciousAccessibilityServices(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val suspiciousKeywords = listOf("anydesk", "teamviewer", "rustdesk", "airdroid", "vysor", "scrcpy")
        return enabled.any { info -> suspiciousKeywords.any { info.id.lowercase().contains(it) } }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Privacy protection", NotificationManager.IMPORTANCE_MIN)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val dbg = ProtectionDebugState.state.value
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, ProtectionOrchestratorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("PrivacyShield protection active")
            .setContentText("FG:${dbg.foregroundPackage} REC:${dbg.recordingRisk} OVL:${dbg.overlayActive} ${dbg.reason}")
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotificationDebug() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }
}
