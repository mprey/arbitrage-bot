package me.mprey.arbitrage.models

data class Network(
    val name: String,
    val rateLimit: Int,
    val rpcUrl: String,
    val chainId: ChainId,
    val nativeSymbol: String,
    val enabled: Boolean,
    val exchanges: List<Exchange>,
    val flashLoanable: List<String> // List of token symbols we can flash loan
)
