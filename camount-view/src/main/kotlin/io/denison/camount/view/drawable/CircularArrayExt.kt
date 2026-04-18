package io.denison.camount.view.drawable

import androidx.collection.CircularArray
import kotlin.math.max

internal inline fun <E> CircularArray<E>.forEach(skipEnd: Int = 0, block: (E) -> Unit) {
  var from = 0
  val to = size() - skipEnd
  while (from < to) {
    block(get(from))
    from++
  }
}

internal inline fun <E> CircularArray<E>.maxBy(block: (E) -> Int): Int {
  var result = Int.MIN_VALUE
  forEach { result = max(result, block(it)) }
  return result
}

internal inline fun <E> CircularArray<E>.any(block: (E) -> Boolean): Boolean {
  forEach { if (block(it)) return true }
  return false
}

internal fun <E> CircularArray<E>.lastOrNull(): E? = when {
  size() > 0 -> last
  else -> null
}
