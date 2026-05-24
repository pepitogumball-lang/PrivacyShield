package com.privacyshield.protection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.runtimePrefsStore: DataStore<Preferences> by preferencesDataStore(name = "protection_runtime_prefs")

class ProtectionRuntimePrefs(private val context: Context) {
    companion object {
        private val LIVE_PROTECTION_ENABLED = booleanPreferencesKey("live_protection_enabled")
        private val OVERLAY_WAS_VISIBLE = booleanPreferencesKey("overlay_was_visible")
        private val LAST_HEARTBEAT_MS = longPreferencesKey("last_heartbeat_ms")
    }

    val liveProtectionEnabled: Flow<Boolean> = context.runtimePrefsStore.data.map { it[LIVE_PROTECTION_ENABLED] ?: false }

    suspend fun setLiveProtectionEnabled(enabled: Boolean) {
        context.runtimePrefsStore.edit { it[LIVE_PROTECTION_ENABLED] = enabled }
    }

    suspend fun setOverlayVisible(visible: Boolean) {
        context.runtimePrefsStore.edit { it[OVERLAY_WAS_VISIBLE] = visible }
    }

    suspend fun updateHeartbeat(nowMs: Long) {
        context.runtimePrefsStore.edit { it[LAST_HEARTBEAT_MS] = nowMs }
    }
}
