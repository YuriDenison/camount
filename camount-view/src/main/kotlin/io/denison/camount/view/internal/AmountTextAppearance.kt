package io.denison.camount.view.internal

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import io.denison.camount.view.R

internal data class AmountTextAppearance(
  val typeface: Typeface?,
  val fontFeatureSettings: String?,
  val textSize: Float,
  val lineHeight: Float,
  val letterSpacing: Float,
  val textColor: ColorStateList,
)

internal fun AmountTextAppearance.maybeOverrideTextColor(color: ColorStateList?) =
  if (color == null) this else copy(textColor = color)

internal fun AmountTextAppearance.overrideTextColor(color: ColorStateList) =
  copy(textColor = color)

internal fun Context.resolveAmountTextAppearance(textAppearanceId: Int): AmountTextAppearance {
  var typeface: Typeface? = null
  var fontFeatureSettings: String? = null
  var textSize = 0f
  var lineHeight = 0f
  var letterSpacing = 0f
  var textColor: ColorStateList? = null

  if (textAppearanceId != 0) {
    obtainStyledAttributesBlock(textAppearanceId, R.styleable.AmountTextAppearance) {
      val fontResourceId = getResourceId(R.styleable.AmountTextAppearance_android_fontFamily, 0)
      typeface = given(fontResourceId != 0) {
        runCatching {
          ResourcesCompat.getFont(this@resolveAmountTextAppearance,
            fontResourceId)
        }.getOrNull()
      }
      fontFeatureSettings = getString(R.styleable.AmountTextAppearance_android_fontFeatureSettings)
      textSize = getDimension(R.styleable.AmountTextAppearance_android_textSize, 0f)
      lineHeight = getDimension(R.styleable.AmountTextAppearance_lineHeight, textSize)
      letterSpacing = getFloat(R.styleable.AmountTextAppearance_android_letterSpacing, 0f)
      textColor = getColorStateList(R.styleable.AmountTextAppearance_android_textColor)
    }
  }

  return AmountTextAppearance(
    typeface = typeface,
    fontFeatureSettings = fontFeatureSettings,
    textSize = textSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    textColor = textColor ?: ColorStateList.valueOf(Color.BLACK),
  )
}
