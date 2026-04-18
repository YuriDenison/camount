import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  androidTarget {
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

  sourceSets {
    commonMain.dependencies {
      implementation(project(":camount"))
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
      implementation(compose.material3)
      implementation(compose.components.resources)
    }
    androidMain.dependencies {
      implementation(project(":camount-view"))
      implementation(libs.androidx.appcompat)
      implementation(libs.androidx.activity.compose)
    }
  }
}

android {
  namespace = "io.denison.camount.sample"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "io.denison.camount.sample"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  sourceSets["main"].res.srcDirs("src/androidMain/res")
}

compose.resources {
  publicResClass = true
  packageOfResClass = "io.denison.camount.sample.resources"
  generateResClass = always
}
