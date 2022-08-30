package me.mprey.arbitrage.plugins

import io.ktor.application.*
import me.mprey.arbitrage.di.appModule
import org.koin.ktor.ext.Koin
import org.koin.logger.SLF4JLogger

fun Application.configureDI() {
    // Configure Koin
    install(Koin) {
        SLF4JLogger()
        modules(appModule)
    }
}