package io.denison.camount.view.drawable.calculator

import android.animation.ArgbEvaluator
import android.graphics.LinearGradient
import android.graphics.Rect
import android.graphics.Shader
import androidx.annotation.ColorInt

internal abstract class GradientCalculator {

  protected var width: Int = 0
    private set
  protected var height: Int = 0
    private set

  fun setSize(width: Int, height: Int) {
    this.width = width
    this.height = height
  }

  abstract fun calculate(bounds: Rect): Shader
}

internal class VerticalGradientCalculator(
  @ColorInt val startColor: Int,
  @ColorInt val endColor: Int,
) : GradientCalculator() {

  override fun calculate(bounds: Rect): LinearGradient {
    val x0 = 0f
    val y0 = 0f
    val y1 = bounds.height().toFloat()

    return LinearGradient(
      x0,
      y0,
      x0,
      y1,
      startColor,
      endColor,
      Shader.TileMode.CLAMP,
    )
  }
}

internal class HorizontalGradientCalculator(
  @ColorInt val startColor: Int,
  @ColorInt val endColor: Int,
) : GradientCalculator() {

  private val argEvaluator = ArgbEvaluator()

  override fun calculate(bounds: Rect): Shader {
    val startFraction = bounds.left.toFloat() / width
    val endFraction = bounds.right.toFloat() / width

    val startColor = color(startFraction)
    val endColor = color(endFraction)

    val x0 = 0f
    val y0 = 0f
    val x1 = bounds.width().toFloat()

    return LinearGradient(
      x0,
      y0,
      x1,
      y0,
      startColor,
      endColor,
      Shader.TileMode.REPEAT,
    )
  }

  private fun color(fraction: Float) = argEvaluator.evaluate(fraction, startColor, endColor) as Int
}
