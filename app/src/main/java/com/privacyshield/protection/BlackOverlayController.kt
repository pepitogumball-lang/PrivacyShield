package com.privacyshield.protection

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlackOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val scope: CoroutineScope
) {
    companion object { private const val TAG = "BlackOverlayController" }
    private var overlayView: View? = null
    private var desiredVisible = false
    private var applyJob: Job? = null
    private var lastShowAtMs = 0L
    private var lastHideAtMs = 0L

    private val minVisibleMs = 500L
    private val minHiddenMs = 150L
    private val settleDelayMs = 65L
    var onOverlayVisibilityChanged: ((Boolean, Long) -> Unit)? = null

    fun setDesiredVisible(visible: Boolean) {
        desiredVisible = visible
        scheduleApply()
    }

    fun forceHide() {
        desiredVisible = false
        applyJob?.cancel()
        hideNow()
    }

    private fun scheduleApply() {
        applyJob?.cancel()
        applyJob = scope.launch(Dispatchers.Main.immediate) {
            delay(settleDelayMs)
            applyDesiredState()
        }
    }

    private fun applyDesiredState() {
        if (desiredVisible) {
            val hiddenFor = SystemClock.elapsedRealtime() - lastHideAtMs
            if (hiddenFor < minHiddenMs) {
                applyJob = scope.launch(Dispatchers.Main.immediate) {
                    delay(minHiddenMs - hiddenFor)
                    showNow()
                }
            } else {
                showNow()
            }
        } else {
            val visibleFor = SystemClock.elapsedRealtime() - lastShowAtMs
            if (overlayView != null && visibleFor < minVisibleMs) {
                applyJob = scope.launch(Dispatchers.Main.immediate) {
                    delay(minVisibleMs - visibleFor)
                    if (!desiredVisible) hideNow()
                }
            } else {
                hideNow()
            }
        }
    }

    private fun showNow() {
        if (overlayView != null || !Settings.canDrawOverlays(context)) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = View(context).apply { setBackgroundColor(Color.BLACK) }
        windowManager.addView(overlayView, params)
        lastShowAtMs = SystemClock.elapsedRealtime()
        Log.i(TAG, "Overlay shown at=$lastShowAtMs")
        onOverlayVisibilityChanged?.invoke(true, lastShowAtMs)
    }

    private fun hideNow() {
        overlayView?.let {
            windowManager.removeViewImmediate(it)
            overlayView = null
            lastHideAtMs = SystemClock.elapsedRealtime()
            Log.i(TAG, "Overlay hidden at=$lastHideAtMs")
            onOverlayVisibilityChanged?.invoke(false, lastHideAtMs)
        }
    }
}
