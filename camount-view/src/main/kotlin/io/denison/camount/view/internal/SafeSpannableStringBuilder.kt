package io.denison.camount.view.internal

import android.text.Selection
import android.text.SpannableStringBuilder
import androidx.annotation.CallSuper
import androidx.annotation.IntRange

internal open class SafeSpannableStringBuilder @JvmOverloads constructor(
  text: CharSequence? = null,
  start: Int = 0,
  end: Int = text?.length ?: 0,
) : SpannableStringBuilder(text ?: "", start, end) {

  final override fun replace(
    start: Int,
    end: Int,
    tb: CharSequence?,
    tbstart: Int,
    tbend: Int,
  ): SpannableStringBuilder {
    val length = length
    val fixedStart = start.coerceIn(0, length)
    val fixedEnd = end.coerceIn(0, length)

    val fixedText = tb ?: ""
    val textLength = fixedText.length
    val fixedTextStart = tbstart.coerceIn(0, textLength)
    val fixedTextEnd = tbend.coerceIn(0, textLength)

    return safeReplace(fixedStart, fixedEnd, fixedText, fixedTextStart, fixedTextEnd)
  }

  @CallSuper
  open fun safeReplace(
    @IntRange(from = 0L) start: Int,
    @IntRange(from = 0L) end: Int,
    text: CharSequence,
    @IntRange(from = 0L) textStart: Int,
    @IntRange(from = 0L) textEnd: Int,
  ): SpannableStringBuilder {
    return super.replace(start, end, text, textStart, textEnd)
  }

  @CallSuper
  override fun setSpan(what: Any?, start: Int, end: Int, flags: Int) {
    val length = length
    when (what) {
      Selection.SELECTION_START, Selection.SELECTION_END -> {
        val fixedStart = start.coerceIn(0, length)
        val fixedEnd = end.coerceIn(0, length)
        super.setSpan(what, fixedStart, fixedEnd, flags)
      }

      else -> when {
        start in 0..length && end in 0..length -> super.setSpan(what, start, end, flags)
        else -> Unit
      }
    }
  }
}
