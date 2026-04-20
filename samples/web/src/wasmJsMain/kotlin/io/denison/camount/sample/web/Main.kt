package io.denison.camount.sample.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.denison.camount.sample.CamountSampleScreen
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val root = document.getElementById("ComposeTarget") ?: document.body!!
  ComposeViewport(root) {
    CamountSampleScreen()
  }
}
