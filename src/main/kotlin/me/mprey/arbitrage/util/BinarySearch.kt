package me.mprey.arbitrage.util

/**
 * Finds the maximum value in an array which contains a "cliff".
 * Meaning, the numbers are increasing for a while until it peaks
 * and then continues downward. We are finding that peak.
 */
suspend fun <T : Comparable<T>> findPeakValue(numItems: Int, valueFetcher: suspend (idx: Int) -> T): Pair<T, Int>? {
    var left = 0
    var right = numItems - 1
    while (left <= right) {
        val mid = (left + right) / 2

        val at = valueFetcher(mid)
        println("Mid $mid value: $at")
        if (mid == 0 || mid == numItems - 1) {
            return at to mid
        }

        val previous = valueFetcher(mid - 1)
        val next = valueFetcher(mid + 1)

        if (previous <= at && at > next) {
            return at to mid
        }

        if (previous >= at) {
            right = mid - 1
        } else {
            left = mid + 1
        }
    }
    return null
}