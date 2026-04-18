package io.denison.camount.view.internal

import java.text.FieldPosition

internal fun FieldPosition.isValid(): Boolean = beginIndex < endIndex && beginIndex >= 0

internal fun FieldPosition.length() = if (isValid()) endIndex - beginIndex else 0

internal operator fun FieldPosition.contains(index: Int) = index in beginIndex until endIndex

internal fun FieldPosition.clear() {
  beginIndex = 0
  endIndex = 0
}

internal fun FieldPosition.offset(offset: Int) {
  if (isValid()) {
    beginIndex += offset
    endIndex += offset
  }
}
