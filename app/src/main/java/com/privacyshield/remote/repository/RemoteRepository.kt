package com.privacyshield.remote.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.privacyshield.remote.model.RemoteDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.remoteDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "remote_prefs")

class RemoteRepository(private val context: Context) {

    companion object {
        private val SAVED_DEVICES_KEY = stringSetPreferencesKey("saved_devices")
        private val LAST_DEVICE_ID_KEY = stringPreferencesKey("last_device_id")
    }

    // ── Device persistence ────────────────────────────────────────────────

    val savedDevicesFlow: Flow<List<RemoteDevice>> = context.remoteDataStore.data.map { prefs ->
        prefs[SAVED_DEVICES_KEY]
            ?.mapNotNull { RemoteDevice.deserialize(it) }
            ?.sortedBy { it.displayName }
            ?: emptyList()
    }

    val lastDeviceIdFlow: Flow<String?> = context.remoteDataStore.data.map { prefs ->
        prefs[LAST_DEVICE_ID_KEY]
    }

    suspend fun saveDevice(device: RemoteDevice) {
        context.remoteDataStore.edit { prefs ->
            val current = prefs[SAVED_DEVICES_KEY] ?: emptySet()
            // Remove any existing entry with the same id before inserting
            val filtered = current.filter {
                RemoteDevice.deserialize(it)?.id != device.id
            }.toSet()
            prefs[SAVED_DEVICES_KEY] = filtered + device.serialize()
        }
    }

    suspend fun removeDevice(deviceId: String) {
        context.remoteDataStore.edit { prefs ->
            val current = prefs[SAVED_DEVICES_KEY] ?: emptySet()
            prefs[SAVED_DEVICES_KEY] = current.filter {
                RemoteDevice.deserialize(it)?.id != deviceId
            }.toSet()
        }
    }

    suspend fun renameDevice(deviceId: String, newName: String) {
        context.remoteDataStore.edit { prefs ->
            val current = prefs[SAVED_DEVICES_KEY] ?: emptySet()
            prefs[SAVED_DEVICES_KEY] = current.map { raw ->
                val device = RemoteDevice.deserialize(raw)
                if (device?.id == deviceId) device.copy(customName = newName).serialize()
                else raw
            }.toSet()
        }
    }

    suspend fun markPaired(deviceId: String, paired: Boolean) {
        context.remoteDataStore.edit { prefs ->
            val current = prefs[SAVED_DEVICES_KEY] ?: emptySet()
            prefs[SAVED_DEVICES_KEY] = current.map { raw ->
                val device = RemoteDevice.deserialize(raw)
                if (device?.id == deviceId) device.copy(isPaired = paired).serialize()
                else raw
            }.toSet()
        }
    }

    suspend fun setLastDeviceId(deviceId: String) {
        context.remoteDataStore.edit { prefs ->
            prefs[LAST_DEVICE_ID_KEY] = deviceId
        }
    }

    suspend fun getLastDevice(): RemoteDevice? {
        val id = lastDeviceIdFlow.first() ?: return null
        return savedDevicesFlow.first().firstOrNull { it.id == id }
    }
}
