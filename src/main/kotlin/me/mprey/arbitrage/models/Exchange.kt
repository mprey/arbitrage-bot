package me.mprey.arbitrage.models

data class Exchange(
    val name: String,
    val pairs: Map<ChainId, List<TokenPair>>,
)
