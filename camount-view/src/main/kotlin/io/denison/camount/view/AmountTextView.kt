package io.denison.camount.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.annotation.IntDef
import io.denison.camount.view.drawable.AmountDrawable
import io.denison.camount.view.drawable.AmountDrawableAnimation
import io.denison.camount.view.drawable.AmountDrawableStyle
import io.denison.camount.view.drawable.AmountFieldPositions
import io.denison.camount.view.drawable.AmountGradientOrientation
import io.denison.camount.view.drawable.SymbolStyle
import io.denison.camount.view.formatter.AmountConfig
import io.denison.camount.view.formatter.AmountFormatter
import io.denison.camount.view.internal.defaultDecimalFormat
import io.denison.camount.view.internal.horizontalPadding
import io.denison.camount.view.internal.maybeOverrideTextColor
import io.denison.camount.view.internal.obtainStyledAttributesBlock
import io.denison.camount.view.internal.resolveAmountTextAppearance
import io.denison.camount.view.internal.resolveSize
import io.denison.camount.view.internal.verticalPadding
import java.text.FieldPosition

class AmountTextView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : View(context, attrs) {

  private val style: AmountDrawableStyle

  @ShowSign
  private val showSign: Int

  @Formatter
  private val formatterType: Int

  private var config: AmountConfig
  private var amountDrawable: AmountDrawable
  private var formatter: AmountFormatter

  private var currencyCode: String = EUR
    set(value) {
      if (field != value) {
        field = value
        amountDrawable.stop()
        amountDrawable.callback = null

        config = createConfig(value)
        formatter = createFormatter(config)
        amountDrawable = createAmountDrawable(config, style)
      }
    }

  var amount: Money? = Money(0, 0, currencyCode)
    set(value) {
      if (field != value) {
        field = value
        currencyCode = value?.currencyCode ?: ""
        internalSetAmount(value)
      }
    }

  init {
    setLayerType(LAYER_TYPE_HARDWARE, null)

    var gravity = Gravity.NO_GRAVITY
    var textAppearance = context.resolveAmountTextAppearance(0)
    var localShowSign = SHOW_SIGN_IF_NEGATIVE
    var localFormatter = FORMATTER_FULL

    context.obtainStyledAttributesBlock(attrs, R.styleable.AmountTextView) {
      gravity = getInteger(R.styleable.AmountTextView_android_gravity, Gravity.NO_GRAVITY)

      val textAppearanceId = getResourceId(R.styleable.AmountTextView_android_textAppearance, 0)
      textAppearance = context.resolveAmountTextAppearance(textAppearanceId)
        .maybeOverrideTextColor(getColorStateList(R.styleable.AmountTextView_android_textColor))

      localShowSign =
        when (getInteger(R.styleable.AmountTextView_amount_showSign, SHOW_SIGN_IF_NEGATIVE)) {
          SHOW_SIGN_ALWAYS -> SHOW_SIGN_ALWAYS
          else -> SHOW_SIGN_IF_NEGATIVE
        }

      localFormatter = getInteger(R.styleable.AmountTextView_amount_formatter, FORMATTER_FULL)
    }

    showSign = localShowSign
    formatterType = localFormatter

    style = AmountDrawableStyle(
      gravity = gravity,
      gradientStartColor = Color.BLACK,
      gradientEndColor = Color.BLACK,
      gradientOrientation = AmountGradientOrientation.VERTICAL,
      cursorStyle = null,
      defaultSymbolStyle = SymbolStyle(
        textAppearance = textAppearance,
        type = SymbolStyle.StyleType.COLOR
      ),
      symbolStyles = emptyMap()
    )
    config = createConfig(currencyCode)
    formatter = createFormatter(config)
    amountDrawable = createAmountDrawable(config, style)
    internalSetAmount(amount)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    amountDrawable.draw(canvas)
  }

  override fun verifyDrawable(who: Drawable): Boolean {
    return amountDrawable == who || super.verifyDrawable(who)
  }

  override fun drawableStateChanged() {
    super.drawableStateChanged()
    val changed = amountDrawable.setState(drawableState)
    if (changed) {
      invalidateDrawable(amountDrawable)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(
      resolveSize(widthMeasureSpec) {
        amountDrawable.intrinsicWidth + horizontalPadding
      },
      resolveSize(heightMeasureSpec) {
        amountDrawable.intrinsicHeight + verticalPadding
      }
    )
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    amountDrawable.setBounds(
      paddingLeft,
      paddingTop,
      w - paddingRight,
      h - paddingBottom
    )
  }

  private fun createAmountDrawable(config: AmountConfig, style: AmountDrawableStyle) =
    AmountDrawable(
      config = config,
      style = style,
      animation = AmountDrawableAnimation.LEVENSHTEIN
    ).also {
      it.setBounds(
        paddingLeft,
        paddingTop,
        width - paddingRight,
        height - paddingBottom
      )
      it.callback = this
      it.state = drawableState
    }

  private fun createConfig(currencyCode: String) = AmountConfig(
    maximumNotationDigits = 5,
    decimalFormat = defaultDecimalFormat(currencyCode)
  )

  private fun createFormatter(config: AmountConfig): AmountFormatter {
    val withFixedFractionLength = formatterType == FORMATTER_FULL
    return AmountFormatter(
      config = config,
      withCurrency = true,
      withGroupingSeparators = true,
      withFixedFractionLength = withFixedFractionLength,
      withFixedZeroNotation = true,
    )
  }

  private fun internalSetAmount(value: Money?) {
    val base = value?.let { formatter.format(it).toString() } ?: ""
    val signed = when {
      value == null -> base
      value.isZero() -> base
      !value.isPositive() -> "-$base"
      showSign == SHOW_SIGN_ALWAYS -> "+$base"
      else -> base
    }
    amountDrawable.setText(signed, fakePosition)
  }

  companion object {

    const val SHOW_SIGN_IF_NEGATIVE = 0
    const val SHOW_SIGN_ALWAYS = 1

    const val FORMATTER_FULL = 0
    const val FORMATTER_IGNORE_ZERO_NANOS = 1

    private val fakePosition = AmountFieldPositions(
      cursorPosition = -1,
      fixedFractionPosition = FieldPosition(AmountFormatter.Field.FixedFraction),
      zeroNotationPosition = FieldPosition(AmountFormatter.Field.ZeroNotation),
    )
  }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(FORMATTER_FULL, FORMATTER_IGNORE_ZERO_NANOS)
  annotation class Formatter

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(SHOW_SIGN_IF_NEGATIVE, SHOW_SIGN_ALWAYS)
  annotation class ShowSign
}
