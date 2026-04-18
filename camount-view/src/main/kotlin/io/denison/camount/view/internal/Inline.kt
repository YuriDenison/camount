@file:Suppress("NOTHING_TO_INLINE")

package io.denison.camount.view.internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View

internal inline fun <T : Any> given(condition: Boolean, body: () -> T?): T? =
  if (condition) body() else null

internal inline fun Any?.asTrue(): Boolean = true
internal inline fun Any?.asFalse(): Boolean = false

@SuppressLint("Recycle")
internal inline fun Context.obtainStyledAttributesBlock(
  set: AttributeSet?,
  attrs: IntArray,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
  action: TypedArray.() -> Unit,
) {
  val typedArray = obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes)
  try {
    typedArray.action()
  } finally {
    typedArray.recycle()
  }
}

@SuppressLint("Recycle")
internal inline fun Context.obtainStyledAttributesBlock(
  resourceId: Int,
  attrs: IntArray,
  action: TypedArray.() -> Unit,
) {
  val typedArray = obtainStyledAttributes(resourceId, attrs)
  try {
    typedArray.action()
  } finally {
    typedArray.recycle()
  }
}

internal val View.horizontalPadding: Int get() = paddingLeft + paddingRight
internal val View.verticalPadding: Int get() = paddingTop + paddingBottom

internal inline fun resolveSize(measureSpec: Int, size: () -> Int): Int {
  val specMode = View.MeasureSpec.getMode(measureSpec)
  val specSize = View.MeasureSpec.getSize(measureSpec)
  return when (specMode) {
    View.MeasureSpec.AT_MOST -> size().let { if (specSize < it) specSize or View.MEASURED_STATE_TOO_SMALL else it }
    View.MeasureSpec.EXACTLY -> specSize
    View.MeasureSpec.UNSPECIFIED -> size()
    else -> size()
  }
}

internal fun android.content.res.Resources.dpi(dp: Int): Int =
  (dp * displayMetrics.density).toInt()

internal inline fun <T> List<T>.forEachReversedFast(action: (T) -> Unit) {
  for (index in size - 1 downTo 0) {
    action(this[index])
  }
}

internal open class SimpleTextWatcher : TextWatcher {

  override fun afterTextChanged(editable: Editable) = Unit
  override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) = Unit
  override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) = Unit
}
