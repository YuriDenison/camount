package io.denison.camount.view.drawable.calculator

import androidx.core.util.Pools
import io.denison.camount.view.drawable.AmountDrawableStyle
import io.denison.camount.view.drawable.AmountFieldPositions
import io.denison.camount.view.drawable.SymbolCellDrawable
import io.denison.camount.view.formatter.AmountConfig
import io.denison.camount.view.internal.asFalse

internal class AmountLevenshteinDiffCalculator(
  private val config: AmountConfig,
  private val style: AmountDrawableStyle,
  private val newCell: () -> SymbolCellDrawable,
) : AmountDiffCalculator {

  private val maxLength = config.localizedPrefix.length +
          config.maximumNotationDigits +
          (if (config.groupingSize == 0) 0 else (config.maximumNotationDigits - config.groupingSize).coerceAtLeast(
            0)) +
          1 + config.maximumFractionDigits +
          config.localizedSuffix.length

  private val pool = Pools.SimplePool<ArrayList<SymbolCellDrawable>>(2).apply {
    release(ArrayList(maxLength))
    release(ArrayList(maxLength))
  }

  override fun createCells(): ArrayList<SymbolCellDrawable> = pool.acquire()!!
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
    cells.forEachAnimatingWithRetry { _, cell ->
      result.add(cell).asFalse()
    }
    releaseCells(cells)

    val matrix = levenshteinMatrix(result, text)
    result.apply(text, matrix)
    return result
  }

  private fun levenshteinMatrix(
    cells: List<SymbolCellDrawable>,
    text: CharSequence,
  ): Array<IntArray> {
    val xLength = cells.size
    val yLength = text.length
    val dp = Array(xLength + 1) { IntArray(yLength + 1) }

    for (i in 0..xLength) {
      for (j in 0..yLength) {
        when {
          i == 0 -> dp[i][j] = j
          j == 0 -> dp[i][j] = i

          else -> {
            val cell = cells[i - 1]
            val c1 = cell.char
            val c2 = text[j - 1]

            val replaceCost = dp[i - 1][j - 1] + if (c1 == c2) 0 else 1
            val insertCost = dp[i][j - 1] + 1
            val deleteCost = dp[i - 1][j] + 1

            dp[i][j] = minOf(
              replaceCost,
              insertCost,
              deleteCost,
            )
          }
        }
      }
    }
    return dp
  }

  private fun MutableList<SymbolCellDrawable>.apply(text: CharSequence, matrix: Array<IntArray>) {
    var i = size
    var j = text.length

    while (i >= 0 && j >= 0) {
      when {
        i == 0 && j == 0 -> break

        i == 0 && j > 0 -> {
          val to = text[j - 1]
          j -= 1
          insert(i, to)
        }

        i > 0 && j == 0 -> {
          i -= 1
          delete(i)
        }

        else -> {
          val replaceCost = matrix[i - 1][j - 1]
          val deleteCost = matrix[i - 1][j]
          val insertCost = matrix[i][j - 1]

          val from = this[i - 1].char
          val to = text[j - 1]

          when {
            replaceCost <= insertCost && replaceCost <= deleteCost -> {
              when {
                from.isSeparator() && !to.isSeparator() -> {
                  i -= 1
                  delete(i)
                }

                to.isSeparator() && !from.isSeparator() -> {
                  j -= 1
                  insert(i, to)
                }

                else -> {
                  i -= 1
                  j -= 1
                  replace(i, to)
                }
              }
            }

            insertCost <= replaceCost && insertCost <= deleteCost -> {
              j -= 1
              insert(i, to)
            }

            deleteCost <= replaceCost && deleteCost <= insertCost -> {
              i -= 1
              delete(i)
            }

            else -> error("never happen")
          }
        }
      }
    }
  }

  private fun MutableList<SymbolCellDrawable>.insert(index: Int, char: Char) {
    val cell = newCell()
    cell.replace(char, style.defaultSymbolStyle)
    add(index, cell)
  }

  private fun MutableList<SymbolCellDrawable>.delete(index: Int) {
    val cell = this[index]
    cell.delete()
  }

  private fun MutableList<SymbolCellDrawable>.replace(index: Int, char: Char) {
    val cell = this[index]
    cell.replace(char, style.defaultSymbolStyle)
  }

  private fun Char.isSeparator() =
    config.isDecimalSeparator(this) || config.isGroupingSeparator(this)
}
