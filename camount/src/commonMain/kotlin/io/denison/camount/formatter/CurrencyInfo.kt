package io.denison.camount.formatter

internal class CurrencyInfo(
  val decimalSeparator: Char,
  val groupingSeparator: Char,
  val prefix: String,
  val suffix: String,
  val groupingSize: Int,
  val maximumFractionDigits: Int,
)

internal expect fun currencyInfo(currencyCode: String): CurrencyInfo
