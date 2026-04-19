package io.denison.camount.view.drawable.calculator

import android.graphics.Rect
import android.view.Gravity
import io.denison.camount.view.drawable.AmountDrawableStyle
import io.denison.camount.view.drawable.CursorCellDrawable
import io.denison.camount.view.drawable.SymbolCellDrawable
import io.denison.camount.view.internal.given
import kotlin.math.max

internal interface AmountBoundsCalculator {

  fun calculateBounds(
    symbols: List<SymbolCellDrawable>,
    cursor: CursorCellDrawable?,
    cursorPosition: Int,
    bounds: Rect,
  )

  fun calculateIntrinsicWidth(cells: List<SymbolCellDrawable>): Int
  fun calculateIntrinsicHeight(cells: List<SymbolCellDrawable>): Int
}

internal class DefaultBoundsCalculator(
  private val style: AmountDrawableStyle,
) : AmountBoundsCalculator {

  private val calculationRect = Rect()

  private val gradientCalculator = style.gradientOrientation.calculator(
    startColor = style.gradientStartColor,
    endColor = style.gradientEndColor,
  )

  override fun calculateBounds(
    symbols: List<SymbolCellDrawable>,
    cursor: CursorCellDrawable?,
    cursorPosition: Int,
    bounds: Rect,
  ) {
    val width = bounds.width()
    val height = bounds.height()

    var visibleSymbolsWidth = 0
    var visibleSymbolsHeight = 0
    var gradientSymbolsWidth = 0
    symbols.forEachVisible {
      val intrinsicWidth = it.intrinsicWidth
      val intrinsicHeight = it.intrinsicHeight

      visibleSymbolsHeight = max(visibleSymbolsHeight, intrinsicHeight)
      visibleSymbolsWidth += intrinsicWidth
      if (it.canApplyGradient()) gradientSymbolsWidth += intrinsicWidth
    }
    cursor?.let {
      visibleSymbolsHeight = max(visibleSymbolsHeight, it.intrinsicHeight)
      visibleSymbolsWidth += it.intrinsicWidth
    }

    gradientCalculator.setSize(gradientSymbolsWidth, height)

    val symbolScale: Float
    var leftGradientOffset = 0

    if (width < visibleSymbolsWidth) {
      symbolScale = width.toFloat() / visibleSymbolsWidth

      Gravity.apply(
        style.gravity,
        width,
        (visibleSymbolsHeight * symbolScale).toInt(),
        bounds,
        calculationRect,
      )
    } else {
      symbolScale = 1f

      Gravity.apply(
        style.gravity,
        visibleSymbolsWidth,
        visibleSymbolsHeight,
        bounds,
        calculationRect,
      )
    }

    val symbolTopOffset = calculationRect.top
    var symbolLeftOffset = calculationRect.left

    var cursorLeftOffset = calculationRect.left

    symbols.forEachVisibleIndexed { index, it ->
      val originalSymbolWidth = it.intrinsicWidth
      val originalSymbolHeight = it.intrinsicHeight

      val symbolWidth = (originalSymbolWidth * symbolScale).toInt()
      val symbolHeight = (originalSymbolHeight * symbolScale).toInt()

      calculationRect.set(0, 0, symbolWidth, symbolHeight)
      calculationRect.offset(symbolLeftOffset, symbolTopOffset)
      symbolLeftOffset = calculationRect.right

      it.setTargetBounds(calculationRect)

      val gradient = given(it.canApplyGradient()) {
        calculationRect.set(0, 0, originalSymbolWidth, originalSymbolHeight)
        calculationRect.offset(leftGradientOffset, 0)
        leftGradientOffset = calculationRect.right

        gradientCalculator.calculate(calculationRect)
      }

      it.setShader(gradient)

      if (index + 1 == cursorPosition) {
        cursorLeftOffset = symbolLeftOffset
      }
    }

    if (cursor != null) {
      val originalCursorWidth = cursor.intrinsicWidth
      val originalCursorHeight = cursor.intrinsicHeight

      val cursorWidth = (originalCursorWidth * symbolScale).toInt()
      val cursorHeight = (originalCursorHeight * symbolScale).toInt()
      calculationRect.set(0, 0, cursorWidth, cursorHeight)

      val cursorTopOffset =
        symbolTopOffset + ((visibleSymbolsHeight - originalCursorHeight) * symbolScale / 2).toInt()
      calculationRect.offset(cursorLeftOffset, cursorTopOffset)

      cursor.setTargetBounds(calculationRect)
    }
  }

  override fun calculateIntrinsicWidth(cells: List<SymbolCellDrawable>): Int {
    var result = 0
    cells.forEachVisible {
      result += it.intrinsicWidth
    }
    return result
  }

  override fun calculateIntrinsicHeight(cells: List<SymbolCellDrawable>): Int {
    var result = 0
    cells.forEachVisible {
      result = max(result, it.intrinsicHeight)
    }
    return result
  }
}

private fun SymbolCellDrawable.canApplyGradient() =
  char.let { !it.isWhitespace() && it != 0.toChar() }
