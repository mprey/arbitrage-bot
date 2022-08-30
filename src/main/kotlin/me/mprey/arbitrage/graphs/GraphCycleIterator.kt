package me.mprey.arbitrage.graphs

import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.graph.GraphWalk

class GraphCycleIterator<Vertex, Edge>(
    private val graph: Graph<Vertex, Edge>,
    private val minEdges: Int = 2,
    private val maxEdges: Int = 3,
    private val shouldAddEdge: (current: List<Edge>, next: Edge) -> Boolean
) {
    fun computeCycles(start: Vertex): List<GraphPath<Vertex, Edge>> {
        val cycles = computeCyclesRecursive(start, mutableListOf(), start, mutableSetOf(), mutableListOf())
        return cycles.map {
            GraphWalk(graph, start, start, it, 0.0)
        }
    }

    private fun computeCyclesRecursive(
        node: Vertex,
        edges: MutableList<Edge>,
        goal: Vertex,
        visited: MutableSet<Vertex>,
        accum: MutableList<List<Edge>>
    ): List<List<Edge>> {
        if (edges.size >= maxEdges) {
            return accum
        }

        graph.outgoingEdgesOf(node)
            .filter { shouldAddEdge(edges, it) }
            .forEach {
                val destination = graph.getEdgeTarget(it)
                if (goal == destination && edges.size + 1 >= minEdges) {
                    accum.add(edges + listOf(it))
                } else if (!visited.contains(destination)) {
                    visited.add(destination)
                    edges.add(it)
                    computeCyclesRecursive(destination, edges, goal, visited, accum)
                    visited.remove(destination)
                    edges.removeLast()
                }
            }

        return accum
    }
}