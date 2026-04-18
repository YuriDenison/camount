package io.denison.camount.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.denison.camount.formatter.AmountField

@Immutable
data class CursorStyle(
  val color: Color,
  val width: Dp = 2.dp,
  val heightFraction: Float = 1.0f,
)

@Immutable
data class AmountStyle(
  val textStyle: TextStyle,
  val gradientBrush: Brush? = null,
  val cursor: CursorStyle? = null,
  val zeroNotationStyle: TextStyle? = null,
  val fixedFractionStyle: TextStyle? = null,
) {
  internal fun styleFor(field: AmountField?): TextStyle = when (field) {
    AmountField.ZeroNotation -> zeroNotationStyle ?: textStyle
    AmountField.FixedFraction -> fixedFractionStyle ?: textStyle
    else -> textStyle
  }
}
