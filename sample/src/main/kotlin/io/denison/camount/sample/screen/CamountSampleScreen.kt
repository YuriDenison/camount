package io.denison.camount.sample.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.denison.camount.Money
import io.denison.camount.sample.R
import io.denison.camount.compose.AmountField
import io.denison.camount.compose.AmountStyle
import io.denison.camount.compose.AmountText
import io.denison.camount.compose.CursorStyle
import io.denison.camount.compose.FractionPolicy
import io.denison.camount.compose.ShowSign
import kotlin.random.Random

private val currencies = listOf(
  "EUR" to "Euro",
  "USD" to "US Dollar",
  "GBP" to "British Pound",
  "JPY" to "Japanese Yen",
  "CHF" to "Swiss Franc",
  "CAD" to "Canadian Dollar",
  "AUD" to "Australian Dollar",
  "SEK" to "Swedish Krona",
  "NOK" to "Norwegian Krone",
  "PLN" to "Polish Zloty",
  "CNY" to "Chinese Yuan",
  "INR" to "Indian Rupee",
)

private val Manrope = FontFamily(Font(R.font.manrope_medium, FontWeight.Medium))

private val Accent = Color(0xFF4049FF)
private val AccentAlt = Color(0xFFFF4081)
private val Ink = Color(0xFF0F1024)
private val InkMuted = Color(0xFF5A5E80)
private val Canvas = Color(0xFFF7F7FB)
private val FieldSurface = Color(0xFFEEEFF6)
private val Placeholder = Color(0xFFB5B8CC)

@Composable
fun CamountSampleScreen(
  modifier: Modifier = Modifier,
  viewSection: (@Composable (Money, (Money) -> Unit) -> Unit)? = null,
) {
  var money by remember { mutableStateOf(Money(1234, 560_000_000, "EUR")) }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = Canvas,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Header()
      ControlsCard(money = money, onMoneyChange = { money = it })

      if (viewSection != null) {
        SectionCard(title = "View", subtitle = "classic Android") {
          viewSection(money) { money = it }
        }
      }

      SectionCard(title = "AmountText", subtitle = "Compose") {
        AmountTextSection(money = money)
      }

      SectionCard(title = "AmountField", subtitle = "Compose") {
        AmountFieldSection(money = money, onMoneyChange = { money = it })
      }

      Spacer(Modifier.height(8.dp))
    }
  }
}

@Composable
private fun Header() {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      "Camount",
      style = TextStyle(
        fontFamily = Manrope,
        fontSize = 34.sp,
        color = Ink,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
      ),
    )
    Text(
      "Every widget below shares the same Money — change any, watch the rest animate in sync.",
      style = TextStyle(fontFamily = Manrope, fontSize = 14.sp, color = InkMuted),
    )
  }
}

@Composable
private fun ControlsCard(money: Money, onMoneyChange: (Money) -> Unit) {
  var pickerOpen by remember { mutableStateOf(false) }
  ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      CurrencyDropdown(
        selected = money.currencyCode,
        onClick = { pickerOpen = true },
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ActionButton(
          label = "Shuffle",
          modifier = Modifier.weight(1f),
          onClick = { onMoneyChange(randomMoney(money.currencyCode)) },
        )
        PlusOneButton(
          modifier = Modifier.weight(1f),
          onClick = { onMoneyChange(money.copy(units = money.units + 1)) },
        )
        ResetButton(
          modifier = Modifier.weight(1f),
          onClick = { onMoneyChange(Money.zero(money.currencyCode)) },
        )
      }
    }
  }

  if (pickerOpen) {
    CurrencyPickerSheet(
      selected = money.currencyCode,
      onSelect = {
        onMoneyChange(money.copy(currencyCode = it))
        pickerOpen = false
      },
      onDismiss = { pickerOpen = false },
    )
  }
}

@Composable
private fun CurrencyDropdown(selected: String, onClick: () -> Unit) {
  val name = currencies.firstOrNull { it.first == selected }?.second ?: selected
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .clip(RoundedCornerShape(14.dp))
      .border(1.dp, Color(0xFFE0E1EC), RoundedCornerShape(14.dp)),
    color = Color.White,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        selected,
        style = TextStyle(fontFamily = Manrope, fontSize = 15.sp, color = Ink, fontWeight = FontWeight.Bold),
      )
      Text(
        name,
        style = TextStyle(fontFamily = Manrope, fontSize = 14.sp, color = InkMuted, fontWeight = FontWeight.Medium),
        modifier = Modifier.weight(1f),
      )
      Text(
        "▾",
        style = TextStyle(fontFamily = Manrope, fontSize = 16.sp, color = InkMuted),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
  selected: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor = Color.White,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
    ) {
      Text(
        "Choose currency",
        style = TextStyle(fontFamily = Manrope, fontSize = 18.sp, color = Ink, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
      currencies.forEach { (code, name) ->
        CurrencyRow(
          code = code,
          name = name,
          selected = code == selected,
          onClick = {
            scope.launch {
              sheetState.hide()
              onSelect(code)
            }
          },
        )
      }
    }
  }
}

@Composable
private fun CurrencyRow(code: String, name: String, selected: Boolean, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = if (selected) Color(0xFFEFF0FF) else Color.Transparent,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        code,
        style = TextStyle(
          fontFamily = Manrope,
          fontSize = 15.sp,
          color = if (selected) Accent else Ink,
          fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier.width(48.dp),
      )
      Text(
        name,
        style = TextStyle(fontFamily = Manrope, fontSize = 15.sp, color = Ink, fontWeight = FontWeight.Medium),
        modifier = Modifier.weight(1f),
      )
      if (selected) {
        Text(
          "✓",
          style = TextStyle(fontFamily = Manrope, fontSize = 18.sp, color = Accent, fontWeight = FontWeight.Bold),
        )
      }
    }
  }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  FilledTonalButton(
    onClick = onClick,
    modifier = modifier.height(44.dp),
    shape = RoundedCornerShape(14.dp),
  ) {
    Text(label, style = TextStyle(fontFamily = Manrope, fontSize = 14.sp, fontWeight = FontWeight.Medium))
  }
}

@Composable
private fun PlusOneButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
  FilledIconButton(
    onClick = onClick,
    modifier = modifier.height(44.dp),
    shape = RoundedCornerShape(14.dp),
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = Accent,
      contentColor = Color.White,
    ),
  ) {
    Text("+1", style = TextStyle(fontFamily = Manrope, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
  }
}

@Composable
private fun ResetButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
  Surface(
    modifier = modifier
      .height(44.dp)
      .clip(RoundedCornerShape(14.dp))
      .border(1.dp, Color(0xFFE0E1EC), RoundedCornerShape(14.dp)),
    color = Color.White,
    onClick = onClick,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        "Reset",
        style = TextStyle(fontFamily = Manrope, fontSize = 14.sp, color = Ink, fontWeight = FontWeight.Medium),
      )
    }
  }
}

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
  ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          title,
          style = TextStyle(fontFamily = Manrope, fontSize = 18.sp, color = Ink, fontWeight = FontWeight.SemiBold),
        )
        Text(
          subtitle,
          style = TextStyle(fontFamily = Manrope, fontSize = 12.sp, color = InkMuted, fontWeight = FontWeight.Medium),
          modifier = Modifier.padding(bottom = 2.dp),
        )
      }
      content()
    }
  }
}

@Composable
private fun AmountTextSection(money: Money) {
  val big = remember {
    AmountStyle(
      textStyle = TextStyle(fontFamily = Manrope, fontSize = 44.sp, color = Ink, fontWeight = FontWeight.Bold),
    )
  }
  val mid = remember {
    AmountStyle(
      textStyle = TextStyle(fontFamily = Manrope, fontSize = 22.sp, color = Ink, fontWeight = FontWeight.Medium),
    )
  }
  val muted = remember {
    AmountStyle(
      textStyle = TextStyle(fontFamily = Manrope, fontSize = 18.sp, color = InkMuted, fontWeight = FontWeight.Normal),
    )
  }
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    AmountText(amount = money, style = big, modifier = Modifier.fillMaxWidth().height(60.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      LabeledAmount("Always signed", Modifier.weight(1f)) {
        AmountText(
          amount = money,
          style = mid,
          showSign = ShowSign.Always,
          modifier = Modifier.fillMaxWidth().height(32.dp),
        )
      }
      LabeledAmount("No trailing zeros", Modifier.weight(1f)) {
        AmountText(
          amount = money,
          style = muted,
          fractionPolicy = FractionPolicy.IgnoreZero,
          modifier = Modifier.fillMaxWidth().height(32.dp),
        )
      }
    }
  }
}

@Composable
private fun LabeledAmount(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      label.uppercase(),
      style = TextStyle(
        fontFamily = Manrope,
        fontSize = 10.sp,
        color = InkMuted,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
      ),
    )
    content()
  }
}

@Composable
private fun AmountFieldSection(money: Money, onMoneyChange: (Money) -> Unit) {
  val defaultStyle = remember {
    AmountStyle(
      textStyle = TextStyle(fontFamily = Manrope, fontSize = 36.sp, color = Ink, fontWeight = FontWeight.SemiBold),
      cursor = CursorStyle(color = Accent),
      zeroNotationStyle = TextStyle(fontFamily = Manrope, fontSize = 36.sp, color = Placeholder, fontWeight = FontWeight.SemiBold),
      fixedFractionStyle = TextStyle(fontFamily = Manrope, fontSize = 36.sp, color = Placeholder, fontWeight = FontWeight.SemiBold),
    )
  }
  val gradientStyle = remember {
    AmountStyle(
      textStyle = TextStyle(fontFamily = Manrope, fontSize = 44.sp, color = Ink, fontWeight = FontWeight.Bold),
      gradientBrush = Brush.horizontalGradient(listOf(Accent, AccentAlt)),
      cursor = CursorStyle(color = AccentAlt),
    )
  }
  val compactStyle = remember {
    AmountStyle(
      textStyle = TextStyle(fontFamily = Manrope, fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Medium),
      cursor = CursorStyle(color = Ink),
    )
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    FieldBox(height = 64.dp) {
      AmountField(
        amount = money,
        onAmountChange = onMoneyChange,
        style = defaultStyle,
        modifier = Modifier.fillMaxWidth().height(56.dp),
      )
    }
    FieldBox(height = 76.dp) {
      AmountField(
        amount = money,
        onAmountChange = onMoneyChange,
        style = gradientStyle,
        modifier = Modifier.fillMaxWidth().height(64.dp),
      )
    }
    FieldBox(height = 48.dp) {
      AmountField(
        amount = money,
        onAmountChange = onMoneyChange,
        style = compactStyle,
        modifier = Modifier.fillMaxWidth().height(32.dp),
      )
    }
  }
}

@Composable
private fun FieldBox(height: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth().height(height),
    color = FieldSurface,
    shape = RoundedCornerShape(14.dp),
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      content()
    }
  }
}

private fun randomMoney(currencyCode: String): Money = Money(
  units = Random.nextLong(0, 99_999L),
  nanos = Random.nextInt(0, 1_000_000_000),
  currencyCode = currencyCode,
)
