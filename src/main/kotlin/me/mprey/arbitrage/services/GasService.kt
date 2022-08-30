package me.mprey.arbitrage.services

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.Token
import me.mprey.arbitrage.util.SuspendingCache
import me.mprey.arbitrage.util.toDecimals
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.web3j.protocol.Web3j
import java.math.BigInteger

class GasService(
    private val rpcMap: Map<Network, Web3j>,
    private val networks: List<Network>,
    private val database: CoroutineDatabase,
    private val priceService: PriceService,
    private val cache: SuspendingCache<Network, BigInteger> = SuspendingCache()
) {
    private val nativeTokens by lazy {
        runBlocking {
            networks.associateWith {
                database.getCollection<Token>().findOne(Token::symbol eq it.nativeSymbol)!!
            }
        }
    }

    companion object {
        private const val GAS_DECIMALS = 6
        private const val NATIVE_GAS_DECIMALS = 9 // 1 gwei = ether * 10^-9
    }

    suspend fun getGasPrice(network: Network): BigInteger {
        // TODO: Should we also look at off-chain sources?
        return cache.get(network) {
            rpcMap[network]!!.ethGasPrice().sendAsync().await().gasPrice
        }.also { println("Gas price $it") }
    }

    suspend fun convertGasToToken(network: Network, gasAmount: BigInteger, token: Token): BigInteger {
        val gasPrice = getGasPrice(network)
        val totalGas = gasAmount * gasPrice

        val nativePrice = priceService.getPrice(network, nativeTokens[network]!!)
        val tokenPrice = priceService.getPrice(network, token)

        val totalGasInNative = totalGas * 18.toDecimals() / NATIVE_GAS_DECIMALS.toDecimals()// / GAS_DECIMALS.toDecimals()
        val tokenPerNative = nativePrice * token.decimals.toDecimals() / tokenPrice * token.decimals.toDecimals() / 18.toDecimals()

        return totalGasInNative * tokenPerNative / 18.toDecimals()
    }
}