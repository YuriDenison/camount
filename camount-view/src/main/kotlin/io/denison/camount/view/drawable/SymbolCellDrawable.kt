package io.denison.camount.view.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.collection.CircularArray
import io.denison.camount.view.drawable.animation.AnimatableSymbol
import io.denison.camount.view.drawable.animation.BoundsAnimation

internal class SymbolCellDrawable : Drawable(), Drawable.Callback, AnimatableSymbol {

  private val maxSymbols: Int = 3
  private var symbols = CircularArray<SymbolDrawable>(maxSymbols + 1)

  override fun getIntrinsicWidth() = symbols.lastOrNull()?.intrinsicWidth ?: -1
  override fun getIntrinsicHeight() = symbols.lastOrNull()?.intrinsicHeight ?: -1

  override fun draw(canvas: Canvas) {
    symbols.forEach { it.draw(canvas) }
  }

  var char: Char = 0.toChar()
    private set

  fun replace(value: Char, style: SymbolStyle) {
    val last = symbols.lastOrNull()
    val lastChar = last?.char
    val lastStyle = last?.symbolStyle

    when {
      lastChar == value && lastStyle == style -> last.setVisible(visible = true, restart = false)
      else -> {
        char = value

        val newSymbol = newSymbolDrawable(value, style)

        symbols.addLast(newSymbol)
        if (symbols.size() > maxSymbols) {
          symbols.removeFromStart(1)
        }

        symbols.forEach(skipEnd = 1) { it.setVisible(visible = false, restart = false) }
        newSymbol.setVisible(visible = true, restart = false)
      }
    }

    setVisible(true, false)
  }

  fun delete() {
    symbols.lastOrNull()?.setVisible(visible = false, restart = false)
    setVisible(false, false)
  }

  fun setDuration(value: Long) {
    bounder.duration = value
    symbols.forEach { it.setDuration(value) }
  }

  private var _alpha: Int = 0xFF
  override fun getAlpha() = _alpha
  override fun setAlpha(alpha: Int) {
    _alpha = alpha
    symbols.forEach { it.alpha = alpha }
  }

  private var _colorFilter: ColorFilter? = null
  override fun getColorFilter(): ColorFilter? = _colorFilter
  override fun setColorFilter(filter: ColorFilter?) {
    _colorFilter = filter
    symbols.forEach { it.colorFilter = filter }
  }

  override fun onBoundsChange(bounds: Rect) {
    symbols.forEach { it.bounds = bounds }
    invalidateSelf()
  }

  private val bounder = BoundsAnimation(this)

  fun setTargetBounds(bounds: Rect) {
    bounder.setBounds(bounds)
  }

  override fun start() {
    symbols.forEach {
      it.callback = this
      it.start()
    }
    bounder.start()
  }

  override fun pause() {
    bounder.pause()
    symbols.forEach {
      it.pause()
      it.callback = null
    }
  }

  override val isRunning get() = bounder.isRunning() || symbols.any { it.isRunning }

  override fun isStateful() = symbols.any { it.isStateful }

  override fun onStateChange(state: IntArray): Boolean {
    var changed = false
    symbols.forEach {
      if (it.isStateful) {
        changed = it.setState(state) || changed
      }
    }
    return changed
  }

  fun setShader(shader: Shader?) {
    symbols.forEach { it.setShader(shader) }
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

  private fun newSymbolDrawable(c: Char, style: SymbolStyle) = SymbolDrawable(c, style).also {
    it.state = state
    it.alpha = alpha
    it.colorFilter = colorFilter
    it.bounds = bounds
    it.setDuration(bounder.duration)
  }
}
