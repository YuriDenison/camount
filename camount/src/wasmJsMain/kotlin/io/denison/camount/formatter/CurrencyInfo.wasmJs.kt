@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.denison.camount.formatter

import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsName
import kotlin.js.definedExternally
import kotlin.js.get

private external interface IntlNumberFormatPart : JsAny {
  val type: String
  val value: String
}

@JsName("Intl.NumberFormat")
private external class IntlNumberFormat(
  locales: JsAny? = definedExternally,
  options: JsAny? = definedExternally,
) : JsAny {
  fun formatToParts(value: Double): JsArray<IntlNumberFormatPart>
}

private fun currencyOptions(code: String): JsAny =
  js("({ style: 'currency', currency: code })")

internal actual fun currencyInfo(currencyCode: String): CurrencyInfo {
  val parts = runCatching {
    IntlNumberFormat(options = currencyOptions(currencyCode)).formatToParts(1234.56)
  }.getOrNull() ?: return fallback()

  var decimal = '.'
  var grouping = ','
  var fractionDigits = 2
  val prefix = StringBuilder()
  val suffix = StringBuilder()
  var seenNumber = false

  for (i in 0 until parts.length) {
    val part = parts[i] ?: continue
    when (part.type) {
      "decimal" -> decimal = part.value.firstOrNull() ?: '.'
      "group" -> grouping = part.value.firstOrNull() ?: ','
      "integer" -> seenNumber = true
      "fraction" -> {
        fractionDigits = part.value.length
        seenNumber = true
      }
      "currency", "literal" -> {
        if (seenNumber) suffix.append(part.value) else prefix.append(part.value)
      }
    }
  }

  return CurrencyInfo(
    decimalSeparator = decimal,
    groupingSeparator = grouping,
    prefix = prefix.toString().sanitizeBidi(),
    suffix = suffix.toString().sanitizeBidi(),
    groupingSize = 3,
    maximumFractionDigits = fractionDigits,
  )
}

private fun fallback() = CurrencyInfo(
  decimalSeparator = '.',
  groupingSeparator = ',',
  prefix = "",
  suffix = "",
  groupingSize = 3,
  maximumFractionDigits = 2,
)

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
