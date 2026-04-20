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
    namespace = "io.denison.camount.sample.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "SampleShared"
      isStatic = true
    }
  }

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
      implementation(project(":camount"))
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
      implementation(compose.material3)
      implementation(compose.components.resources)
    }
  }
}

compose.resources {
  publicResClass = true
  packageOfResClass = "io.denison.camount.sample.resources"
  generateResClass = always
}
