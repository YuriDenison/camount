package io.denison.camount.view.internal

import android.graphics.drawable.GradientDrawable

internal fun roundedRectDrawable(color: Int, radiusPx: Int): GradientDrawable =
  GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(color)
    cornerRadius = radiusPx.toFloat().coerceAtLeast(1f)
  }
