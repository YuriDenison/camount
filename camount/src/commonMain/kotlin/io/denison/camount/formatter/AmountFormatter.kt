package io.denison.camount.formatter

import io.denison.camount.Money
import io.denison.camount.absoluteValueNanos
import io.denison.camount.absoluteValueUnits

internal class AmountFormatter(
  private val config: AmountConfig,
  private val withCurrency: Boolean = true,
  private val withGroupingSeparators: Boolean = true,
  private val withFixedFractionLength: Boolean = true,
  private val withFixedZeroNotation: Boolean = true,
) {

  private val fixedFractionPosition = FieldRange()
  private val zeroNotationPosition = FieldRange()
  private val currencySuffixPosition = FieldRange()
  private var cursorPosition = 0

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
        if (length <= config.maximumNotationDigits) this
        else substring(0, config.maximumNotationDigits)
      }

    resultBuffer.append(units)

    if (config.maximumFractionDigits > 0 && money.nanos != 0) {
      val nanos = money.absoluteValueNanos
        .toString()
        .padStart(9, config.zero)
        .run {
          if (length <= config.maximumFractionDigits) this
          else substring(0, config.maximumFractionDigits)
        }

      if (withFixedFractionLength || nanos.any { it != config.zero }) {
        resultBuffer.append(config.decimalSeparator)
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
    } catch (_: DuplicateSeparatorException) {
      cursorPosition = end
      source
    }
  }

  fun cursorPosition(): Int = cursorPosition

  fun fieldPositions(): AmountFieldPositions = AmountFieldPositions(
    cursorPosition = cursorPosition,
    fixedFraction = FieldRange(fixedFractionPosition.beginIndex, fixedFractionPosition.endIndex),
    zeroNotation = FieldRange(zeroNotationPosition.beginIndex, zeroNotationPosition.endIndex),
  )

  fun parse(raw: CharSequence, currencyCode: String): Money {
    var negative = false
    var separator = false
    var hasDigits = false
    val integer = StringBuilder()
    val fractionDigits = StringBuilder()

    for (i in 0 until raw.length) {
      val c = raw[i]
      when {
        c == '-' && !hasDigits -> negative = true
        config.isInputSeparator(c) -> if (!separator) separator = true
        config.isDigit(c) -> {
          hasDigits = true
          if (separator) fractionDigits.append(c) else integer.append(c)
        }
      }
    }

    if (!hasDigits) return Money(0, 0, currencyCode)

    val units = integer.toString().ifEmpty { "0" }.toLongOrNull() ?: 0L

    val nanoDigits = fractionDigits.padEnd(9, '0').substring(0, 9)
    val nanos = nanoDigits.toIntOrNull() ?: 0

    val signedUnits = if (negative) -units else units
    val signedNanos = if (negative) -nanos else nanos
    return Money(signedUnits, signedNanos, currencyCode)
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
            notation.deleteAt(0)
            notation.append(c)
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
            var offset = notationLength - groupLength
            while (offset >= 1) {
              insert(offset, config.groupingSeparator)
              offset -= groupLength
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

    append(config.decimalSeparator)
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

    if (config.prefix.isNotBlank()) {
      insert(0, config.prefix)

      fixedFractionPosition.offset(config.prefix.length)
      zeroNotationPosition.offset(config.prefix.length)
    }

    if (config.suffix.isNotBlank()) {
      savePosition(currencySuffixPosition) {
        append(config.suffix)
      }
    }
  }

  private fun CharSequence.findSelection(selection: Int): Int {
    var count = selection
    var index = length - currencySuffixPosition.length -
      fixedFractionPosition.length - zeroNotationPosition.length

    while (index > 0 && count > 0) {
      val c = this[index - 1]
      if (config.isDecimalSeparator(c) || config.isDigit(c)) count--
      index--
    }
    return index
  }
}

private inline fun StringBuilder.savePosition(
  position: FieldRange,
  block: StringBuilder.() -> Unit,
) {
  position.beginIndex = length
  block()
  position.endIndex = length
}
