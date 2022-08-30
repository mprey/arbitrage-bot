val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val kmongo_version: String by project
val koin_version: String by project
val kethereum_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
    id("org.web3j") version "4.9.0"
}

web3j {
    generatedPackageName = "me.mprey.arbitrage.{0}"
}

tasks.test {
    useJUnitPlatform()
}

group = "me.mprey.arbitrage"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")

    // Koin core features
    implementation("io.insert-koin:koin-core:$koin_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // KMongo
    implementation("org.litote.kmongo:kmongo:$kmongo_version")
    implementation("org.litote.kmongo:kmongo-coroutine:$kmongo_version")

    // Rate limiting
    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:1.7.1")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.0")
//    implementation("io.github.reactivecircus.cache4k:cache4k:0.5.0")

    // Graphs
    implementation("org.jgrapht:jgrapht-core:1.5.1")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("io.mockk:mockk:1.12.3")
}