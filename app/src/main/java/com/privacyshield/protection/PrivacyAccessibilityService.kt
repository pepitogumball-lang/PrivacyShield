package com.privacyshield.protection

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class PrivacyAccessibilityService : AccessibilityService() {
    companion object { private const val TAG = "PrivacyAccessibility" }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != packageName) {
            Log.d(TAG, "Window changed pkg=$pkg")
            ForegroundAppSignal.updateFromAccessibility(pkg)
        }
    }

    override fun onInterrupt() = Unit
}
