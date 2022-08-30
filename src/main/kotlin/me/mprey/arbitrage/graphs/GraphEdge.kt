package me.mprey.arbitrage.graphs

import me.mprey.arbitrage.exchanges.AbstractExchange
import org.jgrapht.graph.DefaultWeightedEdge

class GraphEdge(val exchange: AbstractExchange) : DefaultWeightedEdge()