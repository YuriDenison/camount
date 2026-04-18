package io.denison.camount.compose.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import io.denison.camount.compose.AmountStyle
import io.denison.camount.formatter.AmountField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal const val DIFF_ANIMATION_DURATION_MS: Int = 120

private const val MAX_STACK_SYMBOLS = 3
private const val ANIMATION_SCALE = 0.6f

internal class SymbolCell(
  private val measurer: TextMeasurer,
  private val style: AmountStyle,
  private val scope: CoroutineScope,
) {
  var field: AmountField? = null
    private set

  private var durationMs: Int = DIFF_ANIMATION_DURATION_MS

  private val symbols: ArrayDeque<SymbolLayer> = ArrayDeque()

  val currentChar: Char get() = symbols.lastOrNull()?.char ?: 0.toChar()

  var isVisible: Boolean = false
    private set

  val isRunning: Boolean
    get() = symbols.any { it.animating } || boundsAnim.isRunning

  val intrinsicWidth: Float get() = symbols.lastOrNull()?.width ?: 0f
  val intrinsicHeight: Float get() = symbols.lastOrNull()?.height ?: 0f

  private val boundsAnim = BoundsAnimation()

  fun setTargetBounds(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
  ) {
    boundsAnim.setTarget(left, top, width, height, durationMs, scope)
  }

  fun replace(char: Char, field: AmountField?) {
    val last = symbols.lastOrNull()
    if (last != null && last.char == char && last.field == field) {
      last.show(durationMs, scope)
    } else {
      this.field = field
      val style = style.styleFor(field)
      val layer = SymbolLayer(
        char = char,
        field = field,
        style = style,
        layout = measurer.measure(
          text = char.toString(),
          style = style,
          constraints = Constraints(),
          softWrap = false,
          maxLines = 1,
        ),
      )
      symbols.addLast(layer)
      while (symbols.size > MAX_STACK_SYMBOLS) symbols.removeFirst()

      for (i in 0 until symbols.size - 1) symbols[i].hide(durationMs, scope)
      layer.show(durationMs, scope)
    }
    isVisible = true
  }

  fun delete() {
    symbols.lastOrNull()?.hide(durationMs, scope)
    isVisible = false
  }

  fun setDuration(value: Int) {
    durationMs = value
  }

  fun draw(drawScope: DrawScope, brush: Brush?) {
    val left = boundsAnim.left
    val top = boundsAnim.top
    val width = boundsAnim.width
    val height = boundsAnim.height
    if (width <= 0f || height <= 0f) return

    val iw = symbols.lastOrNull()?.width ?: return
    val ih = symbols.lastOrNull()?.height ?: return
    val scaleX = if (iw <= 0f) 1f else width / iw
    val scaleY = if (ih <= 0f) 1f else height / ih

    drawScope.translate(left, top) {
      scale(scaleX, scaleY, pivot = Offset.Zero) {
        for (i in symbols.indices) {
          val layer = symbols[i]
          val level = layer.appearance.value
          if (level <= 0f) continue
          val selfScale = ANIMATION_SCALE + (1f - ANIMATION_SCALE) * level
          val pivotX = layer.width * 0.5f
          val pivotY = layer.baseline

          scale(selfScale, selfScale, pivot = Offset(pivotX, pivotY)) {
            val effectiveBrush = if (brush != null && layer.field == null && isGradientTarget(layer.char)) brush
            else SolidColor(layer.style.color)
            drawText(
              textLayoutResult = layer.layout,
              brush = effectiveBrush,
              alpha = level,
            )
          }
        }
      }
    }
  }

  private fun isGradientTarget(c: Char): Boolean = !c.isWhitespace() && c != 0.toChar()
}

private class SymbolLayer(
  val char: Char,
  val field: AmountField?,
  val style: TextStyle,
  val layout: TextLayoutResult,
) {
  val width: Float = layout.size.width.toFloat()
  val height: Float = layout.size.height.toFloat()
  val baseline: Float = layout.getLineBaseline(0)

  val appearance: Animatable<Float, *> = Animatable(0f)
  private var job: Job? = null
  val animating: Boolean get() = job?.isActive == true

  fun show(durationMs: Int, scope: CoroutineScope) {
    job?.cancel()
    job = scope.launch {
      appearance.animateTo(1f, tween(durationMs))
    }
  }

  fun hide(durationMs: Int, scope: CoroutineScope) {
    job?.cancel()
    job = scope.launch {
      appearance.animateTo(0f, tween(durationMs))
    }
  }
}

internal class BoundsAnimation {
  private val leftAnim = Animatable(0f)
  private val topAnim = Animatable(0f)
  private val widthAnim = Animatable(0f)
  private val heightAnim = Animatable(0f)

  val left: Float get() = leftAnim.value
  val top: Float get() = topAnim.value
  val width: Float get() = widthAnim.value
  val height: Float get() = heightAnim.value

  val isRunning: Boolean
    get() = leftAnim.isRunning || topAnim.isRunning ||
      widthAnim.isRunning || heightAnim.isRunning

  fun setTarget(
    targetLeft: Float,
    targetTop: Float,
    targetWidth: Float,
    targetHeight: Float,
    durationMs: Int,
    scope: CoroutineScope,
  ) {
    if (widthAnim.value == 0f && heightAnim.value == 0f) {
      scope.launch { leftAnim.snapTo(targetLeft) }
      scope.launch { topAnim.snapTo(targetTop) }
      scope.launch { widthAnim.snapTo(targetWidth) }
      scope.launch { heightAnim.snapTo(targetHeight) }
    } else {
      scope.launch { leftAnim.animateTo(targetLeft, tween(durationMs)) }
      scope.launch { topAnim.animateTo(targetTop, tween(durationMs)) }
      scope.launch { widthAnim.animateTo(targetWidth, tween(durationMs)) }
      scope.launch { heightAnim.animateTo(targetHeight, tween(durationMs)) }
    }
  }
}
