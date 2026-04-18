package io.denison.camount.view.databinding

import androidx.databinding.BindingAdapter
import io.denison.camount.view.AmountTextView
import io.denison.camount.view.Money

@BindingAdapter("amount")
fun setAmount(view: AmountTextView, value: Money?) {
  view.amount = value
}
