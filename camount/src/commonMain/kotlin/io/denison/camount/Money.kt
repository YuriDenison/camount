package io.denison.camount

import kotlin.math.absoluteValue

data class Money(
  val units: Long,
  val nanos: Int,
  val currencyCode: String,
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

  fun isPositive(): Boolean = units > 0 && nanos >= 0 || units >= 0 && nanos > 0
  fun isZero(): Boolean = units == 0L && nanos == 0

  companion object {
    fun zero(currencyCode: String): Money = Money(0, 0, currencyCode)
  }
}

internal val Money.absoluteValueUnits: Long get() = units.absoluteValue
internal val Money.absoluteValueNanos: Int get() = nanos.absoluteValue
