package io.denison.camount.formatter

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal actual fun currencyInfo(currencyCode: String): CurrencyInfo {
  val locale = Locale.getDefault()
  val format = NumberFormat.getCurrencyInstance(locale) as? DecimalFormat
    ?: (NumberFormat.getNumberInstance(locale) as DecimalFormat)
  val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
  if (currency != null) {
    runCatching { format.currency = currency }
  }

  val symbols = format.decimalFormatSymbols
  return CurrencyInfo(
    decimalSeparator = symbols.monetaryDecimalSeparator,
    groupingSeparator = symbols.monetaryGroupingSeparator,
    prefix = format.positivePrefix.sanitizeBidi(),
    suffix = format.positiveSuffix.sanitizeBidi(),
    groupingSize = format.groupingSize.coerceAtLeast(0),
    maximumFractionDigits = format.maximumFractionDigits,
  )
}

private fun String.sanitizeBidi(): String = buildString(length) {
  for (c in this@sanitizeBidi) {
    when (c.code) {
      0x200E, 0x200F, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
      0x2066, 0x2067, 0x2068, 0x2069,
      -> Unit
      else -> append(c)
    }
  }
}
