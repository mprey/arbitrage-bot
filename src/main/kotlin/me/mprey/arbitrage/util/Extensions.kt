package me.mprey.arbitrage.util

import java.math.BigInteger

fun Int.toDecimals(): BigInteger = BigInteger.TEN.pow(this)

fun BigInteger.toDecimals(): BigInteger = BigInteger.TEN.pow(this.toInt())

fun BigInteger.isZero(): Boolean = this == BigInteger.ZERO

fun String.toBytes32(): ByteArray {
    val byteValue = this.toByteArray()
    val byteValueLen32 = ByteArray(32)
    System.arraycopy(byteValue, 0, byteValueLen32, 0, byteValue.size)
    return byteValueLen32
}