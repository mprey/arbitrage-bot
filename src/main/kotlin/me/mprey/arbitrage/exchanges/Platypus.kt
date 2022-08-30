package me.mprey.arbitrage.exchanges

import me.mprey.arbitrage.models.ChainId
import me.mprey.arbitrage.models.ExchangeIndex
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.TokenPair
import me.mprey.arbitrage.platypusrouter.PlatypusRouter
import me.mprey.arbitrage.util.RateLimiter.sendRateLimited
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

class Platypus(
    network: Network,
    web3j: Web3j,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider,
    private val router: PlatypusRouter = PlatypusRouter.load(
        routerAddress[network.chainId]!!,
        web3j,
        credentials,
        gasProvider
    ),
    override val estimatedGas: BigInteger = 250_000.toBigInteger(),
    override val index: Int = ExchangeIndex.PLATYPUS.ordinal,
    override val lowFees: Boolean = true
) : AbstractExchange(network, web3j) {

    companion object {
        private val routerAddress = mapOf(
            ChainId.AVALANCHE to "0x73256EC7575D999C360c1EeC118ECbEFd8DA7D12"
        )
        private val poolAddress = mapOf(
            ChainId.AVALANCHE to "0x66357dcace80431aee0a7507e2e361b7e2402370"
        )
    }

    override suspend fun getExecutionParams(pair: TokenPair): String {
        TODO("Not yet implemented")
    }

    override suspend fun _getOutputAmount(pair: TokenPair, quoteAmount: BigInteger): BigInteger {
        val tokenPath = listOf(
            pair.quoteToken.addresses[network.chainId]!!,
            pair.baseToken.addresses[network.chainId]!!
        )
        val poolPath = listOf(poolAddress[network.chainId]!!)
        return router.quotePotentialSwaps(tokenPath, poolPath, quoteAmount).sendRateLimited(network).component1()
    }
}