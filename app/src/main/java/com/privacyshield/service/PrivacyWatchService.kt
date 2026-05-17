package com.privacyshield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.privacyshield.MainActivity

/**
 * PrivacyWatchService — a foreground service skeleton for ongoing privacy monitoring.
 *
 * Limitations on stock Android:
 * - Apps cannot intercept or block other apps' system calls.
 * - Apps cannot read another app's screen content without accessibility service consent.
 * - Apps cannot kill or freeze other apps without root or device-owner privileges.
 *
 * What this service CAN legitimately do:
 * - Stay alive in the background to run periodic scans.
 * - Post notifications when a newly installed app matches a risk profile.
 * - Alert the user when a protected app appears to be running alongside a risky app.
 *   (Detectable via UsageStatsManager, which requires PACKAGE_USAGE_STATS permission
 *    granted by the user in system settings — not a runtime permission.)
 *
 * The service is started only when the user explicitly enables monitoring.
 */
class PrivacyWatchService : Service() {

    companion object {
        const val CHANNEL_ID = "privacy_shield_watch"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.privacyshield.action.START_WATCH"
        const val ACTION_STOP = "com.privacyshield.action.STOP_WATCH"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> startForeground(NOTIFICATION_ID, buildNotification())
        }
        // START_NOT_STICKY: do not auto-restart if killed; user must opt in again
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Privacy Watch",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active privacy monitoring by PrivacyShield"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, PrivacyWatchService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("PrivacyShield is active")
            .setContentText("Monitoring for privacy risks…")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openPending)
            .addAction(
                Notification.Action.Builder(
                    null, "Stop", stopPending
                ).build()
            )
            .setOngoing(true)
            .build()
    }
}
