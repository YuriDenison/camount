package io.denison.camount.compose.internal

import io.denison.camount.compose.AmountStyle
import io.denison.camount.formatter.AmountConfig
import io.denison.camount.formatter.AmountField
import io.denison.camount.formatter.AmountFieldPositions

internal enum class DiffMode { Edit, Levenshtein }

internal interface DiffCalculator {

  fun diff(
    cells: MutableList<SymbolCell>,
    text: CharSequence,
    positions: AmountFieldPositions,
  ): MutableList<SymbolCell>
}

internal fun createDiffCalculator(
  mode: DiffMode,
  config: AmountConfig,
  style: AmountStyle,
  newCell: () -> SymbolCell,
): DiffCalculator = when (mode) {
  DiffMode.Edit -> EditDiffCalculator(config, newCell)
  DiffMode.Levenshtein -> LevenshteinDiffCalculator(config, newCell)
}

private fun fieldAt(positions: AmountFieldPositions, index: Int): AmountField? = when (index) {
  in positions.zeroNotation -> AmountField.ZeroNotation
  in positions.fixedFraction -> AmountField.FixedFraction
  else -> null
}

private inline fun MutableList<SymbolCell>.forEachAnimatingWithRetry(
  action: (Int, SymbolCell) -> Boolean,
) {
  var index = 0
  while (index < size) {
    val cell = this[index]
    if (cell.isVisible || cell.isRunning) {
      var retry: Boolean
      do {
        retry = action(index, cell)
      } while (retry)
    }
    index++
  }
}

internal class EditDiffCalculator(
  private val config: AmountConfig,
  private val newCell: () -> SymbolCell,
) : DiffCalculator {

  override fun diff(
    cells: MutableList<SymbolCell>,
    text: CharSequence,
    positions: AmountFieldPositions,
  ): MutableList<SymbolCell> {
    val result = ArrayList<SymbolCell>(config.maximumFormattedSymbols)

    var textIndex = 0

    cells.forEachAnimatingWithRetry { _, curCell ->
      val cell: SymbolCell
      val retry: Boolean

      if (textIndex >= text.length) {
        curCell.delete()
        cell = curCell
        retry = false
      } else {
        val s1 = curCell.currentChar
        val s2 = text[textIndex]
        val field = fieldAt(positions, textIndex)

        when {
          s1 == s2 -> {
            curCell.replace(s2, field)
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
            val fresh = newCell()
            fresh.replace(s2, field)
            textIndex++
            cell = fresh
            retry = true
          }

          else -> {
            val s1IsDigit = config.isDigit(s1)
            val s2IsDigit = config.isDigit(s2)

            when {
              s1IsDigit -> when {
                s2IsDigit -> {
                  curCell.replace(s2, field)
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
                  val fresh = newCell()
                  fresh.replace(s2, field)
                  textIndex++
                  cell = fresh
                  retry = true
                }
                else -> {
                  curCell.replace(s2, field)
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

    val restCount = minOf(text.length, config.maximumFormattedSymbols)
    while (textIndex < restCount) {
      val s = text[textIndex]
      val field = fieldAt(positions, textIndex)
      textIndex++

      val cell = newCell()
      cell.replace(s, field)
      result.add(cell)
    }
    return result
  }
}

internal class LevenshteinDiffCalculator(
  private val config: AmountConfig,
  private val newCell: () -> SymbolCell,
) : DiffCalculator {

  override fun diff(
    cells: MutableList<SymbolCell>,
    text: CharSequence,
    positions: AmountFieldPositions,
  ): MutableList<SymbolCell> {
    val result = ArrayList<SymbolCell>(cells.size + text.length)
    cells.forEachAnimatingWithRetry { _, cell ->
      result.add(cell)
      false
    }

    val matrix = levenshteinMatrix(result, text)
    applyDiff(result, text, matrix, positions)
    return result
  }

  private fun levenshteinMatrix(
    cells: List<SymbolCell>,
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
            val c1 = cell.currentChar
            val c2 = text[j - 1]

            val replaceCost = dp[i - 1][j - 1] + if (c1 == c2) 0 else 1
            val insertCost = dp[i][j - 1] + 1
            val deleteCost = dp[i - 1][j] + 1

            dp[i][j] = minOf(replaceCost, insertCost, deleteCost)
          }
        }
      }
    }
    return dp
  }

  private fun applyDiff(
    cells: MutableList<SymbolCell>,
    text: CharSequence,
    matrix: Array<IntArray>,
    positions: AmountFieldPositions,
  ) {
    var i = cells.size
    var j = text.length

    while (i >= 0 && j >= 0) {
      when {
        i == 0 && j == 0 -> break
        i == 0 && j > 0 -> {
          j -= 1
          cells.insert(i, text[j], fieldAt(positions, j))
        }
        i > 0 && j == 0 -> {
          i -= 1
          cells[i].delete()
        }
        else -> {
          val replaceCost = matrix[i - 1][j - 1]
          val deleteCost = matrix[i - 1][j]
          val insertCost = matrix[i][j - 1]

          val from = cells[i - 1].currentChar
          val to = text[j - 1]

          val minCost = minOf(replaceCost, insertCost, deleteCost)
          when (minCost) {
            replaceCost -> when {
              from.isSeparator() && !to.isSeparator() -> {
                i -= 1
                cells[i].delete()
              }

              to.isSeparator() && !from.isSeparator() -> {
                j -= 1
                cells.insert(i, to, fieldAt(positions, j))
              }

              else -> {
                i -= 1
                j -= 1
                cells[i].replace(to, fieldAt(positions, j))
              }
            }
            insertCost -> {
              j -= 1
              cells.insert(i, to, fieldAt(positions, j))
            }
            else -> {
              i -= 1
              cells[i].delete()
            }
          }
        }
      }
    }
  }

  private fun MutableList<SymbolCell>.insert(index: Int, char: Char, field: AmountField?) {
    val cell = newCell()
    cell.replace(char, field)
    add(index, cell)
  }

  private fun Char.isSeparator(): Boolean = config.isDecimalSeparator(this) || config.isGroupingSeparator(this)
}
