package me.mprey.arbitrage.exchanges

import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.TokenPair
import me.mprey.arbitrage.util.SuspendingCache
import me.mprey.arbitrage.util.roundTokenAmountToDecimals
import org.web3j.protocol.Web3j
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class AbstractExchange(
    protected val network: Network,
    protected val web3j: Web3j,
    protected open val defaultCacheExpiry: Duration = 15.seconds,
    private val cache: SuspendingCache<Pair<TokenPair, BigInteger>, BigInteger> = SuspendingCache(defaultCacheExpiry)
) {
    abstract val estimatedGas: BigInteger
    abstract val index: Int
    open val orderBook: Boolean = false
    open val lowFees: Boolean = false

    suspend fun getOutputAmount(pair: TokenPair, quoteAmount: BigInteger): BigInteger {
//        val rounded = roundTokenAmountToDecimals(pair.quoteToken, quoteAmount)
        return cache.get(pair to quoteAmount) {
            runCatching {
                _getOutputAmount(pair, quoteAmount)
            }.recoverCatching {
                println("Error fetching ${pair.baseToken.symbol}/${pair.quoteToken.symbol}: ${it.message}")
                BigInteger.ZERO
            }.getOrNull()!!
        }
    }

    abstract suspend fun getExecutionParams(pair: TokenPair): String
    protected abstract suspend fun _getOutputAmount(pair: TokenPair, quoteAmount: BigInteger): BigInteger
}