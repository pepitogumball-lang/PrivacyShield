package com.privacyshield.protection

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class PrivacyAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        ForegroundAppSignal.packageFlow.value = pkg
    }

    override fun onInterrupt() = Unit
}
