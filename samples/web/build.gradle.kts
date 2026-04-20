import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    outputModuleName.set("camount-web")
    browser {
      commonWebpackConfig {
        outputFileName = "camount-web.js"
      }
    }
    binaries.executable()
  }

  sourceSets {
    val wasmJsMain by getting {
      dependencies {
        implementation(project(":samples:shared-compose"))
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.components.resources)
      }
    }
  }
}
