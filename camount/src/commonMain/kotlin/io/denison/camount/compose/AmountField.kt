package io.denison.camount.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import io.denison.camount.Money
import io.denison.camount.compose.internal.AmountPainter
import io.denison.camount.compose.internal.DiffMode
import io.denison.camount.formatter.AmountFormatter

@Composable
fun AmountField(
  amount: Money,
  onAmountChange: (Money) -> Unit,
  modifier: Modifier = Modifier,
  style: AmountStyle = defaultAmountStyle(),
  maximumNotationDigits: Int = 5,
  enabled: Boolean = true,
  imeAction: ImeAction = ImeAction.Done,
) {
  val measurer = rememberTextMeasurer()
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  val config = remember(amount.currencyCode, maximumNotationDigits) {
    createConfig(amount.currencyCode, maximumNotationDigits)
  }
  val inputFormatter = remember(config) {
    AmountFormatter(
      config = config,
      withCurrency = false,
      withGroupingSeparators = false,
      withFixedFractionLength = false,
      withFixedZeroNotation = true,
    )
  }
  val displayFormatter = remember(config) {
    AmountFormatter(config = config)
  }
  val painter = remember(measurer, scope, config) {
    AmountPainter(
      measurer = measurer,
      style = style,
      mode = DiffMode.Edit,
      config = config,
      scope = scope,
    )
  }

  SideEffect {
    painter.setDensity(density.density)
    painter.updateStyle(style, config)
  }

  var fieldValue by remember(config) {
    mutableStateOf(TextFieldValue(inputFormatter.format(amount).toString()))
  }

  LaunchedEffect(amount) {
    val current = inputFormatter.parseOrZero(fieldValue.text, amount.currencyCode)
    if (current != amount) {
      val next = inputFormatter.format(amount).toString()
      fieldValue = fieldValue.copy(text = next, selection = androidx.compose.ui.text.TextRange(next.length))
    }
  }

  val focusRequester = remember { FocusRequester() }
  var focused by remember { mutableStateOf(false) }

  SideEffect {
    painter.setCursorVisible(focused)
  }

  LaunchedEffect(fieldValue.text, painter, displayFormatter) {
    val cursor = fieldValue.selection.start.coerceAtLeast(0)
    val formatted = displayFormatter.format(
      source = fieldValue.text,
      start = cursor,
      end = fieldValue.text.length,
      text = fieldValue.text,
      textStart = cursor,
      textEnd = fieldValue.text.length,
    )
    painter.setText(formatted, displayFormatter.fieldPositions())
    val newAmount = displayFormatter.parse(fieldValue.text, amount.currencyCode)
    if (newAmount != amount) onAmountChange(newAmount)
  }

  BoxWithConstraints(
    modifier = modifier
      .onFocusChanged { focused = it.isFocused }
      .focusRequester(focusRequester)
      .focusable(enabled, MutableInteractionSource()),
  ) {
    val cw = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else painter.intrinsicWidth
    val ch = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else painter.intrinsicHeight
    SideEffect { painter.setBounds(cw, ch) }

    Canvas(modifier = Modifier.matchParentSize()) {
      painter.draw(this)
    }

    BasicTextField(
      value = fieldValue,
      onValueChange = { new ->
        fieldValue = sanitizeInput(new, config, inputFormatter)
      },
      enabled = enabled,
      textStyle = TextStyle(color = Color.Transparent, fontSize = style.textStyle.fontSize),
      cursorBrush = SolidColor(Color.Transparent),
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Decimal,
        imeAction = imeAction,
      ),
      modifier = Modifier
        .matchParentSize()
        .focusRequester(focusRequester),
    )
  }
}

private fun sanitizeInput(
  value: TextFieldValue,
  config: io.denison.camount.formatter.AmountConfig,
  formatter: AmountFormatter,
): TextFieldValue {
  val builder = StringBuilder(value.text.length)
  var separatorSeen = false
  for (c in value.text) {
    when {
      config.isDigit(c) -> builder.append(c)
      config.isInputSeparator(c) -> if (!separatorSeen) {
        separatorSeen = true
        builder.append(config.decimalSeparator)
      }
    }
  }
  val sanitized = builder.toString()
  return if (sanitized == value.text) value
  else value.copy(
    text = sanitized,
    selection = androidx.compose.ui.text.TextRange(sanitized.length),
  )
}

private fun AmountFormatter.parseOrZero(raw: CharSequence, currencyCode: String): Money =
  parse(raw, currencyCode)
