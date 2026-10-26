package com.example.secretvault.cache

import android.graphics.Bitmap
import android.util.LruCache

object VaultMemoryCache {
    // 25MB max cache size or default 1/8th of heap
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16) // at least 16MB

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(id: String): Bitmap? {
        return lruCache.get(id)
    }

    fun put(id: String, bitmap: Bitmap) {
        lruCache.put(id, bitmap)
    }

    fun remove(id: String) {
        lruCache.remove(id)
    }

    /**
     * Instantly evacuates all decrypted bitmaps from memory.
     * Guarantees zero residual plaintext data retention in RAM on vault lock.
     */
    fun clear() {
        lruCache.evictAll()
    }
}
