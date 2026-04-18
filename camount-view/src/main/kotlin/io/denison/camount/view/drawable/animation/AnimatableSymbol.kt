package io.denison.camount.view.drawable.animation

internal interface AnimatableSymbol {

  val isRunning: Boolean

  fun start()
  fun pause()
}
