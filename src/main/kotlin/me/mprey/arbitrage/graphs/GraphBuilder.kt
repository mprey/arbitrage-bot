package me.mprey.arbitrage.graphs

import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.Token
import me.mprey.arbitrage.providers.ExchangeProvider
import org.jgrapht.Graph
import org.jgrapht.graph.DirectedMultigraph

class GraphBuilder(
    private val exchangeProvider: ExchangeProvider
) {

    /**
     * Build a graph representing an Ethereum network.
     * The vertices are tokens, and the edges between them are exchanges
     * which can trade the pair from token A -> token B.
     */
    fun build(network: Network): Graph<Token, GraphEdge> {
        val tokens = network.exchanges
            .flatMap { it.pairs[network.chainId] ?: emptyList() }
            .flatMap { listOf(it.baseToken, it.quoteToken) }
            .distinctBy { it.symbol }

        val graph: Graph<Token, GraphEdge> = DirectedMultigraph(GraphEdge::class.java)
        tokens.forEach { graph.addVertex(it) }

        // Create edges between tokens
        network.exchanges.forEach {
            val exchangeImpl = exchangeProvider.createExchange(network, it)
            val pairs = it.pairs[network.chainId] ?: emptyList()
            pairs.forEach { pair ->
                val source = graph.vertexSet().find { it.symbol == pair.quoteToken.symbol }!!
                val dest = graph.vertexSet().find { it.symbol == pair.baseToken.symbol }!!
                graph.addEdge(source, dest, GraphEdge(exchangeImpl))
            }
        }

        return graph
    }
}