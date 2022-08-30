package me.mprey.arbitrage.di

import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import me.mprey.arbitrage.graphs.GraphBuilder
import me.mprey.arbitrage.managers.ArbitrageManager
import me.mprey.arbitrage.models.Network
import me.mprey.arbitrage.models.Token
import me.mprey.arbitrage.models.documents.NetworkDocument
import me.mprey.arbitrage.models.documents.TokenPairDocument
import me.mprey.arbitrage.providers.ExchangeProvider
import me.mprey.arbitrage.services.GasService
import me.mprey.arbitrage.services.PriceService
import org.koin.dsl.module
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider

val appModule = module {
    val dotenv = dotenv()
    // MongoDB connection
    single {
        val client = KMongo.createClient(dotenv["MONGODB_URI"]).coroutine
        client.getDatabase(dotenv["MONGODB_DB"])
    }

    single<ContractGasProvider> { DefaultGasProvider() } // Useless cast
    single { Credentials.create(dotenv["PRIVATE_KEY"]) }

    single { GasService(get(), get(), get(), get()) }
    single { PriceService(get(), get(), get()) }

    single {
        runBlocking {
            val database: CoroutineDatabase = get()
            database.getCollection<NetworkDocument>()
                .find()
                .toList()
                .map { it.populate(database) }
        }
    }

    // RPC provider for each network
    single {
        runBlocking {
            val networks: List<Network> = get()
            networks.associateWith {
                Web3j.build(HttpService(it.rpcUrl))
            }
        }
    }

    single { ExchangeProvider(get(), get(), get()) }
    single { GraphBuilder(get()) }

    // Start arbitrage
    single(createdAtStart = true) { ArbitrageManager(get(), get(), get(), get(), get()) }

    // Code for adding things to the database. You can ignore this
    single(createdAtStart = false) {
        val db: CoroutineDatabase = get()

        runBlocking {
            val usdte = db.getCollection<Token>().findOne(Token::symbol eq "USDT.e")!!
            val usdce = db.getCollection<Token>().findOne(Token::symbol eq "USDC.e")!!
            val daie = db.getCollection<Token>().findOne(Token::symbol eq "DAI.e")!!
            val usdt = db.getCollection<Token>().findOne(Token::symbol eq "USDT")!!
            val usdc = db.getCollection<Token>().findOne(Token::symbol eq "USDC")!!
            val avax = db.getCollection<Token>().findOne(Token::symbol eq "AVAX")!!
            val tokens = listOf(usdte, usdce, daie, usdt, usdc, avax)
            val fp: suspend (base: String, quote: String) -> Id<TokenPairDocument> = { b, q ->
                val base = tokens.find { it.symbol == b }
                val quote = tokens.find { it.symbol == q }
                val pair = db.getCollection<TokenPairDocument>().findOne(
                    TokenPairDocument::baseToken eq base!!.id,
                    TokenPairDocument::quoteToken eq quote!!.id
                )!!
                pair.id
            }
            println(fp("DAI.e", "USDT"))
            println(fp("USDT", "DAI.e"))
//
//            val dexalot = ExchangeDocument(
//                name = "Dexalot",
//                pairs = mapOf(
//                    ChainId.AVALANCHE to listOf(
//                        fp("AVAX", "USDC.e"),
//                        fp("USDC.e", "AVAX"),
//                        fp("USDT.e", "AVAX"),
//                        fp("AVAX", "USDT.e")
//                    )
//                )
//            )
//
//            val traderJoe = ExchangeDocument(
//                name = "TraderJoe",
//                pairs = mapOf(
//                    ChainId.AVALANCHE to listOf(
//                        fp("USDC", "DAI.e"),
//                        fp("USDC.e", "DAI.e"),
//                        fp("USDT.e", "DAI.e"),
//                        fp("USDT", "DAI.e"),
//                        fp("AVAX", "DAI.e"),
//
//                        // From USDC
//                        fp("DAI.e", "USDC"),
//                        fp("USDC.e", "USDC"),
//                        fp("USDT.e", "USDC"),
//                        fp("USDT", "USDC"),
//                        fp("AVAX", "USDC"),
//
//                        // From USDC.e
//                        fp("DAI.e", "USDC.e"),
//                        fp("USDC", "USDC.e"),
//                        fp("USDT.e", "USDC.e"),
//                        fp("USDT", "USDC.e"),
//                        fp("AVAX", "USDC.e"),
//
//                        // From USDT
//                        fp("DAI.e", "USDT"),
//                        fp("USDC", "USDT"),
//                        fp("USDC.e", "USDT"),
//                        fp("USDT.e", "USDT"),
//                        fp("AVAX", "USDT"),
//
//                        // From USDT.e
//                        fp("DAI.e", "USDT.e"),
//                        fp("USDC", "USDT.e"),
//                        fp("USDC.e", "USDT.e"),
//                        fp("USDT", "USDT.e"),
//                        fp("AVAX", "USDT.e"),
//
//                        // From AVAX
//                        fp("DAI.e", "AVAX"),
//                        fp("USDC.e", "AVAX"),
//                        fp("USDC", "AVAX"),
//                        fp("USDT.e", "AVAX"),
//                        fp("USDT", "AVAX"),
//                    )
//                )
//            )
//
//            val platypus = ExchangeDocument(
//                name = "Platypus",
//                pairs = mapOf(
//                    ChainId.AVALANCHE to listOf(
//                         fp("USDC", "DAI.e"),
//                         fp("USDC.e", "DAI.e"),
//                         fp("USDT.e", "DAI.e"),
//                         fp("USDT", "DAI.e"),
//
//                         // From USDC
//                         fp("DAI.e", "USDC"),
//                         fp("USDC.e", "USDC"),
//                         fp("USDT.e", "USDC"),
//                         fp("USDT", "USDC"),
//
//                         // From USDC.e
//                         fp("DAI.e", "USDC.e"),
//                         fp("USDC", "USDC.e"),
//                         fp("USDT.e", "USDC.e"),
//                         fp("USDT", "USDC.e"),
//
//                         // From USDT
//                         fp("DAI.e", "USDT"),
//                         fp("USDC", "USDT"),
//                         fp("USDC.e", "USDT"),
//                         fp("USDT.e", "USDT"),
//
//                         // From USDT.e
//                         fp("DAI.e", "USDT.e"),
//                         fp("USDC", "USDT.e"),
//                         fp("USDC.e", "USDT.e"),
//                         fp("USDT", "USDT.e"),
//                    )
//                )
//            )
//
//            db.getCollection<ExchangeDocument>().save(dexalot)
//            db.getCollection<ExchangeDocument>().save(traderJoe)
//            db.getCollection<ExchangeDocument>().save(platypus)
//
////            tokens.forEach { outer ->
////                tokens.forEach { inner ->
////                    if (outer.name != inner.name) {
////                        val existingPair = db.getCollection<TokenPairDocument>().findOne(
////                            TokenPairDocument::baseToken eq outer.id,
////                            TokenPairDocument::quoteToken eq inner.id
////                        )
////
////                        if (existingPair == null) {
////                            println("Adding token pair")
////                            val newTokenPair = TokenPairDocument(outer.id, inner.id)
////                            db.getCollection<TokenPairDocument>().save(newTokenPair)
////                        }
////                    }
////                }
////            }
        }

        1
    }
}