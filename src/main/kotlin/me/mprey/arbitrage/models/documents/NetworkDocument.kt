package me.mprey.arbitrage.models.documents

import me.mprey.arbitrage.models.ChainId
import me.mprey.arbitrage.models.Network
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

data class NetworkDocument(
    val name: String,
    val rateLimit: Int,
    val rpcUrl: String,
    val chainId: ChainId,
    val nativeSymbol: String,
    val enabled: Boolean,
    val exchanges: List<Id<ExchangeDocument>>,
    val flashLoanable: List<String> // List of token symbols we can flash loan
) {
    suspend fun populate(database: CoroutineDatabase): Network {
        val collection = database.getCollection<ExchangeDocument>()
        return Network(
            name = name,
            rateLimit = rateLimit,
            rpcUrl = rpcUrl,
            chainId = chainId,
            nativeSymbol = nativeSymbol,
            enabled = enabled,
            exchanges = exchanges.mapNotNull { collection.findOne(ExchangeDocument::id eq it) }.map { it.populate(database) },
            flashLoanable = flashLoanable
        )
    }
}
