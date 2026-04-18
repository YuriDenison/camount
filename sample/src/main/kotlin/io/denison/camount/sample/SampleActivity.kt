package io.denison.camount.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.denison.camount.view.AmountEditView
import io.denison.camount.view.AmountTextView
import io.denison.camount.view.Money

class SampleActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_sample)

    findViewById<AmountTextView>(R.id.amount_text_full).amount = Money(1234, 560_000_000, "EUR")
    findViewById<AmountTextView>(R.id.amount_text_short).amount = Money(-42, 0, "USD")

    findViewById<AmountEditView>(R.id.amount_edit).amount = Money(0, 0, "EUR")
    findViewById<AmountEditView>(R.id.amount_edit_gradient).apply {
      amount = Money(99, 990_000_000, "USD")
      addAmountChangeListener { /* observe changes */ }
    }
  }
}
