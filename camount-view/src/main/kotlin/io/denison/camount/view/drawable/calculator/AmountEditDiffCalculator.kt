package io.denison.camount.view.drawable.calculator

import androidx.core.util.Pools
import io.denison.camount.view.drawable.AmountDrawableStyle
import io.denison.camount.view.drawable.AmountFieldPositions
import io.denison.camount.view.drawable.SymbolCellDrawable
import io.denison.camount.view.formatter.AmountConfig
import io.denison.camount.view.internal.contains

internal class AmountEditDiffCalculator(
  private val config: AmountConfig,
  private val style: AmountDrawableStyle,
  private val newCell: () -> SymbolCellDrawable,
) : AmountDiffCalculator {

  private val maxLength = config.localizedPrefix.length +
    config.maximumNotationDigits +
    (if (config.groupingSize == 0) 0 else (config.maximumNotationDigits - config.groupingSize).coerceAtLeast(0)) +
    1 +
    1 +
    config.maximumFractionDigits +
    config.localizedSuffix.length

  private val pool = Pools.SimplePool<ArrayList<SymbolCellDrawable>>(2).apply {
    release(ArrayList(maxLength))
    release(ArrayList(maxLength))
  }

  override fun createCells() = pool.acquire()!!
  private fun releaseCells(cells: List<SymbolCellDrawable>) {
    cells as ArrayList<SymbolCellDrawable>
    cells.clear()
    pool.release(cells)
  }

  override fun diff(
    cells: List<SymbolCellDrawable>,
    text: CharSequence,
    fieldPositions: AmountFieldPositions,
  ): List<SymbolCellDrawable> {
    val result = createCells()

    var textIndex = 0

    cells.forEachAnimatingWithRetry { _, curCell ->
      val cell: SymbolCellDrawable
      val retry: Boolean

      if (textIndex >= text.length) {
        curCell.delete()
        cell = curCell
        retry = false
      } else {
        val s1 = curCell.char
        val s2 = text[textIndex]
        val style = calculateStyle(textIndex, fieldPositions)

        when {
          s1 == s2 -> {
            curCell.replace(s2, style)
            textIndex++
            cell = curCell
            retry = false
          }

          config.isGroupingSeparator(s1) || config.isDecimalSeparator(s1) -> {
            curCell.delete()
            cell = curCell
            retry = false
          }

          config.isGroupingSeparator(s2) || config.isDecimalSeparator(s2) -> {
            val newCell = newCell()
            newCell.replace(s2, style)
            textIndex++
            cell = newCell
            retry = true
          }

          else -> {
            val s1IsDigit = config.isDigit(s1)
            val s2IsDigit = config.isDigit(s2)

            when {
              s1IsDigit -> when {
                s2IsDigit -> {
                  curCell.replace(s2, style)
                  textIndex++
                  cell = curCell
                  retry = false
                }

                else -> {
                  curCell.delete()
                  cell = curCell
                  retry = false
                }
              }

              else -> when {
                s2IsDigit -> {
                  val newCell = newCell()
                  newCell.replace(s2, style)
                  textIndex++
                  cell = newCell
                  retry = true
                }

                else -> {
                  curCell.replace(s2, style)
                  textIndex++
                  cell = curCell
                  retry = false
                }
              }
            }
          }
        }
      }

      result.add(cell)
      retry
    }
    releaseCells(cells)

    val restCount = minOf(text.length, maxLength)
    while (textIndex < restCount) {
      val s = text[textIndex]
      val style = calculateStyle(textIndex, fieldPositions)
      textIndex++

      val cell = newCell()
      cell.replace(s, style)

      result.add(cell)
    }

    return result
  }

  private fun calculateStyle(textIndex: Int, fieldPositions: AmountFieldPositions) =
    when (textIndex) {
      in fieldPositions.zeroNotationPosition -> style.symbolStyles[fieldPositions.zeroNotationPosition.fieldAttribute]
      in fieldPositions.fixedFractionPosition -> style.symbolStyles[fieldPositions.fixedFractionPosition.fieldAttribute]
      else -> style.defaultSymbolStyle
    } ?: style.defaultSymbolStyle
}
