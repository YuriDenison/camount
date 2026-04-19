package io.denison.camount.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import io.denison.camount.Money
import io.denison.camount.compose.internal.AmountPainter
import io.denison.camount.compose.internal.DiffMode
import io.denison.camount.formatter.AmountConfig
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
  alignment: HorizontalAlignment = HorizontalAlignment.Center,
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
  }
  LaunchedEffect(painter, style, config, alignment) {
    painter.updateStyle(style, config, alignment)
  }

  var fieldValue by remember(config) {
    mutableStateOf(TextFieldValue(inputFormatter.format(amount).toString()))
  }

  LaunchedEffect(amount) {
    val current = inputFormatter.parse(fieldValue.text, amount.currencyCode)
    if (current != amount) {
      val next = inputFormatter.format(amount).toString()
      fieldValue = fieldValue.copy(text = next, selection = TextRange(next.length))
    }
  }

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

  Box(
    modifier = modifier
      .onSizeChanged { painter.setBounds(it.width.toFloat(), it.height.toFloat()) },
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      painter.draw(this)
    }

    BasicTextField(
      value = fieldValue,
      onValueChange = { new -> fieldValue = sanitizeInput(new, config) },
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
        .onFocusChanged { focused = it.isFocused },
    )
  }
}

private fun sanitizeInput(
  value: TextFieldValue,
  config: AmountConfig,
): TextFieldValue {
  val src = value.text
  val builder = StringBuilder(src.length)
  var separatorSeen = false
  var integerDigits = 0
  var fractionDigits = 0
  val originalCursor = value.selection.start.coerceIn(0, src.length)
  var mappedCursor = 0
  for (i in src.indices) {
    val c = src[i]
    val kept = when {
      config.isDigit(c) -> {
        val underLimit = if (separatorSeen) {
          fractionDigits < config.maximumFractionDigits
        } else {
          integerDigits < config.maximumNotationDigits
        }
        if (underLimit) {
          builder.append(c)
          if (separatorSeen) fractionDigits++ else integerDigits++
          true
        } else {
          false
        }
      }

      config.isInputSeparator(c) && !separatorSeen && config.maximumFractionDigits > 0 -> {
        separatorSeen = true
        builder.append(config.decimalSeparator)
        true
      }
      else -> false
    }
    if (kept && i < originalCursor) mappedCursor++
  }
  val sanitized = builder.toString()
  if (sanitized == src) return value
  val cursor = mappedCursor.coerceAtMost(sanitized.length)
  return value.copy(text = sanitized, selection = TextRange(cursor))
}
