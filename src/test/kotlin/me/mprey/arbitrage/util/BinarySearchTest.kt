package me.mprey.arbitrage.util

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BinarySearchTest {
    private fun searchData() = arrayOf(
        Arguments.of(
            listOf(-7, -6, -5, 0, 1, -1).bn(),
            1 to 4
        ),
        Arguments.of(
            listOf(1, 2, 3, 4, 5, 6, 10).bn(),
            10 to 6
        ),
        Arguments.of(
            listOf(5, 2, -1, -3, -4, -3).bn(),
            5 to 0
        ),
        Arguments.of(
            listOf(1, 1, 4, 3, 3, 3).bn(),
            4 to 2
        ),
        Arguments.of(
            listOf(-102, -101, -100, -50, -101, -102).bn(),
            -50 to 3
        ),
        Arguments.of(
            listOf(2, 1, 0, -1, -5, -6).bn(),
            2 to 0
        )
    )

    @ParameterizedTest
    @MethodSource("searchData")
    fun testFindPeakValue(items: List<BigInteger>, expected: Pair<Int, Int>) {
        runBlocking {
            val maxIndex = findPeakValue(items.size) { idx -> items[idx] }
            assertEquals(expected.first.toBigInteger() to expected.second, maxIndex)
        }
    }

    private fun List<Int>.bn() = this.map { it.toBigInteger() }
}