package io.denison.camount.sample

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.denison.camount.Money
import io.denison.camount.view.AmountChangeListener
import io.denison.camount.view.AmountEditView
import io.denison.camount.view.AmountTextView
import io.denison.camount.view.Money as ViewMoney

class SampleActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(
      ComposeView(this).apply {
        setContent {
          CamountSampleScreen(
            viewSection = { money, onMoneyChange ->
              ViewSection(money = money, onMoneyChange = onMoneyChange)
            },
          )
        }
      },
    )
  }
}

@Composable
private fun ViewSection(money: Money, onMoneyChange: (Money) -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AndroidView(
      factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.view_amount_text, null, false) as AmountTextView },
      update = { it.amount = money.toViewMoney() },
      modifier = Modifier.fillMaxWidth(),
    )

    AndroidView(
      factory = { ctx ->
        val view = LayoutInflater
          .from(ctx)
          .inflate(R.layout.view_amount_edit, null, false) as AmountEditView
        val listener: AmountChangeListener = { updated ->
          onMoneyChange(updated.toCommonMoney())
        }
        view.addAmountChangeListener(listener)
        view
      },
      update = { view ->
        val target = money.toViewMoney()
        if (view.amount != target) view.amount = target
      },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

private fun Money.toViewMoney(): ViewMoney = ViewMoney(
  units = units,
  nanos = nanos,
  currencyCode = currencyCode,
)

private fun ViewMoney.toCommonMoney(): Money = Money(
  units = units,
  nanos = nanos,
  currencyCode = currencyCode,
)
