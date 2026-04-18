package io.denison.camount.view.drawable.animation

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Property
import android.view.animation.DecelerateInterpolator

internal class BoundsAnimation(private val target: Drawable) {

  private val bounds = Rect()
  private val animator =
    ObjectAnimator.ofObject(target, BoundsProperty(), RectEvaluator(), bounds).also {
      it.interpolator = DecelerateInterpolator()
    }

  private var dirty: Boolean = false

  fun setBounds(bounds: Rect) {
    if (this.bounds.isEmpty) {
      this.bounds.set(bounds)
      target.bounds = bounds
      dirty = false
    } else if (this.bounds != bounds) {
      this.bounds.set(bounds)
      animator.setObjectValues(this.bounds)
      dirty = true
    }
  }

  fun start() {
    if (dirty) {
      dirty = false
      animator.start()
    }
    if (animator.isPaused) animator.resume()
  }

  fun pause() {
    animator.pause()
  }

  fun isRunning() = animator.isRunning

  var duration: Long
    get() = animator.duration
    set(value) {
      animator.duration = value
    }
}

private class BoundsProperty : Property<Drawable, Rect>(Rect::class.java, "bounds") {

  private val rect = Rect()

  override fun get(target: Drawable): Rect {
    target.copyBounds(rect)
    return rect
  }

  override fun set(target: Drawable, value: Rect) {
    target.bounds = value
  }
}

private class RectEvaluator : TypeEvaluator<Rect> {

  private val result = Rect()

  override fun evaluate(fraction: Float, startValue: Rect, endValue: Rect) = result.apply {
    set(
      interpolate(startValue.left, endValue.left, fraction),
      interpolate(startValue.top, endValue.top, fraction),
      interpolate(startValue.right, endValue.right, fraction),
      interpolate(startValue.bottom, endValue.bottom, fraction),
    )
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun interpolate(from: Int, to: Int, progress: Float): Int =
    (from + (to - from) * progress).toInt()
}
