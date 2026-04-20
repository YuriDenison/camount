import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  jvm {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  sourceSets {
    val jvmMain by getting {
      dependencies {
        implementation(project(":samples:shared-compose"))
        implementation(compose.desktop.currentOs)
      }
    }
  }
}

compose.desktop {
  application {
    mainClass = "io.denison.camount.sample.desktop.MainKt"
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "CamountDesktop"
      packageVersion = "1.0.0"
    }
  }
}
