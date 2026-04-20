import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKmpLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  androidLibrary {
    namespace = "io.denison.camount"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  iosArm64()
  iosSimulatorArm64()

  jvm("desktop") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser()
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
    }
    androidMain.dependencies {
      implementation(libs.androidx.compose.ui.tooling.preview)
      implementation(libs.androidx.compose.ui.tooling)
    }
  }
}
