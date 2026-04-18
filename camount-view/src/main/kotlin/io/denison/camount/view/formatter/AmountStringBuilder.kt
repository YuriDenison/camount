package io.denison.camount.view.formatter

import android.text.Selection
import android.text.SpannableStringBuilder
import io.denison.camount.view.internal.SafeSpannableStringBuilder

internal class AmountStringBuilder(
  source: CharSequence,
  val formatter: AmountFormatter,
) : SafeSpannableStringBuilder() {

  init {
    replace(0, length, source, 0, source.length)
  }

  override fun safeReplace(
    start: Int,
    end: Int,
    text: CharSequence,
    textStart: Int,
    textEnd: Int,
  ): SpannableStringBuilder {
    if (start == end && textStart == textEnd) return this
    val formatted = formatter.format(this, start, end, text, textStart, textEnd)
    return super.safeReplace(0, length, formatted, 0, formatted.length).also {
      Selection.setSelection(it, formatter.cursorPosition)
    }
  }
}
