package me.mprey.arbitrage.graphs

import io.mockk.every
import io.mockk.mockk
import me.mprey.arbitrage.exchanges.AbstractExchange
import me.mprey.arbitrage.models.*
import me.mprey.arbitrage.providers.ExchangeProvider
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertContentEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GraphBuilderTest {

    companion object {
        private val BASE_NETWORK = Network(
            name = "Network",
            rateLimit = 0,
            rpcUrl = "",
            chainId = ChainId.AVALANCHE,
            nativeSymbol = "",
            enabled = true,
            exchanges = emptyList(),
            flashLoanable = emptyList()
        )

        private val EXCHANGE_IDX = mapOf(
            "Exchange1" to 0,
            "Exchange2" to 1,
            "Exchange3" to 2
        )
    }

    private val exchangeProvider: ExchangeProvider = mockk()
    private val graphBuilder = GraphBuilder(exchangeProvider)

    private fun networkData() = arrayOf(
        Arguments.of(
            network {
                exchanges {
                    exchange {
                        name { "Exchange1" }
                        pairs {
                            pairChained { ChainId.AVALANCHE to "AVAX/USDC.e" }
                            // Should ignore this one
                            pairChained { ChainId.ETHEREUM to "ETH/USDC.e" }
                        }
                    }
                }
            },
            listOf(
                "AVAX", "USDC.e"
            ),
            listOf(
                "USDC.e->Exchange1->AVAX"
            )
        ),
        Arguments.of(
            network {
                exchanges {
                    exchange {
                        name { "Exchange1" }
                        pairs {
                            pairChained { ChainId.AVALANCHE to "AVAX/USDC.e" }
                        }
                    }

                    exchange {
                        name { "Exchange2" }
                        pairs {
                            pairChained { ChainId.AVALANCHE to "USDC.e/USDT.e" }
                            pairChained { ChainId.AVALANCHE to "AVAX/USDT.e" }
                            pairChained { ChainId.AVALANCHE to "AVAX/USDC.e" }
                        }
                    }
                }
            },
            listOf(
                "AVAX", "USDC.e", "USDT.e"
            ),
            listOf(
                "USDC.e->Exchange1->AVAX",
                "USDT.e->Exchange2->USDC.e",
                "USDT.e->Exchange2->AVAX",
                "USDC.e->Exchange2->AVAX"
            )
        ),
        Arguments.of(
            network {
                exchanges {
                    exchange {
                        name { "Exchange1" }
                        pairs {
                            pair { "AVAX/USDT.e" }
                            pair { "USDT.e/AVAX" }
                        }
                    }
                    exchange {
                        name { "Exchange2" }
                        pairs {
                            pair { "AVAX/USDT.e" }
                            pair { "USDT.e/AVAX" }
                        }
                    }
                    exchange {
                        name { "Exchange3" }
                        pairs {
                            pair { "AVAX/USDT.e" }
                            pair { "USDT.e/AVAX" }
                        }
                    }
                }
            },
            listOf("USDT.e", "AVAX"),
            listOf(
                "AVAX->Exchange1->USDT.e",
                "AVAX->Exchange2->USDT.e",
                "AVAX->Exchange3->USDT.e",
                "USDT.e->Exchange1->AVAX",
                "USDT.e->Exchange2->AVAX",
                "USDT.e->Exchange3->AVAX",
            )
        )
    )

    @BeforeAll
    fun setup() {
        every { exchangeProvider.createExchange(any(), any()) } answers {
            val exchange = mockk<AbstractExchange>()
            every { exchange.index } returns EXCHANGE_IDX[secondArg<Exchange>().name]!!
            every { exchange.hashCode() } returns EXCHANGE_IDX[secondArg<Exchange>().name]!!
            exchange
        }
    }

    @ParameterizedTest
    @MethodSource("networkData")
    fun testBuildingGraph(
        network: Network,
        vertices: List<String>,
        serializedEdges: List<String>
    ) {
        val graph = graphBuilder.build(network)

        assertContentEquals(vertices.sorted(), graph.vertexSet().map { it.symbol }.sorted())

        val actualEdges = graph.vertexSet().flatMap { outer ->
            graph.vertexSet().flatMap { inner ->
                val edges = graph.getAllEdges(outer, inner)
                edges.map { edge ->
                    val exchangeName = EXCHANGE_IDX.entries.find { it.value == edge.exchange.index }!!.key
                    "${outer.symbol}->${exchangeName}->${inner.symbol}"
                }
            }
        }
        assertContentEquals(serializedEdges.sorted(), actualEdges.sorted())
    }

    class PairsListBuilder {
        private val pairs: MutableMap<ChainId, List<TokenPair>> = mutableMapOf()

        fun pairChained(lambda: () -> Pair<ChainId, String>) {
            val (chainId, rawTokenPair) = lambda()
            // AVAX/USDT.e
            val tokenPair = rawTokenPair.split("/")
            val baseToken = tokenPair[0].t()
            val quoteToken = tokenPair[1].t()
            val list = pairs.getOrDefault(chainId, emptyList())
            pairs[chainId] = list + TokenPair(baseToken, quoteToken)
        }

        fun pair(lambda: () -> String) {
            pairChained { ChainId.AVALANCHE to lambda() }
        }

        fun build() = pairs

        private fun String.t() = Token(
            symbol = this,
            name = "Name",
            decimals = 6,
            stablecoin = true,
            addresses = emptyMap()
        )
    }

    private class ExchangeBuilder {
        private var name: String = ""
        private val pairs = mutableMapOf<ChainId, List<TokenPair>>()

        fun name(lambda: () -> String) {
            name = lambda()
        }

        fun pairs(lambda: PairsListBuilder.() -> Unit) {
            pairs.putAll(PairsListBuilder().apply(lambda).build())
        }

        fun build() = Exchange(name, pairs)
    }

    private class ExchangesListBuilder {
        private val exchanges = mutableListOf<Exchange>()

        fun exchange(lambda: ExchangeBuilder.() -> Unit) {
            exchanges.add(ExchangeBuilder().apply(lambda).build())
        }

        fun build() = exchanges
    }

    private class NetworkBuilder {
        private val exchanges = mutableListOf<Exchange>()

        fun exchanges(lambda: ExchangesListBuilder.() -> Unit) {
            exchanges.addAll(ExchangesListBuilder().apply(lambda).build())
        }

        fun build() = BASE_NETWORK.copy(exchanges = exchanges)
    }

    private fun network(lambda: NetworkBuilder.() -> Unit): Network {
        return NetworkBuilder().apply(lambda).build()
    }
}