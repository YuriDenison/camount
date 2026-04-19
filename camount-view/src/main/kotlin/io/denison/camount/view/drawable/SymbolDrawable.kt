package io.denison.camount.view.drawable

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.withMatrix
import io.denison.camount.view.drawable.animation.AnimatableSymbol
import io.denison.camount.view.drawable.animation.AppearanceAnimation
import io.denison.camount.view.internal.AmountTextAppearance
import io.denison.camount.view.internal.given
import kotlin.math.ceil

private const val DEBUG = false

internal data class SymbolStyle(
  val textAppearance: AmountTextAppearance,
  val type: StyleType,
) {

  enum class StyleType {
    COLOR,
    SHADER,
  }
}

internal class SymbolDrawable(
  val char: Char,
  val symbolStyle: SymbolStyle,
) : Drawable(), AnimatableSymbol {

  private lateinit var debugPaint: Paint

  init {
    if (DEBUG) {
      debugPaint = Paint().also {
        it.style = Paint.Style.STROKE
        it.strokeWidth = 1f
        it.color = Color.RED
      }
    }
  }

  private val symbol: CharSequence = char.toString()
  private val symbolPaint = TextPaint().apply {
    isAntiAlias = true
    isDither = true
    isLinearText = true
    isSubpixelText = true
    textSize = symbolStyle.textAppearance.textSize
    typeface = symbolStyle.textAppearance.typeface
    fontFeatureSettings = symbolStyle.textAppearance.fontFeatureSettings
    letterSpacing = symbolStyle.textAppearance.letterSpacing
    color = symbolStyle.textAppearance.textColor.defaultColor
  }

  private val symbolLayout = newSymbolLayout()
  override fun getIntrinsicWidth() = symbolLayout.width
  override fun getIntrinsicHeight() = symbolLayout.height

  private val symbolCenterX = intrinsicWidth * 0.5f
  private val symbolLineBaseline = symbolLayout.getLineBaseline(0).toFloat()

  private val drawBounds = Rect()
  private val drawMatrix = Matrix()

  override fun draw(canvas: Canvas) {
    val level = level
    if (level == 0) return

    val alpha = alpha * level / MAX_LEVEL
    if (alpha == 0) return

    copyBounds(drawBounds)
    if (drawBounds.isEmpty) return

    if (symbolPaint.alpha != alpha) {
      symbolPaint.alpha = alpha
    }

    if (symbolPaint.shader != shader) {
      symbolPaint.shader = shader
    }

    val width = drawBounds.width()
    val height = drawBounds.height()
    val scaleX = width.toFloat() / intrinsicWidth
    val scaleY = height.toFloat() / intrinsicHeight
    val translateX = drawBounds.left.toFloat()
    val translateY = drawBounds.top.toFloat()

    val selfScale = getScale(level)
    val selfPivotX = symbolCenterX
    val selfPivotY = symbolLineBaseline

    drawMatrix.reset()
    drawMatrix.setScale(scaleX, scaleY)
    drawMatrix.postTranslate(translateX, translateY)
    drawMatrix.preScale(selfScale, selfScale, selfPivotX, selfPivotY)

    canvas.withMatrix(drawMatrix) {
      symbolLayout.draw(this)
    }

    if (DEBUG) {
      canvas.drawRect(drawBounds, debugPaint)
    }
  }

  private var alpha: Int = 0xFF
  override fun getAlpha() = alpha
  override fun setAlpha(alpha: Int) {
    this.alpha = alpha
  }

  override fun getColorFilter(): ColorFilter? = symbolPaint.colorFilter
  override fun setColorFilter(filter: ColorFilter?) {
    if (symbolPaint.colorFilter != filter) {
      symbolPaint.colorFilter = filter
    }
  }

  private val appearance = AppearanceAnimation(
    animator = ValueAnimator.ofInt(0, MAX_LEVEL).apply {
      interpolator = AccelerateInterpolator()
      addUpdateListener {
        val value = it.animatedValue as Int
        if (setLevel(value)) {
          invalidateSelf()
        }
      }
    },
  )

  fun setDuration(value: Long) {
    appearance.duration = value
  }

  private var isRestart = false
  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    val changed = super.setVisible(visible, restart)
    isRestart = restart
    return changed
  }

  override fun start() = appearance.start(isVisible, isRestart)
  override fun pause() = appearance.pause()
  override val isRunning get() = appearance.isRunning()

  override fun isStateful() = true

  private var shader: Shader? = null
  fun setShader(shader: Shader?) {
    this.shader = given(symbolStyle.type == SymbolStyle.StyleType.SHADER) { shader }
  }

  override fun onStateChange(state: IntArray): Boolean {
    val stateList = symbolStyle.textAppearance.textColor
    val default = stateList.defaultColor
    val color = stateList.getColorForState(state, default)
    val changed = symbolPaint.color != color
    if (changed) {
      symbolPaint.color = color
    }
    return changed
  }

  override fun onLevelChange(level: Int) = true

  private fun newSymbolLayout(): Layout {
    val metrics = BoringLayout.isBoring(symbol, symbolPaint)
    val outerWidth = ceil(Layout.getDesiredWidth(symbol, symbolPaint)).toInt()
    val alignment = Layout.Alignment.ALIGN_CENTER
    val spacingAdd = symbolStyle.textAppearance.lineHeight - symbolPaint.getFontMetricsInt(null)
    val spacingMult = 1.0f
    val includePad = true

    return if (metrics != null) {
      BoringLayout.make(
        symbol,
        symbolPaint,
        outerWidth,
        alignment,
        spacingMult,
        spacingAdd,
        metrics,
        includePad,
      )
    } else {
      StaticLayout.Builder.obtain(
        symbol,
        0,
        symbol.length,
        symbolPaint,
        outerWidth,
      )
        .setAlignment(alignment)
        .setLineSpacing(spacingAdd, spacingMult)
        .setMaxLines(1)
        .setIncludePad(includePad)
        .build()
    }
  }

  @Deprecated(
    "Deprecated in Java",
    ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"),
  )
  override fun getOpacity() = PixelFormat.TRANSLUCENT
}

private const val ANIMATION_SCALE = 0.6f
private const val MAX_LEVEL = 10000

@Suppress("NOTHING_TO_INLINE")
private inline fun getScale(level: Int) = interpolate(ANIMATION_SCALE, 1f, level)

@Suppress("NOTHING_TO_INLINE", "SameParameterValue")
private inline fun interpolate(from: Float, to: Float, level: Int): Float =
  from + (to - from) * level / MAX_LEVEL
