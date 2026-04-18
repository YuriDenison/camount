package io.denison.camount.view.drawable

import androidx.annotation.ColorInt
import io.denison.camount.view.formatter.AmountFormatter
import java.text.FieldPosition

internal class AmountDrawableStyle(
  val gravity: Int,
  @ColorInt val gradientStartColor: Int,
  @ColorInt val gradientEndColor: Int,
  val gradientOrientation: AmountGradientOrientation,
  val cursorStyle: CursorStyle?,
  val defaultSymbolStyle: SymbolStyle,
  val symbolStyles: Map<AmountFormatter.Field, SymbolStyle>,
)

internal enum class AmountDrawableAnimation {
  EDIT,
  LEVENSHTEIN,
}

internal enum class AmountGradientOrientation {
  VERTICAL,
  HORIZONTAL,
}

internal class AmountFieldPositions(
  val cursorPosition: Int,
  val fixedFractionPosition: FieldPosition,
  val zeroNotationPosition: FieldPosition,
)
