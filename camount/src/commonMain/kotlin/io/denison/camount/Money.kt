package io.denison.camount

import androidx.compose.runtime.Immutable
import kotlin.math.absoluteValue

@Immutable
data class Money(
  val units: Long,
  val nanos: Int,
  val currencyCode: String,
) : Comparable<Money> {
  override fun compareTo(other: Money): Int {
    val unitsCmp = units.compareTo(other.units)
    return if (unitsCmp != 0) unitsCmp else nanos.compareTo(other.nanos)
  }

  fun isPositive(): Boolean = units > 0 || (units == 0L && nanos > 0)

  fun isZero(): Boolean = units == 0L && nanos == 0

  companion object {
    fun zero(currencyCode: String): Money = Money(0, 0, currencyCode)
  }
}

internal val Money.absoluteValueUnits: Long get() = units.absoluteValue
internal val Money.absoluteValueNanos: Int get() = nanos.absoluteValue
