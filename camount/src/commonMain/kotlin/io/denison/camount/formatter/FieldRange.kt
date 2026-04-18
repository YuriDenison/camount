package io.denison.camount.formatter

internal data class FieldRange(
  var beginIndex: Int = 0,
  var endIndex: Int = 0,
) {
  val length: Int get() = if (isValid()) endIndex - beginIndex else 0

  fun isValid(): Boolean = beginIndex < endIndex && beginIndex >= 0

  fun clear() {
    beginIndex = 0
    endIndex = 0
  }

  fun offset(offset: Int) {
    if (isValid()) {
      beginIndex += offset
      endIndex += offset
    }
  }

  operator fun contains(index: Int): Boolean = index in beginIndex until endIndex
}

enum class AmountField {
  FixedFraction,
  ZeroNotation,
  CurrencySuffix,
}

class AmountFieldPositions internal constructor(
  val cursorPosition: Int,
  internal val fixedFraction: FieldRange,
  internal val zeroNotation: FieldRange,
) {
  companion object {
    val Empty: AmountFieldPositions = AmountFieldPositions(
      cursorPosition = -1,
      fixedFraction = FieldRange(),
      zeroNotation = FieldRange(),
    )
  }
}
