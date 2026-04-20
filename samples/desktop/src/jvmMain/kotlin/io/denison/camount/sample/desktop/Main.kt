package io.denison.camount.sample.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.denison.camount.sample.CamountSampleScreen

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Camount",
    state = rememberWindowState(size = DpSize(480.dp, 820.dp)),
  ) {
    CamountSampleScreen()
  }
}
