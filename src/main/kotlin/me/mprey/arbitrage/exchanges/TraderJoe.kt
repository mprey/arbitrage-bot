package me.mprey.arbitrage.exchanges

import me.mprey.arbitrage.models.ChainId
import me.mprey.arbitrage.models.ExchangeIndex
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.TokenPair
import me.mprey.arbitrage.traderjoerouter.TraderJoeRouter
import me.mprey.arbitrage.util.RateLimiter.sendRateLimited
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

class TraderJoe(
    network: Network,
    web3j: Web3j,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider,
    private val router: TraderJoeRouter = TraderJoeRouter.load(
        routerAddress[network.chainId]!!,
        web3j,
        credentials,
        gasProvider
    ),
    override val estimatedGas: BigInteger = 150_000.toBigInteger(),
    override val index: Int = ExchangeIndex.TRADER_JOE.ordinal,
    override val lowFees: Boolean = true
) : AbstractExchange(network, web3j) {

    companion object {
        private val routerAddress = mapOf(
            ChainId.AVALANCHE to "0x60aE616a2155Ee3d9A68541Ba4544862310933d4"
        )
    }

    override suspend fun getExecutionParams(pair: TokenPair): String {
        TODO("Not yet implemented")
    }

    override suspend fun _getOutputAmount(pair: TokenPair, quoteAmount: BigInteger): BigInteger {
        return router.getAmountsOut(
            quoteAmount,
            listOf(pair.quoteToken.addresses[network.chainId]!!, pair.baseToken.addresses[network.chainId]!!)
        ).sendRateLimited(network)[1] as BigInteger
    }
}