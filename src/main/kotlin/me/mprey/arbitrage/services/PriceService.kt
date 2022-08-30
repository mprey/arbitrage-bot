package me.mprey.arbitrage.services

import me.mprey.arbitrage.aggregatorv3interface.AggregatorV3Interface
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.Token
import me.mprey.arbitrage.util.RateLimiter.sendRateLimitedOrNull
import me.mprey.arbitrage.util.SuspendingCache
import me.mprey.arbitrage.util.toDecimals
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger
import kotlin.time.Duration.Companion.seconds

class PriceService(
    private val web3jMap: Map<Network, Web3j>,
    private val credentials: Credentials,
    private val contractGasProvider: ContractGasProvider,
    private val cache: SuspendingCache<Pair<Network, Token>, BigInteger> = SuspendingCache(expiryTime = 30.seconds)
) {
    suspend fun getAmountFromUSD(network: Network, token: Token, usd: BigInteger): BigInteger {
        val price = getPrice(network, token)
        val inverse = token.decimals.toDecimals().pow(2) / price
        return inverse.multiply(usd)
    }

    suspend fun getUSDFromAmount(network: Network, token: Token, amount: BigInteger): BigInteger {
        val price = getPrice(network, token)
        return amount * price / token.decimals.toDecimals()
    }

    suspend fun getPrice(network: Network, token: Token): BigInteger {
        if (token.stablecoin) {
            // Not ideal
            return BigInteger.TEN.pow(token.decimals)
        }

        return cache.get(network to token) {
            val oracle = token.getOracle(network)
            val price = oracle.latestRoundData().sendRateLimitedOrNull(network)?.component2() ?: BigInteger.ZERO
            val oracleDecimals = oracle.decimals().sendRateLimitedOrNull(network) ?: BigInteger.ONE

            price * token.decimals.toDecimals() / oracleDecimals.toDecimals()
        }
    }

    private fun Token.getOracle(network: Network): AggregatorV3Interface {
        val oracleAddress = requireNotNull(this.oracles[network.chainId]) { "Token ${this.symbol} has no price oracle for ${network.name}" }
        return AggregatorV3Interface.load(oracleAddress, web3jMap[network]!!, credentials, contractGasProvider)
    }
}