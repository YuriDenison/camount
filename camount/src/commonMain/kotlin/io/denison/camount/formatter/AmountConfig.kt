package io.denison.camount.formatter

internal class AmountConfig(
  val maximumNotationDigits: Int,
  val decimalSeparator: Char,
  val groupingSeparator: Char,
  val prefix: String,
  val suffix: String,
  val groupingSize: Int,
  val maximumFractionDigits: Int,
) {

  val zero: Char get() = digitChars[0]

  val maximumFormattedSymbols: Int = prefix.length +
    maximumNotationDigits +
    (if (groupingSize == 0) 0 else (maximumNotationDigits - 1) / groupingSize) +
    1 + maximumFractionDigits +
    suffix.length

  fun isDigit(c: Char): Boolean = digitChars.contains(c)
  fun isZero(c: Char): Boolean = zero == c
  fun isInputSeparator(c: Char): Boolean = c == '.' || c == ','
  fun isDecimalSeparator(c: Char): Boolean = decimalSeparator == c
  fun isGroupingSeparator(c: Char): Boolean = groupingSize > 0 && groupingSeparator == c

  fun getDigit(index: Int): Char = digitChars[index]

  private companion object {
    private val digitChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
  }
}
