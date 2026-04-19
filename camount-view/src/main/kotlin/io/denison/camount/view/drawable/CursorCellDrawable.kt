package io.denison.camount.view.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.DrawableWrapper
import io.denison.camount.view.drawable.animation.AnimatableSymbol
import io.denison.camount.view.drawable.animation.BlinkAnimation
import io.denison.camount.view.drawable.animation.BoundsAnimation
import io.denison.camount.view.internal.roundedRectDrawable

private const val DEBUG = false

internal data class CursorStyle(
  val color: Int,
  val width: Int,
  val height: Int,
)

internal class CursorCellDrawable(
  private val style: CursorStyle,
) : DrawableWrapper(
  roundedRectDrawable(
    color = style.color,
    radiusPx = (style.width / 2).coerceAtLeast(1),
  ),
),
  AnimatableSymbol {

  private lateinit var debugPaint: Paint

  private val bounder = BoundsAnimation(this)
  private val blinker = BlinkAnimation(this)

  init {
    if (DEBUG) {
      debugPaint = Paint().also {
        it.style = Paint.Style.STROKE
        it.strokeWidth = 1f
        it.color = Color.RED
      }
    }
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)

    if (DEBUG) {
      canvas.drawRect(bounds, debugPaint)
    }
  }

  override fun getIntrinsicWidth() = style.width
  override fun getIntrinsicHeight() = style.height

  fun setDuration(value: Long) {
    bounder.duration = value
  }

  fun setTargetBounds(bounds: Rect) {
    bounder.setBounds(bounds)
  }

  private var isRestart = false
  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    val changed = super.setVisible(visible, restart)
    isRestart = restart
    return changed
  }

  override fun start() {
    bounder.start()
    blinker.start(isVisible, isRestart)
  }

  override fun pause() {
    bounder.pause()
    blinker.pause()
  }

  override val isRunning get() = bounder.isRunning()

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    invalidateSelf()
  }
}
