package io.denison.camount.view

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.absoluteValue

const val EUR = "EUR"

data class Money(
  val units: Long,
  val nanos: Int = 0,
  val currencyCode: String = EUR,
) : Comparable<Money> {

  override fun compareTo(other: Money): Int {
    val thisNanos = this.units * 1_000_000_000 + this.nanos
    val thatNanos = other.units * 1_000_000_000 + other.nanos
    return when {
      thisNanos > thatNanos -> 1
      thisNanos == thatNanos -> 0
      else -> -1
    }
  }

  fun isPositive() = units > 0 && nanos >= 0 || units >= 0 && nanos > 0
  fun isPositiveOrZero() = units >= 0 && nanos >= 0
  fun isZero() = units == 0L && nanos == 0

  fun toBigDecimal(ignoreZeroNanos: Boolean): BigDecimal = when {
    isZero() && ignoreZeroNanos -> BigDecimal.ZERO
    nanos == 0 && ignoreZeroNanos -> units.toBigDecimal()
    else -> units.toBigDecimal() + nanos.toBigDecimal().movePointLeft(NANOS_SCALE)
  }
}

val MoneyStub = Money(0, 0, "")

fun BigDecimal.toMoney(currencyCode: String, scale: Int = 2): Money {
  val split = divideAndRemainder(BigDecimal.ONE)
  return Money(
    units = split[0].toLong(),
    nanos = split[1]
      .setScale(scale.coerceAtMost(NANOS_SCALE), RoundingMode.HALF_UP)
      .movePointRight(NANOS_SCALE)
      .toInt(),
    currencyCode = currencyCode,
  )
}

internal val Money.absoluteValueUnits: Long get() = units.absoluteValue
internal val Money.absoluteValueNanos: Int get() = nanos.absoluteValue

private const val NANOS_SCALE = 9
