package me.mprey.arbitrage.util

import me.mprey.arbitrage.models.Token
import java.math.BigInteger

fun roundTokenAmountToDecimals(
    token: Token,
    amount: BigInteger,
    defaultDecimals: Int = 3,
    defaultStablecoinDecimals: Int = 2,
): BigInteger {
    val decimals = if (token.stablecoin) defaultStablecoinDecimals else defaultDecimals
    return amount - (amount % (token.decimals - decimals).toDecimals())
}
