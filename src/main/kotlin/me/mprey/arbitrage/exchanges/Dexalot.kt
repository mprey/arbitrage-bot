package me.mprey.arbitrage.exchanges

import me.mprey.arbitrage.iorderbooks.IOrderBooks
import me.mprey.arbitrage.itradepairs.ITradePairs
import me.mprey.arbitrage.models.ChainId
import me.mprey.arbitrage.models.ExchangeIndex
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.TokenPair
import me.mprey.arbitrage.util.RateLimiter.sendRateLimited
import me.mprey.arbitrage.util.toBytes32
import me.mprey.arbitrage.util.toDecimals
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Dexalot(
    network: Network,
    web3j: Web3j,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider,
    private val orderBooks: IOrderBooks = IOrderBooks.load(
        orderBookAddress[network.chainId]!!,
        web3j,
        credentials,
        gasProvider
    ),
    private val tradePairs: ITradePairs = ITradePairs.load(
        tradePairAddress[network.chainId]!!,
        web3j,
        credentials,
        gasProvider
    ),
    override val estimatedGas: BigInteger = 400_000.toBigInteger(),
    override val index: Int = ExchangeIndex.DEXALOT.ordinal,
    override val orderBook: Boolean = true,
    override val defaultCacheExpiry: Duration = 3.seconds
) : AbstractExchange(network, web3j) {

    companion object {
        private val orderBookAddress = mapOf(
            ChainId.AVALANCHE to "0x3ece76f7add934fb8a35c9c371c4d545e299669a"
        )
        private val tradePairAddress = mapOf(
            ChainId.AVALANCHE to "0x1d34b421a5ede3e300d3b8bcf3be5c6f45971e20"
        )

        private const val N_ORDERS = 1 // Only use the top order on the book for now
        private val TENK = 10_000.toBigInteger() // Divisor for fee
    }

    private val feeMap: MutableMap<TokenPair, BigInteger> = mutableMapOf()

    override suspend fun getExecutionParams(pair: TokenPair): String {
        TODO("Not yet implemented")
    }

    override suspend fun _getOutputAmount(pair: TokenPair, quoteAmount: BigInteger): BigInteger {
        val orders = this.getNOrders(pair)
        val fee = this.getFee(pair)

        var output = 0.toBigInteger()
        var quoteRemaining = quoteAmount
        var idx = 0
        while (quoteRemaining > 0.toBigInteger() && idx < orders.size) {
            val price = orders[idx].price
            val quantity = if (pair.quoteToken.stablecoin) {
                orders[idx].quantity * price / pair.baseToken.decimals.toDecimals()
            } else {
                orders[idx].quantity
            }

            val amountToTake = if (quantity > quoteRemaining) quoteRemaining else quantity
            val addition = if (pair.quoteToken.stablecoin) {
                amountToTake * pair.baseToken.decimals.toDecimals() / price
            } else {
                amountToTake * price / pair.quoteToken.decimals.toDecimals()
            }

            quoteRemaining -= amountToTake
            output += addition
            idx++
        }

        return output * (TENK - fee) / TENK
    }

    private suspend fun getFee(pair: TokenPair): BigInteger {
        return feeMap.getOrPut(pair) {
            tradePairs.getTakerRate(pair.toTradeId().toBytes32()).sendRateLimited(network)
        }
    }

    data class Order(val price: BigInteger, val quantity: BigInteger)
    private suspend fun getNOrders(pair: TokenPair, n: Int = N_ORDERS): List<Order> {
        val (prices, quantities) = orderBooks.getNOrders(
            pair.toOrderBookId().toBytes32(),
            n.toBigInteger(),
            n.toBigInteger(),
            0.toBigInteger(),
            "".toBytes32(),
            pair.toSortType().toBigInteger()
        ).sendRateLimited(network)
        return prices.zip(quantities).map { Order(it.first, it.second) }
    }

    private fun TokenPair.toTradeId(): String = if (this.quoteToken.stablecoin) {
        "${this.baseToken.symbol}/${this.quoteToken.symbol}"
    } else {
        "${this.quoteToken.symbol}/${this.baseToken.symbol}"
    }

    private fun TokenPair.toOrderBookId(): String {
        val tradeId = this.toTradeId()
        return tradeId + if (!this.quoteToken.stablecoin) {
            "-BUYBOOK"
        } else {
            "-SELLBOOK"
        }
    }

    private fun TokenPair.toSortType(): Int = if (this.quoteToken.stablecoin) 0 else 1
}