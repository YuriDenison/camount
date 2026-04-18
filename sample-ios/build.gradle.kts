plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "SampleShared"
      isStatic = true
    }
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
