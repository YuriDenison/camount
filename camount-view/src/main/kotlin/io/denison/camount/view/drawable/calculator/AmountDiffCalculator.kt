package io.denison.camount.view.drawable.calculator

import android.graphics.drawable.Drawable
import io.denison.camount.view.drawable.AmountDrawableAnimation
import io.denison.camount.view.drawable.AmountDrawableStyle
import io.denison.camount.view.drawable.AmountFieldPositions
import io.denison.camount.view.drawable.AmountGradientOrientation
import io.denison.camount.view.drawable.SymbolCellDrawable
import io.denison.camount.view.formatter.AmountConfig

internal interface AmountDiffCalculator {

  fun createCells(): List<SymbolCellDrawable>
  fun diff(
    cells: List<SymbolCellDrawable>,
    text: CharSequence,
    fieldPositions: AmountFieldPositions,
  ): List<SymbolCellDrawable>
}

internal fun AmountDrawableAnimation.calculator(
  config: AmountConfig,
  style: AmountDrawableStyle,
  newCell: () -> SymbolCellDrawable,
) = when (this) {
  AmountDrawableAnimation.EDIT -> AmountEditDiffCalculator(config, style, newCell)
  AmountDrawableAnimation.LEVENSHTEIN -> AmountLevenshteinDiffCalculator(config, style, newCell)
}

internal fun AmountGradientOrientation.calculator(startColor: Int, endColor: Int) = when (this) {
  AmountGradientOrientation.VERTICAL -> VerticalGradientCalculator(startColor, endColor)
  AmountGradientOrientation.HORIZONTAL -> HorizontalGradientCalculator(startColor, endColor)
}

internal inline fun <T : Drawable> List<T>.forEachVisible(action: (T) -> Unit) {
  forEach {
    if (it.isVisible) {
      action(it)
    }
  }
}

internal inline fun <T : Drawable> List<T>.forEachVisibleIndexed(action: (Int, T) -> Unit) {
  var index = 0
  forEach {
    if (it.isVisible) {
      action(index, it)
      index++
    }
  }
}

internal inline fun List<SymbolCellDrawable>.forEachAnimatingWithRetry(action: (Int, SymbolCellDrawable) -> Boolean) {
  forEachIndexed { index, cell ->
    if (cell.isVisible || cell.isRunning) {
      var retry: Boolean
      do {
        retry = action(index, cell)
      } while (retry)
    }
  }
}
