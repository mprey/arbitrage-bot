package me.mprey.arbitrage.managers

import me.mprey.arbitrage.graphs.GraphEdge
import me.mprey.arbitrage.models.Token
import org.jgrapht.GraphPath
import java.math.BigInteger

data class ArbitrageResult(
    val startAmount: BigInteger,
    val endAmount: BigInteger,
    val gas: BigInteger,
    val path: GraphPath<Token, GraphEdge>
) {
    val profit get() = endAmount - startAmount

    override fun toString() = listOf(
        "Route ${path.formatted()} is profitable",
        "Profit: $profit",
        "Start amount: $startAmount",
        "End amount: $endAmount",
        "Gas: $gas"
    ).joinToString("\n")
}

fun GraphPath<Token, GraphEdge>.formatted(): String {
    val sb = StringBuilder()
    this.edgeList.forEach {
        val src = graph.getEdgeSource(it)
        val target = graph.getEdgeTarget(it)
        sb.append("${src.symbol}->${it.exchange::class.simpleName}->${target.symbol}-> ")
    }
    return sb.toString()
}