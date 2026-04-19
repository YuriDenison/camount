package io.denison.camount.view.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import io.denison.camount.view.drawable.calculator.AmountBoundsCalculator
import io.denison.camount.view.drawable.calculator.AmountDiffCalculator
import io.denison.camount.view.drawable.calculator.DefaultBoundsCalculator
import io.denison.camount.view.drawable.calculator.calculator
import io.denison.camount.view.formatter.AmountConfig
import io.denison.camount.view.internal.forEachReversedFast

private const val DIFF_ANIMATION_DURATION = 120L

internal class AmountDrawable(
  config: AmountConfig,
  style: AmountDrawableStyle,
  animation: AmountDrawableAnimation,
) : Drawable(), Drawable.Callback {

  private val diffCalculator: AmountDiffCalculator = animation.calculator(config, style) {
    SymbolCellDrawable().also {
      it.state = state
      it.alpha = alpha
      it.colorFilter = colorFilter
      it.setDuration(DIFF_ANIMATION_DURATION)
      it.delete()
    }
  }

  private val boundsCalculator: AmountBoundsCalculator = DefaultBoundsCalculator(style)

  private var cells = diffCalculator.createCells()

  private var cursorPosition = -1
  private val cursor = style.cursorStyle?.let { cursorStyle ->
    CursorCellDrawable(cursorStyle).also {
      it.setDuration(DIFF_ANIMATION_DURATION)
    }
  }

  override fun draw(canvas: Canvas) {
    cells.forEachReversedFast { it.draw(canvas) }
    cursor?.draw(canvas)
  }

  private var _alpha: Int = 0xFF
  override fun getAlpha() = _alpha
  override fun setAlpha(alpha: Int) {
    _alpha = alpha
    cells.forEach { it.alpha = alpha }
  }

  private var _colorFilter: ColorFilter? = null
  override fun getColorFilter(): ColorFilter? = _colorFilter
  override fun setColorFilter(filter: ColorFilter?) {
    _colorFilter = filter
    cells.forEach { it.colorFilter = filter }
  }

  override fun getIntrinsicWidth() = boundsCalculator.calculateIntrinsicWidth(cells)
  override fun getIntrinsicHeight() = boundsCalculator.calculateIntrinsicHeight(cells)

  private val amountBounds = Rect()
  fun setText(text: CharSequence, fieldPositions: AmountFieldPositions) {
    stop()
    copyBounds(amountBounds)
    cells = diffCalculator.diff(cells, text, fieldPositions)
    cursorPosition = fieldPositions.cursorPosition
    boundsCalculator.calculateBounds(cells, cursor, cursorPosition, amountBounds)
    start()
  }

  fun setCursorVisible(visible: Boolean) {
    cursor?.run {
      pause()
      setVisible(visible, false)
      start()
    }
  }

  override fun onBoundsChange(bounds: Rect) {
    stop()
    amountBounds.set(bounds)
    boundsCalculator.calculateBounds(cells, cursor, cursorPosition, amountBounds)
    start()
  }

  fun stop() {
    cursor?.let {
      it.pause()
      it.callback = null
    }
    cells.forEach {
      it.pause()
      it.callback = null
    }
  }

  private fun start() {
    cursor?.let {
      it.callback = this
      it.start()
    }
    cells.forEach {
      it.callback = this
      it.start()
    }
  }

  override fun isStateful() = true
  override fun onStateChange(state: IntArray): Boolean {
    var changed = false
    cursor?.let {
      if (it.isStateful) {
        changed = it.setState(state)
      }
    }
    cells.forEach {
      if (it.isStateful) {
        changed = it.setState(state) || changed
      }
    }
    return changed
  }

  override fun invalidateDrawable(who: Drawable) = invalidateSelf()
  override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) =
    scheduleSelf(what, `when`)

  override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)

  @Deprecated(
    "Deprecated in Java",
    ReplaceWith("PixelFormat.OPAQUE", "android.graphics.PixelFormat"),
  )
  override fun getOpacity() = PixelFormat.OPAQUE
}
