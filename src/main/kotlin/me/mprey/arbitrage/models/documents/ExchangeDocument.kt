package me.mprey.arbitrage.models.documents

import me.mprey.arbitrage.models.ChainId
import me.mprey.arbitrage.models.Exchange
import me.mprey.arbitrage.models.TokenPair
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.newId

data class ExchangeDocument(
    val name: String,
    val pairs: Map<ChainId, List<Id<TokenPairDocument>>>,
    @BsonId
    val id: Id<ExchangeDocument> = newId(),
) {
    suspend fun populate(database: CoroutineDatabase): Exchange {
        val collection = database.getCollection<TokenPairDocument>()
        return Exchange(
            name = name,
            pairs = pairs.mapValues { (_, tokenPairs) ->
                val documents = tokenPairs.mapNotNull { collection.findOne(TokenPairDocument::id eq it) }
                documents.map { it.populate(database) }
            }
        )
    }
}
