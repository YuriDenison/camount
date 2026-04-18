package io.denison.camount.view.databinding

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.adapters.ListenerUtil
import io.denison.camount.view.AmountChangeListener
import io.denison.camount.view.AmountEditView
import io.denison.camount.view.Money
import io.denison.camount.view.MoneyStub

@InverseBindingAdapter(attribute = "amount")
fun getAmount(view: AmountEditView): Money = view.amount

@BindingAdapter("amount", "amountAttrChanged", requireAll = false)
fun setAmountAndListeners(
  view: AmountEditView,
  value: Money?,
  listener: InverseBindingListener?,
) {
  view.amount = value ?: MoneyStub

  val amountListener: AmountChangeListener? = listener?.let { l -> { l.onChange() } }

  ListenerUtil.trackListener(view, amountListener, R.id.amountTextChangedListener)
    ?.let { view.removeAmountChangeListener(it) }

  amountListener?.let { view.addAmountChangeListener(it) }
}
