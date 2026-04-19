package io.denison.camount.view.formatter

import android.util.Log
import io.denison.camount.view.Money
import io.denison.camount.view.absoluteValueNanos
import io.denison.camount.view.absoluteValueUnits
import io.denison.camount.view.internal.clear
import io.denison.camount.view.internal.length
import io.denison.camount.view.internal.offset
import java.math.BigDecimal
import java.text.FieldPosition
import java.text.Format

internal class AmountFormatter(
  private val config: AmountConfig,
  private val withCurrency: Boolean = true,
  private val withGroupingSeparators: Boolean = true,
  private val withFixedFractionLength: Boolean = true,
  private val withFixedZeroNotation: Boolean = true,
) {

  fun parse(raw: CharSequence): BigDecimal? {
    val sanitized = buildString {
      raw.forEach {
        when {
          config.isDecimalSeparator(it) -> append(config.decimalSeparator)
          config.isDigit(it) -> append(it)
        }
      }
    }
    return if (sanitized.isBlank()) {
      null
    } else {
      runCatching { BigDecimal(sanitized) }
        .onFailure { Log.e("AmountFormatter", "Error parse $raw", it) }
        .getOrNull()
    }
  }

  val fixedFractionPosition = FieldPosition(Field.FixedFraction)
  val zeroNotationPosition = FieldPosition(Field.ZeroNotation)
  val currencySuffixPosition = FieldPosition(Field.CurrencySuffix)
  var cursorPosition = 0
    private set

  private val notation = StringBuilder(config.maximumNotationDigits)
  private var separatorFound = false
  private val fraction = StringBuilder(config.maximumFractionDigits)
  private val resultBuffer = StringBuilder(config.maximumFormattedSymbols)

  private fun reset() {
    notation.clear()
    separatorFound = false
    fraction.clear()
    resultBuffer.clear()
    fixedFractionPosition.clear()
    zeroNotationPosition.clear()
    currencySuffixPosition.clear()
    cursorPosition = 0
  }

  fun format(money: Money): CharSequence {
    reset()

    val units = money.absoluteValueUnits
      .toString()
      .run {
        if (length <= config.maximumNotationDigits) {
          this
        } else {
          substring(0, config.maximumNotationDigits)
        }
      }

    resultBuffer.append(units)

    if (config.maximumFractionDigits > 0 && money.nanos != 0) {
      val nanos = money.absoluteValueNanos
        .toString()
        .padStart(9, config.zero)
        .run {
          if (length <= config.maximumFractionDigits) {
            this
          } else {
            substring(0, config.maximumFractionDigits)
          }
        }

      if (withFixedFractionLength || nanos.any { it != config.zero }) {
        resultBuffer.append(config.localizedDecimalSeparator)
        resultBuffer.append(nanos)
      }
    }

    resultBuffer.appendCurrency()
    return resultBuffer.toString()
  }

  fun format(
    source: CharSequence,
    start: Int,
    end: Int,
    text: CharSequence,
    textStart: Int,
    textEnd: Int,
  ): CharSequence {
    reset()

    return try {
      append(source, 0, start)
      append(text, textStart, textEnd, withInputSeparator = true)
      val afterChangeCount = append(source, end, source.length)
      result().also {
        cursorPosition = it.findSelection(afterChangeCount)
      }
    } catch (e: DuplicateSeparatorException) {
      cursorPosition = end
      source
    }
  }

  private fun append(
    source: CharSequence,
    start: Int,
    end: Int,
    withInputSeparator: Boolean = false,
  ): Int {
    var count = 0
    for (index in start until end) {
      val c = source[index]
      count += when {
        withInputSeparator && config.isInputSeparator(c) -> ensureSeparator()
        !withInputSeparator && config.isDecimalSeparator(c) -> ensureSeparator()
        config.isDigit(c) -> appendDigit(c)
        else -> 0
      }
    }
    return count
  }

  private fun ensureSeparator(): Int {
    if (separatorFound) throw DuplicateSeparatorException()
    separatorFound = true
    return 1
  }

  private fun appendDigit(c: Char): Int {
    if (separatorFound) {
      if (fraction.length < config.maximumFractionDigits) {
        fraction.append(c)
        return 1
      }
    } else {
      if (notation.length < config.maximumNotationDigits) {
        if (notation.length == 1 && config.isZero(notation[0])) {
          if (!config.isZero(c)) {
            notation.replace(0, 1, c.toString())
          }
        } else {
          notation.append(c)
          return 1
        }
      }
    }
    return 0
  }

  private class DuplicateSeparatorException : RuntimeException()

  private fun result(): CharSequence {
    resultBuffer.appendNotation()
    resultBuffer.appendFraction()
    resultBuffer.appendCurrency()
    return resultBuffer.toString()
  }

  private fun StringBuilder.appendNotation() {
    if (notation.isNotEmpty()) {
      append(notation)

      if (withGroupingSeparators) {
        val groupLength = config.groupingSize
        if (groupLength > 0) {
          val notationLength = notation.length
          if (notationLength > groupLength) {
            for (offset in (notationLength - groupLength) downTo 1 step groupLength) {
              insert(offset, config.localizedGroupingSeparator)
            }
          }
        }
      }
    } else if (withFixedZeroNotation) {
      savePosition(zeroNotationPosition) {
        append(config.zero)
      }
    }
  }

  private fun StringBuilder.appendFraction() {
    if (!separatorFound) return

    val maximumFractionDigits = config.maximumFractionDigits
    if (maximumFractionDigits <= 0) return

    zeroNotationPosition.clear()

    if (isEmpty()) append(config.zero)

    append(config.localizedDecimalSeparator)
    append(fraction)

    if (withFixedFractionLength) {
      savePosition(fixedFractionPosition) {
        repeat(maximumFractionDigits - fraction.length) {
          append(config.zero)
        }
      }
    }
  }

  private fun StringBuilder.appendCurrency() {
    if (!withCurrency) return

    if (config.localizedPrefix.isNotBlank()) {
      insert(0, config.localizedPrefix)

      fixedFractionPosition.offset(config.localizedPrefix.length)
      zeroNotationPosition.offset(config.localizedPrefix.length)
    }

    if (config.localizedSuffix.isNotBlank()) {
      savePosition(currencySuffixPosition) {
        append(config.localizedSuffix)
      }
    }
  }

  private fun CharSequence.findSelection(selection: Int): Int {
    var count = selection
    var index =
      length - currencySuffixPosition.length() - fixedFractionPosition.length() - zeroNotationPosition.length()

    while (index > 0 && count > 0) {
      val c = this[index - 1]
      if (config.isDecimalSeparator(c) || config.isDigit(c)) count--
      index--
    }
    return index
  }

  sealed class Field(name: String) : Format.Field(name) {
    object FixedFraction : Field("FixedFraction")
    object ZeroNotation : Field("ZeroNotation")
    object CurrencySuffix : Field("CurrencySuffix")
  }
}

private inline fun StringBuilder.savePosition(
  position: FieldPosition,
  block: StringBuilder.() -> Unit,
) {
  position.beginIndex = length
  block()
  position.endIndex = length
}
