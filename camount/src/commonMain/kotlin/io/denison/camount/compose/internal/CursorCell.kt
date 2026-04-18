package io.denison.camount.compose.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.denison.camount.compose.CursorStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val BLINK_DURATION_MS = 530L
private const val APPEAR_DURATION_MS = 500

internal class CursorCell(
  private val style: CursorStyle,
  private val scope: CoroutineScope,
) {
  private val boundsAnim = BoundsAnimation()
  private val alpha: Animatable<Float, *> = Animatable(0f)

  private var blinkJob: Job? = null
  private var cursorVisible: Boolean = false

  private var durationMs: Int = DIFF_ANIMATION_DURATION_MS

  val intrinsicWidth: Float get() = 0f
  val intrinsicHeight: Float get() = 0f

  val isRunning: Boolean get() = boundsAnim.isRunning

  fun setDuration(value: Int) {
    durationMs = value
  }

  fun setTargetBounds(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
  ) {
    boundsAnim.setTarget(left, top, width, height, durationMs, scope)
  }

  fun setVisible(visible: Boolean) {
    if (cursorVisible == visible) return
    cursorVisible = visible
    blinkJob?.cancel()
    if (visible) {
      blinkJob = scope.launch {
        var on = true
        alpha.animateTo(1f, tween(APPEAR_DURATION_MS))
        while (isActive) {
          delay(BLINK_DURATION_MS)
          on = !on
          alpha.animateTo(if (on) 1f else 0f, tween(APPEAR_DURATION_MS))
        }
      }
    } else {
      scope.launch {
        alpha.animateTo(0f, tween(APPEAR_DURATION_MS))
      }
    }
  }

  fun draw(drawScope: DrawScope) {
    val a = alpha.value
    if (a <= 0f) return
    val w = boundsAnim.width
    val h = boundsAnim.height
    if (w <= 0f || h <= 0f) return
    val radius = (style.width.value / 2f).coerceAtLeast(0f)
    drawScope.drawRoundRectCompat(
      color = style.color,
      topLeft = Offset(boundsAnim.left, boundsAnim.top),
      size = Size(w, h),
      cornerRadius = radius,
      alpha = a,
    )
  }
}

private fun DrawScope.drawRoundRectCompat(
  color: androidx.compose.ui.graphics.Color,
  topLeft: Offset,
  size: Size,
  cornerRadius: Float,
  alpha: Float,
) {
  drawRoundRect(
    color = color,
    topLeft = topLeft,
    size = size,
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
    alpha = alpha,
  )
}
