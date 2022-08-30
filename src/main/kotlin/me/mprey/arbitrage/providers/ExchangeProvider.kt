package me.mprey.arbitrage.providers

import me.mprey.arbitrage.exchanges.*
import me.mprey.arbitrage.models.Exchange
import me.mprey.arbitrage.models.Network
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.ContractGasProvider

class ExchangeProvider(
    private val web3jMap: Map<Network, Web3j>,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider,
) {
    fun createExchange(network: Network, exchange: Exchange): AbstractExchange = when (exchange.name) {
        "Curve" -> Curve(network, web3jMap[network]!!, credentials, gasProvider)
        "TraderJoe" -> TraderJoe(network, web3jMap[network]!!, credentials, gasProvider)
        "Dexalot" -> Dexalot(network, web3jMap[network]!!, credentials, gasProvider)
        "Platypus" -> Platypus(network, web3jMap[network]!!, credentials, gasProvider)
        else -> error("Invalid exchange: ${exchange.name}")
    }
}