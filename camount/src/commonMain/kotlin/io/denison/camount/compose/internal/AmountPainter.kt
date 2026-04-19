package io.denison.camount.compose.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import io.denison.camount.compose.AmountStyle
import io.denison.camount.compose.HorizontalAlignment
import io.denison.camount.formatter.AmountConfig
import io.denison.camount.formatter.AmountFieldPositions
import kotlinx.coroutines.CoroutineScope
import kotlin.math.max

internal class AmountPainter(
  private val measurer: TextMeasurer,
  private var style: AmountStyle,
  private val mode: DiffMode,
  private var config: AmountConfig,
  private val scope: CoroutineScope,
) {

  private var alignment: HorizontalAlignment = HorizontalAlignment.Center

  private val newCell: () -> SymbolCell = {
    SymbolCell(measurer, this.style, scope).also { it.setDuration(DIFF_ANIMATION_DURATION_MS) }
  }

  private var diff: DiffCalculator = createDiffCalculator(mode, config, style, newCell)

  private var cells: MutableList<SymbolCell> by mutableStateOf(ArrayList())
  private var cursorPositionIndex: Int = -1
  private var cursor: CursorCell? = style.cursor?.let { CursorCell(it, scope) }

  private var containerWidth: Float = 0f
  private var containerHeight: Float = 0f

  var intrinsicWidth: Float by mutableStateOf(0f)
    private set
  var intrinsicHeight: Float by mutableStateOf(0f)
    private set

  fun updateStyle(newStyle: AmountStyle, newConfig: AmountConfig, newAlignment: HorizontalAlignment) {
    val cursorStyleChanged = newStyle.cursor != style.cursor
    val configChanged = newConfig != config
    val styleChanged = newStyle != style
    val alignmentChanged = newAlignment != alignment
    style = newStyle
    alignment = newAlignment
    if (configChanged) config = newConfig
    if (cursorStyleChanged) {
      cursor = newStyle.cursor?.let { CursorCell(it, scope) }
    }
    if (configChanged || styleChanged) {
      diff = createDiffCalculator(mode, config, style, newCell)
    }
    if (alignmentChanged) layout()
  }

  fun setText(text: CharSequence, positions: AmountFieldPositions) {
    cells = diff.diff(cells, text, positions)
    cursorPositionIndex = positions.cursorPosition
    calculateIntrinsic()
    layout()
  }

  fun setBounds(width: Float, height: Float) {
    if (containerWidth == width && containerHeight == height) return
    containerWidth = width
    containerHeight = height
    layout()
  }

  fun setCursorVisible(visible: Boolean) {
    cursor?.setVisible(visible)
  }

  private fun calculateIntrinsic() {
    var w = 0f
    var h = 0f
    for (cell in cells) {
      if (!cell.isVisible) continue
      w += cell.intrinsicWidth
      h = max(h, cell.intrinsicHeight)
    }
    intrinsicWidth = w
    intrinsicHeight = h
  }

  private fun layout() {
    if (containerWidth <= 0f || containerHeight <= 0f) return

    var visibleWidth = 0f
    var visibleHeight = 0f
    for (cell in cells) {
      if (!cell.isVisible) continue
      visibleWidth += cell.intrinsicWidth
      visibleHeight = max(visibleHeight, cell.intrinsicHeight)
    }
    val cursorStyle = style.cursor
    val cursor = cursor
    if (cursor != null && cursorStyle != null) {
      val cursorW = cursorStyle.width.value * densityPx
      val cursorH = visibleHeight * cursorStyle.heightFraction
      visibleWidth += cursorW
      visibleHeight = max(visibleHeight, cursorH)
    }

    val scale: Float = if (containerWidth < visibleWidth && visibleWidth > 0f) containerWidth / visibleWidth else 1f
    val scaledWidth = visibleWidth * scale
    val scaledHeight = visibleHeight * scale

    val top = (containerHeight - scaledHeight) / 2f
    var left = when (alignment) {
      HorizontalAlignment.Start -> 0f
      HorizontalAlignment.Center -> (containerWidth - scaledWidth) / 2f
      HorizontalAlignment.End -> containerWidth - scaledWidth
    }

    var cursorLeft = left
    var visibleIndex = 0

    for (cell in cells) {
      if (!cell.isVisible) continue
      val w = cell.intrinsicWidth * scale
      val h = cell.intrinsicHeight * scale
      cell.setTargetBounds(left, top, w, h)
      left += w
      visibleIndex++
      if (visibleIndex == cursorPositionIndex) cursorLeft = left
    }

    if (cursor != null && cursorStyle != null) {
      val cursorW = cursorStyle.width.value * densityPx * scale
      val cursorH = visibleHeight * cursorStyle.heightFraction * scale
      val cursorTop = top + (scaledHeight - cursorH) / 2f
      cursor.setTargetBounds(cursorLeft, cursorTop, cursorW, cursorH)
    }
  }

  private var densityPx: Float = 1f

  fun setDensity(density: Float) {
    if (densityPx != density) {
      densityPx = density
      layout()
    }
  }

  fun draw(drawScope: DrawScope) {
    val brush: Brush? = style.gradientBrush
    for (i in cells.indices.reversed()) cells[i].draw(drawScope, brush)
    cursor?.draw(drawScope)
  }
}
