package me.mprey.arbitrage.util

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SuspendingCache<K, V>(
    private val expiryTime: Duration = 10.seconds,
    private val asyncCache: AsyncCache<K, V> = Caffeine.newBuilder().expireAfterWrite(expiryTime.toJavaDuration()).buildAsync()
) {
    suspend fun get(key: K, loader: suspend () -> V): V = supervisorScope {
        asyncCache.get(key) { _, _ ->
            future {
                loader()
            }
        }.await()
    }
}