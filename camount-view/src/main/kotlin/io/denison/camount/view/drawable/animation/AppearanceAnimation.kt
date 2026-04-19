package io.denison.camount.view.drawable.animation

import android.animation.ValueAnimator

internal class AppearanceAnimation(private val animator: ValueAnimator) {

  fun start(show: Boolean, restart: Boolean) {
    if (restart) {
      animator.cancel()
      strategy = if (show) appear else disappear
    }

    if (show) {
      appear()
    } else {
      disappear()
    }

    if (animator.isPaused) {
      animator.resume()
    }
  }

  fun pause() = animator.pause()
  fun isRunning() = animator.isRunning

  private val appear = Appear()
  private val disappear = Disappear()

  private var strategy: AppearStrategy = appear

  private fun appear() {
    strategy.appear()
    strategy = disappear
  }

  private fun disappear() {
    strategy.disappear()
    strategy = appear
  }

  var duration: Long
    get() = animator.duration
    set(value) {
      animator.duration = value
    }

  private interface AppearStrategy {

    fun appear() = Unit
    fun disappear() = Unit
  }

  private inner class Appear : AppearStrategy {

    override fun appear() {
      if (animator.isRunning) {
        animator.reverse()
      } else {
        animator.start()
      }
    }
  }

  private inner class Disappear : AppearStrategy {

    override fun disappear() {
      animator.reverse()
    }
  }
}
