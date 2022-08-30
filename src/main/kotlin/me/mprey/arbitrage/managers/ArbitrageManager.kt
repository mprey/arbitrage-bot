package me.mprey.arbitrage.managers

import kotlinx.coroutines.*
import me.mprey.arbitrage.graphs.GraphBuilder
import me.mprey.arbitrage.graphs.GraphCycleIterator
import me.mprey.arbitrage.graphs.GraphEdge
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.Token
import me.mprey.arbitrage.models.TokenPair
import me.mprey.arbitrage.services.GasService
import me.mprey.arbitrage.services.PriceService
import me.mprey.arbitrage.util.findPeakValue
import me.mprey.arbitrage.util.isZero
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.web3j.protocol.Web3j
import java.math.BigInteger

class ArbitrageManager(
    networks: List<Network>,
    private val graphBuilder: GraphBuilder,
    private val web3jMap: Map<Network, Web3j>,
    private val priceService: PriceService,
    private val gasService: GasService
) {
    private var graphs: Map<Network, Graph<Token, GraphEdge>> = networks.associateWith { graphBuilder.build(it) }

    init {
        networks.forEach { runArbitrage(it) }
    }

    companion object {
        // If an exchange is low fees, only include it 2 times at max
        private const val MAX_COUNT_FOR_LOW_FEES = 2

        private const val USD_INCREMENT = 1
        private val USD_RANGE = 10 to 1_000
        // Gas to send a transaction and get a flash loan
        private val DEFAULT_GAS = 150_000.toBigInteger()

        private val FLASH_LOAN_FEE = 50.toBigInteger() // .05%
        private val FLASH_LOAN_BASE = 100_000.toBigInteger()

        private const val CYCLE_TIME = 5000L
    }

    private fun shouldAddEdge(edges: List<GraphEdge>, next: GraphEdge): Boolean {
        if (edges.isEmpty()) return true

        if (edges.last().exchange == next.exchange) return false

        val instances = edges.count { it.exchange == next.exchange }

        return if (next.exchange.lowFees) {
            instances < MAX_COUNT_FOR_LOW_FEES
        } else {
            instances == 0
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun runArbitrage(network: Network) {
        println("Starting arbitrage on ${network.name}")

        val graphCycleIterator = GraphCycleIterator(graphs[network]!!, shouldAddEdge = ::shouldAddEdge)

        val startingTokens = network.exchanges
            .flatMap { exchange ->
                val pairs = exchange.pairs[network.chainId] ?: emptyList()
                pairs.flatMap { listOf(it.baseToken, it.quoteToken) }
            }
            .distinctBy { it.symbol }
            .filter { it.symbol in network.flashLoanable }
            .associateWith { graphCycleIterator.computeCycles(it) }

        GlobalScope.launch {
            while (true) {
                println("Cycling...")
                val results = startingTokens.map { (token, paths) ->
                    async {
                        executeArbitrage(network, token, paths)
                    }
                }.awaitAll().filterNotNull()
                if (results.size > 1) println("Found multiple results! ${results.size}")
                if (results.isNotEmpty()) {
                    // TODO: Optimize this
                    val winner = results.first()
                    println("Winner!!!!")
                    println(winner)
                }
                delay(CYCLE_TIME)
            }
        }
    }

    private suspend fun executeArbitrage(
        network: Network,
        token: Token,
        tokenPaths: List<GraphPath<Token, GraphEdge>>
    ): ArbitrageResult? {
        val startAmount = getStartingAmount(network, token)
        return tokenPaths
            .associateWith { walkPath(startAmount, it) }
            .mapValues { (_, result) ->
                // Subtract the flash loan fee
                val endWithFee = result.endAmount * (FLASH_LOAN_BASE - FLASH_LOAN_FEE) / FLASH_LOAN_BASE
                result.copy(endAmount = endWithFee)
            }
//            .onEach {
//                if (it.value.endAmount <= startAmount) {
//                    println("Lost ${it.key.formatted()} (${startAmount}-${it.value.endAmount}=${startAmount - it.value.endAmount})")
//                }
//            }
            .filterValues { it.endAmount > startAmount }
            .mapNotNull { (path, result) ->
                // Calculate optimal starting value
                findMaximumProfit(network, token, path)?.let {
                    println("Original: ${startAmount}\n" +
                            "Original end: ${result.endAmount}\n" +
                            "New start: ${it.startAmount}\n" +
                            "New end: ${it.endAmount}"
                    )
                    ArbitrageResult(it.startAmount, it.endAmount, result.gas, path)
                }
            }
            .associateWith { gasService.convertGasToToken(network, it.gas, token) }
            .filter { (result, gas) ->
                if (result.profit <= gas) {
                    println("Route ${result.path.formatted()} lost from gas\nProfit: ${result.profit}\n Gas: $gas)")
                }
                result.profit > gas
            }
            .maxByOrNull { (result, gas) -> result.profit - gas }
            ?.key
    }

    data class MaximumProfit(val startAmount: BigInteger, val endAmount: BigInteger)
    private suspend fun findMaximumProfit(
        network: Network,
        token: Token,
        path: GraphPath<Token, GraphEdge>
    ): MaximumProfit? {
        val numItems = (USD_RANGE.second - USD_RANGE.first) / USD_INCREMENT
        val maxPath = findPeakValue(numItems) {
            val amount = priceService.getAmountFromUSD(network, token, it.toBigInteger())
            walkPath(amount, path).endAmount - amount
        }

        if (maxPath == null) {
            println("Could not find maximum path for ${path.formatted()}")
            return null
        }
        val (profit, idx) = maxPath
        val usd = USD_RANGE.first + (idx * USD_INCREMENT)
        val startAmount = priceService.getAmountFromUSD(network, token, usd.toBigInteger())
        val endAmount = startAmount + profit
        return MaximumProfit(startAmount, endAmount)
    }

    data class PathResult(val endAmount: BigInteger, val gas: BigInteger)
    private suspend fun walkPath(amount: BigInteger, path: GraphPath<Token, GraphEdge>): PathResult {
        val initial = PathResult(amount, DEFAULT_GAS)
        return path.edgeList.fold(initial) { previousResult, edge ->
            if (previousResult.endAmount.isZero()) return PathResult(0.toBigInteger(), 0.toBigInteger())

            val quote = path.graph.getEdgeSource(edge)
            val base = path.graph.getEdgeTarget(edge)
            val pair = TokenPair(base, quote)

            val outputAmount = edge.exchange.getOutputAmount(pair, previousResult.endAmount)

            PathResult(outputAmount, previousResult.gas + edge.exchange.estimatedGas)
        }
    }

    private suspend fun getStartingAmount(network: Network, token: Token): BigInteger {
        // TODO: Optimize this?
        return priceService.getAmountFromUSD(network, token, USD_RANGE.first.toBigInteger())
    }
}