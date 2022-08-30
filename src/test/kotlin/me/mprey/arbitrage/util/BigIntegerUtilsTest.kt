package me.mprey.arbitrage.util

import me.mprey.arbitrage.models.Token
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BigIntegerUtilsTest {

    companion object {
        val DEFAULT_TOKEN = Token(
            symbol = "",
            name = "",
            decimals = 18,
            stablecoin = false,
            addresses = emptyMap(),
            oracles = emptyMap()
        )
    }

    private fun roundData() = arrayOf(
        Arguments.of(
            DEFAULT_TOKEN,
            BigInteger.valueOf(1111110000000000000),
            1,
            BigInteger.valueOf(1100000000000000000),
        ),
        Arguments.of(
            DEFAULT_TOKEN.copy(stablecoin = true, decimals = 6),
            BigInteger.valueOf(100123456),
            3,
            BigInteger.valueOf(100123000)
        )
    )

    @ParameterizedTest
    @MethodSource("roundData")
    fun testRoundTokenAmountToDecimals(
        token: Token,
        amount: BigInteger,
        decimals: Int,
        expected: BigInteger
    ) {
        val actual = roundTokenAmountToDecimals(token, amount, decimals, decimals)
        assertEquals(expected, actual)
    }
}