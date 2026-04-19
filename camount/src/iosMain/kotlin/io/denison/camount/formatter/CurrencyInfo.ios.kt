package io.denison.camount.formatter

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCurrencyCode
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.localeIdentifierFromComponents

internal actual fun currencyInfo(currencyCode: String): CurrencyInfo {
  val localeId = NSLocale.localeIdentifierFromComponents(
    mapOf<Any?, Any?>(NSLocaleCurrencyCode to currencyCode),
  )
  val formatter = NSNumberFormatter().apply {
    numberStyle = NSNumberFormatterCurrencyStyle
    locale = NSLocale(localeIdentifier = localeId)
    this.currencyCode = currencyCode
  }

  val decimal = formatter.currencyDecimalSeparator.firstOrNull() ?: '.'
  val grouping = formatter.currencyGroupingSeparator.firstOrNull() ?: ','
  return CurrencyInfo(
    decimalSeparator = decimal,
    groupingSeparator = grouping,
    prefix = formatter.positivePrefix.sanitizeBidi(),
    suffix = formatter.positiveSuffix.sanitizeBidi(),
    groupingSize = formatter.groupingSize.toInt().coerceAtLeast(0),
    maximumFractionDigits = formatter.maximumFractionDigits.toInt(),
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
