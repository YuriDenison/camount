package io.denison.camount.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.denison.camount.Money

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light theme")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark theme")
@Composable
private fun AmountTextPreview() {
  val nightMode = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
  val background = if (nightMode) Color(0xFF121212) else Color.White
  val foreground = if (nightMode) Color(0xFFEDEDED) else Color(0xFF1A1A1A)

  val style = AmountStyle(
    textStyle = TextStyle(
      fontSize = 48.sp,
      color = foreground,
      fontWeight = FontWeight.Medium,
    ),
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(background)
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    AmountText(
      amount = Money(1234, 560_000_000, "EUR"),
      style = style,
      modifier = Modifier.fillMaxWidth().height(64.dp),
    )
    AmountText(
      amount = Money(-42, 0, "USD"),
      style = style,
      showSign = ShowSign.Always,
      modifier = Modifier.fillMaxWidth().height(64.dp),
    )
  }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light theme")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark theme")
@Composable
private fun AmountFieldPreview() {
  val nightMode = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
  val background = if (nightMode) Color(0xFF121212) else Color.White
  val foreground = if (nightMode) Color(0xFFEDEDED) else Color(0xFF1A1A1A)

  val style = AmountStyle(
    textStyle = TextStyle(
      fontSize = 48.sp,
      color = foreground,
      fontWeight = FontWeight.Medium,
    ),
    gradientBrush = Brush.horizontalGradient(
      listOf(Color(0xFF4049FF), Color(0xFFFF4081)),
    ),
    cursor = CursorStyle(color = Color(0xFFFF4081)),
  )

  var amount by remember { mutableStateOf(Money(99, 990_000_000, "USD")) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(background)
      .padding(24.dp),
  ) {
    AmountField(
      amount = amount,
      onAmountChange = { amount = it },
      style = style,
      modifier = Modifier.fillMaxWidth().height(64.dp),
    )
  }
}
