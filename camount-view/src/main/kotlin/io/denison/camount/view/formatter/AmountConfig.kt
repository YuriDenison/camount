package io.denison.camount.view.formatter

import androidx.annotation.IntRange
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

internal class AmountConfig(
  @IntRange(from = 1) val maximumNotationDigits: Int,
  decimalFormat: DecimalFormat,
) {

  private val decimalFormatSymbols: DecimalFormatSymbols = decimalFormat.decimalFormatSymbols

  val decimalSeparator: Char get() = decimalSeparatorChars[0]
  val zero: Char get() = digitChars[0]

  val localizedDecimalSeparator: Char = decimalFormatSymbols.monetaryDecimalSeparator
  val localizedGroupingSeparator: Char = decimalFormatSymbols.monetaryGroupingSeparator
  val localizedPrefix: String = decimalFormat.positivePrefix
  val localizedSuffix: String = decimalFormat.positiveSuffix

  val groupingSize: Int = decimalFormat.groupingSize.coerceAtLeast(0)

  val maximumFractionDigits: Int = decimalFormat.maximumFractionDigits

  val maximumFormattedSymbols: Int = localizedPrefix.length +
          maximumNotationDigits +
          (if (groupingSize == 0) 0 else (maximumNotationDigits - 1) / groupingSize) +
          1 + maximumFractionDigits +
          localizedSuffix.length

  fun isDigit(c: Char) = digitChars.contains(c)
  fun isZero(c: Char) = zero == c
  fun isInputSeparator(c: Char) = decimalSeparatorChars.contains(c)
  fun isDecimalSeparator(c: Char) = localizedDecimalSeparator == c
  fun isGroupingSeparator(c: Char) = groupingSize > 0 && localizedGroupingSeparator == c

  fun getDigit(@IntRange(from = 0, to = 9) index: Int) = digitChars[index]

  private companion object {

    private const val decimalSeparatorChars = ".,"
    private val digitChars = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
  }
}
