@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.denison.camount.formatter

internal actual fun currencyInfo(currencyCode: String): CurrencyInfo {
  val raw = formatToPartsString(currencyCode)
  var decimal = '.'
  var grouping = ','
  val prefix = StringBuilder()
  val suffix = StringBuilder()
  var fractionDigits = 2
  val groupingSize = 3
  var seenIntegerOrFraction = false

  for ((type, value) in parseTokens(raw)) {
    when (type) {
      "decimal" -> decimal = value.firstOrNull() ?: '.'
      "group" -> grouping = value.firstOrNull() ?: ','
      "integer" -> seenIntegerOrFraction = true
      "fraction" -> {
        fractionDigits = value.length
        seenIntegerOrFraction = true
      }
      "currency", "literal" -> {
        if (seenIntegerOrFraction) suffix.append(value) else prefix.append(value)
      }
    }
  }
  return CurrencyInfo(
    decimalSeparator = decimal,
    groupingSeparator = grouping,
    prefix = prefix.toString().sanitizeBidi(),
    suffix = suffix.toString().sanitizeBidi(),
    groupingSize = groupingSize,
    maximumFractionDigits = fractionDigits,
  )
}

private fun formatToPartsString(currencyCode: String): String = js(
  """
  (function() {
    try {
      var fmt = new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode });
      var parts = fmt.formatToParts(1234.56);
      return parts.map(function(p) { return p.type + '\u0001' + p.value; }).join('\u0002');
    } catch (e) {
      return '';
    }
  })()
  """,
)

private fun parseTokens(raw: String): List<Pair<String, String>> {
  if (raw.isEmpty()) return emptyList()
  val result = ArrayList<Pair<String, String>>()
  for (entry in raw.split('\u0002')) {
    val sep = entry.indexOf('\u0001')
    if (sep < 0) continue
    result += entry.substring(0, sep) to entry.substring(sep + 1)
  }
  return result
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
