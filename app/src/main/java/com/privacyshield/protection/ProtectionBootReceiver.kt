package com.privacyshield.protection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProtectionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = ProtectionRuntimePrefs(appContext)
                if (prefs.liveProtectionEnabled.first()) {
                    val serviceIntent = Intent(appContext, ProtectionOrchestratorService::class.java).apply {
                        this.action = ProtectionOrchestratorService.ACTION_START
                    }
                    ContextCompat.startForegroundService(appContext, serviceIntent)
                }
            }
            pendingResult.finish()
        }
    }
}
