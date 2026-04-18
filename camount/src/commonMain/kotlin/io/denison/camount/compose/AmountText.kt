package io.denison.camount.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import io.denison.camount.Money
import io.denison.camount.compose.internal.AmountPainter
import io.denison.camount.compose.internal.DiffMode
import io.denison.camount.formatter.AmountConfig
import io.denison.camount.formatter.AmountFieldPositions
import io.denison.camount.formatter.AmountFormatter
import io.denison.camount.formatter.currencyInfo

enum class ShowSign { IfNegative, Always }
enum class FractionPolicy { Fixed, IgnoreZero }

@Composable
fun AmountText(
  amount: Money,
  modifier: Modifier = Modifier,
  style: AmountStyle = defaultAmountStyle(),
  showSign: ShowSign = ShowSign.IfNegative,
  fractionPolicy: FractionPolicy = FractionPolicy.Fixed,
  maximumNotationDigits: Int = 5,
) {
  val measurer = rememberTextMeasurer()
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  val config = remember(amount.currencyCode, maximumNotationDigits) {
    createConfig(amount.currencyCode, maximumNotationDigits)
  }
  val formatter = remember(config, fractionPolicy) {
    AmountFormatter(
      config = config,
      withCurrency = true,
      withGroupingSeparators = true,
      withFixedFractionLength = fractionPolicy == FractionPolicy.Fixed,
      withFixedZeroNotation = true,
    )
  }
  val painter = remember(measurer, scope, config) {
    AmountPainter(
      measurer = measurer,
      style = style,
      mode = DiffMode.Levenshtein,
      config = config,
      scope = scope,
    )
  }

  SideEffect {
    painter.setDensity(density.density)
    painter.updateStyle(style, config)
  }

  val rendered = remember(amount, formatter, showSign) {
    val base = formatter.format(amount).toString()
    when {
      amount.isZero() -> base
      !amount.isPositive() -> "-$base"
      showSign == ShowSign.Always -> "+$base"
      else -> base
    }
  }

  LaunchedEffect(rendered, painter) {
    painter.setText(rendered, AmountFieldPositions.Empty)
  }

  BoxWithConstraints(modifier = modifier) {
    val cw = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else painter.intrinsicWidth
    val ch = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else painter.intrinsicHeight
    SideEffect { painter.setBounds(cw, ch) }
    Canvas(modifier = Modifier.matchParentSize()) {
      painter.draw(this)
    }
  }
}

@Composable
internal fun defaultAmountStyle(): AmountStyle = AmountStyle(
  textStyle = androidx.compose.ui.text.TextStyle.Default,
)

internal fun createConfig(currencyCode: String, maximumNotationDigits: Int): AmountConfig {
  val info = currencyInfo(currencyCode)
  return AmountConfig(
    maximumNotationDigits = maximumNotationDigits,
    decimalSeparator = info.decimalSeparator,
    groupingSeparator = info.groupingSeparator,
    prefix = info.prefix,
    suffix = info.suffix,
    groupingSize = info.groupingSize,
    maximumFractionDigits = info.maximumFractionDigits,
  )
}
