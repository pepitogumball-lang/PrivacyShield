package com.privacyshield.protection

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.privacyshield.MainActivity
import com.privacyshield.R
import com.privacyshield.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProtectionOrchestratorService : Service() {
    companion object {
        const val ACTION_START = "com.privacyshield.protection.START"
        const val ACTION_STOP = "com.privacyshield.protection.STOP"
        private const val CHANNEL_ID = "protection_orchestrator"
        private const val NOTIFICATION_ID = 3001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: AppRepository
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    @Volatile private var currentForegroundPackage: String? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = evaluateProtection()
        override fun onDisplayChanged(displayId: Int) = evaluateProtection()
    }

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(applicationContext)
        windowManager = getSystemService(WindowManager::class.java)
        createNotificationChannel()
        getSystemService(DisplayManager::class.java)?.registerDisplayListener(displayListener, null)

        scope.launch {
            combine(repository.protectedPackagesFlow, ForegroundAppSignal.packageFlow) { protected, foreground ->
                currentForegroundPackage = foreground
                protected.contains(foreground)
            }.collect { protectedOpen ->
                val suspicious = isSuspiciousCaptureActive() || hasSuspiciousAccessibilityServices()
                if (protectedOpen && suspicious) showOverlay() else hideOverlay()
            }
        }
    }

    override fun onDestroy() {
        hideOverlay()
        getSystemService(DisplayManager::class.java)?.unregisterDisplayListener(displayListener)
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isSuspiciousCaptureActive(): Boolean {
        val mediaRouter = getSystemService(MediaRouter::class.java)
        val selectedRoute = mediaRouter?.selectedRoute
        val routeEnabled = selectedRoute != null && selectedRoute != mediaRouter.defaultRoute

        val isMirroring = (getSystemService(DisplayManager::class.java)?.displays?.size ?: 1) > 1
        val screenCaptured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            false
        } else {
            false
        }
        return routeEnabled || isMirroring || screenCaptured
    }

    private fun hasSuspiciousAccessibilityServices(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val suspiciousKeywords = listOf("anydesk", "teamviewer", "rustdesk", "airdroid", "vysor", "scrcpy")
        return enabled.any { info ->
            val id = info.id.lowercase()
            suspiciousKeywords.any { id.contains(it) }
        }
    }

    private fun evaluateProtection() {
        val pkg = currentForegroundPackage ?: return
        scope.launch {
            val protected = repository.protectedPackagesFlow
            // noop trigger through collector
        }
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this) || overlayView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = View(this).apply { setBackgroundColor(Color.BLACK) }
        windowManager.addView(overlayView, params)
    }

    private fun hideOverlay() {
        overlayView?.let {
            windowManager.removeViewImmediate(it)
            overlayView = null
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Privacy protection", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
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
            .setContentText("Monitoring capture and remote-viewing risks")
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
}

object ForegroundAppSignal {
    val packageFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
}
