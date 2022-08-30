package me.mprey.arbitrage.util

import io.github.resilience4j.kotlin.ratelimiter.RateLimiterConfig
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import me.mprey.arbitrage.models.Network
import org.web3j.protocol.core.RemoteFunctionCall
import java.time.Duration

object RateLimiter {
    @OptIn(DelicateCoroutinesApi::class)
    private val rateContext = newSingleThreadContext("RateLimiterContext")
    private val rateLimiters: MutableMap<Network, RateLimiter> = mutableMapOf()

    private suspend fun Network.getRateLimiter(): RateLimiter {
        val network = this
        return withContext(rateContext) {
            rateLimiters.getOrPut(network) {
                RateLimiter.of("HTTP-RPC", RateLimiterConfig {
                    this.limitForPeriod(network.rateLimit).also {
                        println("Rate limit ${network.rateLimit} for ${network.name}")
                    }
                    this.limitRefreshPeriod(Duration.ofMinutes(1))
                    this.timeoutDuration(Duration.ofSeconds(60))
                })
            }
        }
    }

    suspend fun <T> RemoteFunctionCall<T>.sendRateLimitedOrNull(network: Network) = runCatching {
        sendRateLimited(network)
    }.getOrNull()

    suspend fun <T> RemoteFunctionCall<T>.sendRateLimited(network: Network): T =
        network.getRateLimiter().executeSuspendFunction {
            this.sendAsync().await()
        }
}