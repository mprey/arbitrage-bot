package me.mprey.arbitrage.models.documents

import me.mprey.arbitrage.models.Token
import me.mprey.arbitrage.models.TokenPair
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.newId

data class TokenPairDocument(
    val baseToken: Id<Token>,
    val quoteToken: Id<Token>,
    @BsonId
    val id: Id<TokenPairDocument> = newId(),
) {
    suspend fun populate(database: CoroutineDatabase): TokenPair {
        val tokens = database.getCollection<Token>()
        return TokenPair(
            baseToken = tokens.findOne(Token::id eq baseToken)!!,
            quoteToken = tokens.findOne(Token::id eq quoteToken)!!
        )
    }
}