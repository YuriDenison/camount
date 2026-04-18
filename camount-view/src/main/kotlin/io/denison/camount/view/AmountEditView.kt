package io.denison.camount.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import io.denison.camount.view.drawable.AmountDrawable
import io.denison.camount.view.drawable.AmountDrawableAnimation
import io.denison.camount.view.drawable.AmountDrawableStyle
import io.denison.camount.view.drawable.AmountFieldPositions
import io.denison.camount.view.drawable.AmountGradientOrientation
import io.denison.camount.view.drawable.CursorStyle
import io.denison.camount.view.drawable.SymbolStyle
import io.denison.camount.view.formatter.AmountConfig
import io.denison.camount.view.formatter.AmountFormatter
import io.denison.camount.view.formatter.AmountStringBuilder
import io.denison.camount.view.internal.AmountTextAppearance
import io.denison.camount.view.internal.SimpleTextWatcher
import io.denison.camount.view.internal.asTrue
import io.denison.camount.view.internal.defaultDecimalFormat
import io.denison.camount.view.internal.dpi
import io.denison.camount.view.internal.given
import io.denison.camount.view.internal.horizontalPadding
import io.denison.camount.view.internal.maybeOverrideTextColor
import io.denison.camount.view.internal.obtainStyledAttributesBlock
import io.denison.camount.view.internal.overrideTextColor
import io.denison.camount.view.internal.resolveAmountTextAppearance
import io.denison.camount.view.internal.resolveSize
import io.denison.camount.view.internal.verticalPadding
import kotlinx.parcelize.Parcelize
import kotlin.math.floor

typealias AmountChangeListener = (Money) -> Unit

class AmountEditView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : View(context, attrs) {

  private val inputMethodManager: InputMethodManager?
    get() = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

  private val amountChangeListeners = mutableListOf<AmountChangeListener>()

  private val attributes: AmountEditAttributes

  private var config: AmountConfig
  private var amountDrawable: AmountDrawable
  private var inputFormatter: AmountFormatter

  private var input: Editable = SpannableStringBuilder()
    set(value) {
      field = value
      refreshInputMethodManager()
    }

  private var currencyCode = EUR
    set(value) {
      if (field != value) {
        field = value
        config = createConfig(value)
        amountDrawable = recreateAmountDrawable(config, attributes.style)
        inputFormatter = createInputFormatter(config, attributes.withFixedZeroNotation)
      }
    }

  private var internalAmount: Money = Money(0, 0, currencyCode)
    set(value) {
      if (field != value) {
        field = value
        notifyAmountListeners(value)
      }
    }

  var amount: Money
    get() = internalAmount
    set(value) {
      if (value != internalAmount) {
        when {
          value.currencyCode != internalAmount.currencyCode -> {
            currencyCode = value.currencyCode
            input = recreateInput(config, inputFormatter, inputFormatter.format(value))
          }

          !attributes.withFixedZeroNotation && value.isZero() -> {
            input.clear()
          }

          else -> {
            input.replace(0, input.length, inputFormatter.format(value))
          }
        }
      }
    }

  init {
    setLayerType(LAYER_TYPE_HARDWARE, null)
    isFocusableInTouchMode = true

    attributes = readAttributes(attrs)
    config = createConfig(currencyCode)
    amountDrawable = createAmountDrawable(config, attributes.style)
    inputFormatter = createInputFormatter(config, attributes.withFixedZeroNotation)

    val amount = internalAmount
    val source = if (!attributes.withFixedZeroNotation && amount.isZero()) ""
    else inputFormatter.format(amount)

    input = createInput(config, inputFormatter, source)

    if (!hasOnClickListeners()) setOnClickListener(null)
  }

  override fun onSaveInstanceState(): Parcelable = SavedState(
    super.onSaveInstanceState() ?: AbsSavedState.EMPTY_STATE,
    input.toString(),
    currencyCode
  )

  override fun onRestoreInstanceState(state: Parcelable?) {
    if (state !is SavedState) {
      super.onRestoreInstanceState(state)
      return
    }

    super.onRestoreInstanceState(state.superState)
    restoreState(state)
  }

  private fun restoreState(value: SavedState) {
    currencyCode = value.currencyCode
    input = recreateInput(config, inputFormatter, value.input)
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

  override fun setEnabled(enabled: Boolean) {
    if (isEnabled == enabled) {
      return
    }

    if (!enabled) {
      hideKeyboard()
    }

    super.setEnabled(enabled)

    refreshInputMethodManager()
  }

  override fun setOnClickListener(l: OnClickListener?) {
    super.setOnClickListener {
      requestFocus()
      showKeyboard()
      l?.onClick(it)
    }
  }

  override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    amountDrawable.setCursorVisible(gainFocus)
  }

  private fun showKeyboard() {
    if (attributes.enableInputConnection && isEnabled) {
      inputMethodManager?.showSoftInput(this, 0)
    }
  }

  private fun hideKeyboard() {
    inputMethodManager?.let {
      if (it.isActive(this)) {
        it.hideSoftInputFromWindow(windowToken, 0)
      }
    }
  }

  private fun refreshInputMethodManager() {
    if (attributes.enableInputConnection && isEnabled) {
      inputMethodManager?.restartInput(this)
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    return onKeyCode(keyCode) || super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    return when (keyCode) {
      KeyEvent.KEYCODE_ENTER -> handleEnter().asTrue()
      else -> false
    }
  }

  override fun onCheckIsTextEditor() = attributes.enableInputConnection

  override fun onCreateInputConnection(outAttrs: EditorInfo) =
    given(attributes.enableInputConnection && isEnabled) {
      outAttrs.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

      var imeOptions = attributes.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
      }
      if (focusSearch(FOCUS_DOWN) != null) {
        imeOptions = imeOptions or EditorInfo.IME_FLAG_NAVIGATE_NEXT
      }

      outAttrs.imeOptions = imeOptions

      outAttrs.initialSelEnd = Selection.getSelectionStart(input)
      outAttrs.initialSelStart = Selection.getSelectionEnd(input)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        outAttrs.setInitialSurroundingText(input)
      }

      object : BaseInputConnection(this, true) {
        override fun getEditable() = input
      }
    }

  fun onKeyCode(code: Int): Boolean = when (code) {
    KeyEvent.KEYCODE_0,
    KeyEvent.KEYCODE_NUMPAD_0,
      -> appendLast(config.getDigit(0)).asTrue()

    KeyEvent.KEYCODE_1,
    KeyEvent.KEYCODE_NUMPAD_1,
      -> appendLast(config.getDigit(1)).asTrue()

    KeyEvent.KEYCODE_2,
    KeyEvent.KEYCODE_NUMPAD_2,
      -> appendLast(config.getDigit(2)).asTrue()

    KeyEvent.KEYCODE_3,
    KeyEvent.KEYCODE_NUMPAD_3,
      -> appendLast(config.getDigit(3)).asTrue()

    KeyEvent.KEYCODE_4,
    KeyEvent.KEYCODE_NUMPAD_4,
      -> appendLast(config.getDigit(4)).asTrue()

    KeyEvent.KEYCODE_5,
    KeyEvent.KEYCODE_NUMPAD_5,
      -> appendLast(config.getDigit(5)).asTrue()

    KeyEvent.KEYCODE_6,
    KeyEvent.KEYCODE_NUMPAD_6,
      -> appendLast(config.getDigit(6)).asTrue()

    KeyEvent.KEYCODE_7,
    KeyEvent.KEYCODE_NUMPAD_7,
      -> appendLast(config.getDigit(7)).asTrue()

    KeyEvent.KEYCODE_8,
    KeyEvent.KEYCODE_NUMPAD_8,
      -> appendLast(config.getDigit(8)).asTrue()

    KeyEvent.KEYCODE_9,
    KeyEvent.KEYCODE_NUMPAD_9,
      -> appendLast(config.getDigit(9)).asTrue()

    KeyEvent.KEYCODE_PERIOD,
    KeyEvent.KEYCODE_COMMA,
    KeyEvent.KEYCODE_NUMPAD_DOT,
    KeyEvent.KEYCODE_NUMPAD_COMMA,
      -> appendLast(config.localizedDecimalSeparator).asTrue()

    KeyEvent.KEYCODE_DEL,
    KeyEvent.KEYCODE_FORWARD_DEL,
      -> deleteLast().asTrue()

    KeyEvent.KEYCODE_ENTER -> true
    else -> false
  }

  private fun appendLast(char: Char) {
    if (isEnabled) input.append(char)
  }

  private fun deleteLast() {
    if (isEnabled && input.isNotEmpty()) {
      with(input) { delete(length - 1, length) }
    }
  }

  private fun handleEnter() {
    if (hasFocus()) {
      focusSearch(FOCUS_DOWN)?.requestFocus() ?: run {
        clearFocus()
        hideKeyboard()
      }
    }
  }

  private fun readAttributes(attrs: AttributeSet?): AmountEditAttributes {
    var gravity = Gravity.NO_GRAVITY
    var textAppearance = AmountTextAppearance(
      typeface = null,
      fontFeatureSettings = null,
      textSize = 0f,
      lineHeight = 0f,
      letterSpacing = 0f,
      textColor = ColorStateList.valueOf(Color.BLACK),
    )
    var cursorVisible = false
    var cursorColor = Color.BLACK
    var defaultTextColor: ColorStateList? = null
    var withFixedZeroNotation = true
    var fixedFractionTextColor: ColorStateList? = null
    var zeroNotationTextColor: ColorStateList? = null
    @ColorInt var amountGradientStartColor = Color.BLACK
    @ColorInt var amountGradientEndColor = Color.BLACK
    var orientation = AmountGradientOrientation.VERTICAL
    var enableInputConnection = false
    var imeOptions = EditorInfo.IME_NULL

    context.obtainStyledAttributesBlock(attrs, R.styleable.AmountEditView) {
      gravity = getInteger(R.styleable.AmountEditView_android_gravity, Gravity.NO_GRAVITY)

      val textAppearanceId = getResourceId(R.styleable.AmountEditView_android_textAppearance, 0)
      textAppearance = context.resolveAmountTextAppearance(textAppearanceId)

      cursorVisible = getBoolean(R.styleable.AmountEditView_android_cursorVisible, false)
      cursorColor = getColor(R.styleable.AmountEditView_amount_cursorColor, Color.BLACK)
      defaultTextColor = getColorStateList(R.styleable.AmountEditView_android_textColor)
      zeroNotationTextColor =
        getColorStateList(R.styleable.AmountEditView_amount_zeroNotationTextColor)
      fixedFractionTextColor =
        getColorStateList(R.styleable.AmountEditView_amount_fixedFractionTextColor)

      amountGradientStartColor =
        getColor(R.styleable.AmountEditView_amount_gradientStartColor, Color.BLACK)
      amountGradientEndColor =
        getColor(R.styleable.AmountEditView_amount_gradientEndColor, Color.BLACK)
      orientation = when (getInteger(R.styleable.AmountEditView_amount_gradientOrientation,
        GRADIENT_ORIENTATION_VERTICAL)) {
        GRADIENT_ORIENTATION_HORIZONTAL -> AmountGradientOrientation.HORIZONTAL
        else -> AmountGradientOrientation.VERTICAL
      }

      withFixedZeroNotation =
        getBoolean(R.styleable.AmountEditView_amount_withFixedZeroNotation, true)
      enableInputConnection =
        getBoolean(R.styleable.AmountEditView_amount_enableInputConnection, false)
      imeOptions = getInteger(R.styleable.AmountEditView_android_imeOptions, EditorInfo.IME_NULL)
    }

    return AmountEditAttributes(
      style = AmountDrawableStyle(
        gravity = gravity,
        gradientStartColor = amountGradientStartColor,
        gradientEndColor = amountGradientEndColor,
        gradientOrientation = orientation,
        cursorStyle = given(cursorVisible) {
          CursorStyle(
            color = cursorColor,
            height = floor(textAppearance.textSize).toInt(),
            width = context.resources.dpi(2)
          )
        },
        defaultSymbolStyle = SymbolStyle(
          textAppearance = textAppearance.maybeOverrideTextColor(defaultTextColor),
          type = SymbolStyle.StyleType.SHADER
        ),
        symbolStyles = mutableMapOf<AmountFormatter.Field, SymbolStyle>().apply {
          zeroNotationTextColor?.let { textColor ->
            this[AmountFormatter.Field.ZeroNotation] = SymbolStyle(
              textAppearance = textAppearance.overrideTextColor(textColor),
              type = SymbolStyle.StyleType.COLOR
            )
          }

          fixedFractionTextColor?.let { textColor ->
            this[AmountFormatter.Field.FixedFraction] = SymbolStyle(
              textAppearance = textAppearance.overrideTextColor(textColor),
              type = SymbolStyle.StyleType.COLOR
            )
          }
        }
      ),
      withFixedZeroNotation = withFixedZeroNotation,
      enableInputConnection = enableInputConnection,
      imeOptions = imeOptions
    )
  }

  private fun createConfig(currencyCode: String) = AmountConfig(
    maximumNotationDigits = 5,
    decimalFormat = defaultDecimalFormat(currencyCode)
  )

  private fun createAmountDrawable(config: AmountConfig, style: AmountDrawableStyle) =
    AmountDrawable(
      config = config,
      style = style,
      animation = AmountDrawableAnimation.EDIT
    ).also {
      it.setBounds(
        paddingLeft,
        paddingTop,
        width - paddingRight,
        height - paddingBottom
      )
      it.state = drawableState
      it.setCursorVisible(isFocused)
      it.callback = this
    }

  private fun recreateAmountDrawable(
    config: AmountConfig,
    style: AmountDrawableStyle,
  ): AmountDrawable {
    amountDrawable.stop()
    amountDrawable.callback = null
    return createAmountDrawable(config, style)
  }

  private fun createInputFormatter(config: AmountConfig, withFixedZeroNotation: Boolean) =
    AmountFormatter(
      config = config,
      withCurrency = false,
      withGroupingSeparators = false,
      withFixedFractionLength = false,
      withFixedZeroNotation = withFixedZeroNotation,
    )

  private fun createInput(config: AmountConfig, formatter: AmountFormatter, source: CharSequence) =
    AmountStringBuilder(source, formatter).apply {
      val watcher = InputChangesWatcher(config)
      watcher.afterTextChanged(this)
      setSpan(watcher, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    }

  private fun recreateInput(
    config: AmountConfig,
    formatter: AmountFormatter,
    source: CharSequence,
  ): Editable {
    input.clearSpans()
    return createInput(config, formatter, source)
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

  fun addAmountChangeListener(listener: AmountChangeListener) {
    amountChangeListeners.add(listener)
  }

  fun removeAmountChangeListener(listener: AmountChangeListener) {
    amountChangeListeners.remove(listener)
  }

  private fun notifyAmountListeners(amount: Money) {
    amountChangeListeners.forEach { it(amount) }
  }

  private inner class InputChangesWatcher(config: AmountConfig) : SimpleTextWatcher() {

    private val formatter = AmountFormatter(config)
    private var before: String? = null

    override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
      before = text.toString()
    }

    override fun afterTextChanged(editable: Editable) {
      if (!TextUtils.equals(before, editable)) {
        internalAmount = parse(editable, currencyCode)
        setText(editable)
      }
    }

    private fun setText(editable: Editable) {
      val cursorPosition =
        Selection.getSelectionStart(editable).let { if (it < 0) editable.length else it }

      amountDrawable.setText(
        text = formatter.format(
          source = editable,
          start = cursorPosition,
          end = editable.length,
          text = editable,
          textStart = cursorPosition,
          textEnd = editable.length
        ),
        fieldPositions = AmountFieldPositions(
          cursorPosition = formatter.cursorPosition,
          fixedFractionPosition = formatter.fixedFractionPosition,
          zeroNotationPosition = formatter.zeroNotationPosition,
        )
      )
    }

    private fun parse(source: CharSequence, currencyCode: String): Money {
      val number = formatter.parse(source) ?: return Money(0, 0, currencyCode)
      return number.toMoney(currencyCode)
    }
  }

  @Parcelize
  class SavedState(
    val superState: Parcelable?,
    val input: String,
    val currencyCode: String,
  ) : Parcelable

  companion object {

    const val GRADIENT_ORIENTATION_VERTICAL = 0
    const val GRADIENT_ORIENTATION_HORIZONTAL = 1
  }
}

private class AmountEditAttributes(
  val style: AmountDrawableStyle,
  val withFixedZeroNotation: Boolean,
  val enableInputConnection: Boolean,
  val imeOptions: Int,
)
