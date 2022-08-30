package me.mprey.arbitrage.exchanges

import me.mprey.arbitrage.curverouter.CurveRouter
import me.mprey.arbitrage.models.ChainId
import me.mprey.arbitrage.models.ExchangeIndex
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.TokenPair
import me.mprey.arbitrage.util.RateLimiter.sendRateLimited
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

class Curve(
    network: Network,
    web3j: Web3j,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider,
    private val curveRouter: CurveRouter = CurveRouter.load(
        addressMap[network.chainId]!!,
        web3j,
        credentials,
        gasProvider
    ),
    override val estimatedGas: BigInteger = 700_000.toBigInteger(), // yikes
    override val index: Int = ExchangeIndex.CURVE.ordinal,
    override val lowFees: Boolean = true
) : AbstractExchange(network, web3j) {

    companion object {
        private val addressMap = mapOf(
            ChainId.AVALANCHE to "0x58e57cA18B7A47112b877E31929798Cd3D703b0f"
        )

        private val tokenIndexMap = mapOf(
            ChainId.AVALANCHE to mapOf(
                "USDT.e" to 2,
                "USDC.e" to 1,
                "DAI.e" to 0
            )
        )
    }
    override suspend fun getExecutionParams(pair: TokenPair): String {
        // TODO:
        return "hi"
    }

    override suspend fun _getOutputAmount(pair: TokenPair, quoteAmount: BigInteger): BigInteger {
        val baseIndex = requireNotNull(tokenIndexMap[network.chainId]?.get(pair.baseToken.symbol))
        val quoteIndex = requireNotNull(tokenIndexMap[network.chainId]?.get(pair.quoteToken.symbol))
        return curveRouter.get_dy_underlying(quoteIndex.toBigInteger(), baseIndex.toBigInteger(), quoteAmount).sendRateLimited(network)
    }
}