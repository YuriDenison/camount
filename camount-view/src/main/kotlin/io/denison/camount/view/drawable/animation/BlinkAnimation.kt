package io.denison.camount.view.drawable.animation

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.animation.AccelerateInterpolator

private const val BLINK_DURATION = 530L
private const val APPEAR_DURATION = 500L

internal class BlinkAnimation(private val target: Drawable) {

  private var isVisible = false

  init {
    target.alpha = 0
  }

  private val appearance = AppearanceAnimation(
    animator = ValueAnimator.ofInt(0, 255).apply {
      interpolator = AccelerateInterpolator()
      duration = APPEAR_DURATION
      addUpdateListener {
        val old = target.alpha
        val new = it.animatedValue as Int
        if (old != new) {
          target.alpha = new
          target.invalidateSelf()
        }
      }
    }
  )

  private val blinkTime get() = SystemClock.uptimeMillis() + BLINK_DURATION

  private val blink = object : Runnable {
    override fun run() {
      target.unscheduleSelf(this)
      val show = !isVisible
      isVisible = show
      appearance.start(show = show, restart = false)
      target.scheduleSelf(this, blinkTime)
    }
  }

  fun start(visible: Boolean, restart: Boolean) {
    target.unscheduleSelf(blink)
    isVisible = visible
    appearance.start(show = visible, restart = restart)
    if (visible) {
      target.scheduleSelf(blink, blinkTime)
    }
  }

  fun pause() {
    target.unscheduleSelf(blink)
    appearance.pause()
  }
}
