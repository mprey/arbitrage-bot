package me.mprey.arbitrage

import io.ktor.application.*
import me.mprey.arbitrage.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    configureDI()
    configureRouting()
}
