package com.privacyshield.util

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IconCache {

    private val maxMemKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemKb / 8).coerceAtLeast(4096)

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun getSync(packageName: String): Bitmap? = cache.get(packageName)

    suspend fun loadIcon(context: Context, packageName: String): Bitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(72, 72)
                cache.put(packageName, bitmap)
                bitmap
            } catch (_: Exception) {
                null
            }
        }
    }

    fun clear() = cache.evictAll()

    fun size(): Int = cache.size()

    fun hitCount(): Int = cache.hitCount()

    fun missCount(): Int = cache.missCount()

    fun maxSizeKb(): Int = cache.maxSize()

    fun currentSizeKb(): Int = cache.size()
}
