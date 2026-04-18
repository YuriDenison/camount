package io.denison.camount.view.internal

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal fun defaultDecimalFormat(currencyCode: String): DecimalFormat {
  val locale = Locale.getDefault()
  val format = NumberFormat.getCurrencyInstance(locale) as? DecimalFormat
    ?: (NumberFormat.getNumberInstance(locale) as DecimalFormat)
  val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
  if (currency != null) {
    runCatching { format.currency = currency }
  }
  return format
}
