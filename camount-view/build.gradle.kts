import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinParcelize)
  alias(libs.plugins.vanniktechMavenPublish)
}

android {
  namespace = "io.denison.camount.view"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  sourceSets["main"].java.srcDirs("src/main/kotlin")
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

dependencies {
  implementation(libs.androidx.annotation)
  implementation(libs.androidx.collection)
  implementation(libs.androidx.core)
}

mavenPublishing {
  publishToMavenCentral()
  if (project.hasProperty("signingInMemoryKey")) {
    signAllPublications()
  }
  configure(
    AndroidSingleVariantLibrary(
      javadocJar = JavadocJar.Empty(),
      sourcesJar = SourcesJar.Sources(),
      variant = "release",
    ),
  )
}
