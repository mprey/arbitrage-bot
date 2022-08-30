package me.mprey.arbitrage.graphs

import io.mockk.InternalPlatformDsl.toStr
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedMultigraph
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertContentEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GraphCycleIteratorTest {

    private fun cycleData() = arrayOf(
        Arguments.of(
            graph {
                edge { "A->Edge1->B" }
                edge { "B->Edge2->A" }
            },
            "A",
            1,
            2,
            { _: List<RelationshipEdge>, _: RelationshipEdge -> true },
            listOf(
                "A->Edge1->B->Edge2->A"
            )
        ),
        Arguments.of(
            graph {
                edge { "A->Edge1->B" }
                edge { "B->Edge2->A" }
            },
            "A",
            1,
            1,
            { _: List<RelationshipEdge>, _: RelationshipEdge -> true },
            emptyList<String>()
        ),
        Arguments.of(
            graph {
                edge { "A->Edge1->B" }
                edge { "A->Edge2->C" }
                edge { "C->Edge3->B" }
                edge { "B->Edge4->C" }
                edge { "C->Edge5->A" }
                edge { "B->Edge6->A" }
            },
            "A",
            2,
            3,
            { _: List<RelationshipEdge>, _: RelationshipEdge -> true },
            listOf(
                "A->Edge1->B->Edge6->A",
                "A->Edge1->B->Edge4->C->Edge5->A",
                "A->Edge2->C->Edge5->A",
                "A->Edge2->C->Edge3->B->Edge6->A"
            )
        ),
        Arguments.of(
            graph {
                edge { "A->Edge1->B" }
                edge { "A->Edge2->C" }
                edge { "C->Edge3->B" }
                edge { "B->Edge4->C" }
                edge { "C->Edge5->A" }
                edge { "B->Edge6->A" }
            },
            "A",
            2,
            3,
            { _: List<RelationshipEdge>, next: RelationshipEdge -> next.label != "Edge4" },
            listOf(
                "A->Edge1->B->Edge6->A",
                "A->Edge2->C->Edge5->A",
                "A->Edge2->C->Edge3->B->Edge6->A"
            )
        ),
        Arguments.of(
            graph {
                edge { "A->Edge1->B" }
                edge { "A->Edge2->C" }
                edge { "C->Edge3->B" }
                edge { "B->Edge4->C" }
                edge { "C->Edge5->A" }
                edge { "B->Edge6->A" }
            },
            "A",
            2,
            3,
            { current: List<RelationshipEdge>, next: RelationshipEdge ->
                current.size < 2 && next.label != "Edge5"
            },
            listOf(
                "A->Edge1->B->Edge6->A"
            )
        )
    )

    @ParameterizedTest
    @MethodSource("cycleData")
    fun computeCycles(
        graph: Graph<String, RelationshipEdge>,
        startVertex: String,
        minEdges: Int,
        maxEdges: Int,
        shouldAddEdge: (current: List<RelationshipEdge>, next: RelationshipEdge) -> Boolean,
        output: List<String>
    ) {
        val graphCycleIterator = GraphCycleIterator(graph, minEdges, maxEdges, shouldAddEdge)
        val actual = graphCycleIterator.computeCycles(startVertex)
        val actualParsed = actual.map { path ->
            val sb = StringBuilder()
            sb.append(path.startVertex)
            path.edgeList.forEach { sb.append("->${it.label}->${graph.getEdgeTarget(it)}") }
            sb.toStr()
        }
        assertContentEquals(output.sorted(), actualParsed.sorted())
    }

    class RelationshipEdge(val label: String) : DefaultEdge() {
        override fun toString() = label
    }

    private class GraphBuilder {
        private val graph: Graph<String, RelationshipEdge> = DirectedMultigraph(RelationshipEdge::class.java)

        fun edge(lambda: () -> String) {
            val parts = lambda().split("->")
            val (source, edge, dest) = parts
            if (!graph.containsVertex(source)) graph.addVertex(source)
            if (!graph.containsVertex(dest)) graph.addVertex(dest)
            graph.addEdge(source, dest, RelationshipEdge(edge))
        }

        fun build() = graph
    }

    private fun graph(lambda: GraphBuilder.() -> Unit): Graph<String, RelationshipEdge> {
        return GraphBuilder().apply(lambda).build()
    }
}