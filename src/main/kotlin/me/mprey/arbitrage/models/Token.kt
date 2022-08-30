package me.mprey.arbitrage.models

import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Token(
    val symbol: String,
    val name: String,
    val decimals: Int,
    val stablecoin: Boolean,
    val addresses: Map<ChainId, String>,
    val oracles: Map<ChainId, String> = emptyMap(),
    @BsonId
    val id: Id<Token> = newId()
)
