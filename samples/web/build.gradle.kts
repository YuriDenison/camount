import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

val camountJsDir = rootProject.layout.projectDirectory.dir("camount-js")
val webReactDir = rootProject.layout.projectDirectory.dir("samples/web-react")
val nativeResourcesDir = layout.projectDirectory.dir("src/wasmJsMain/resources/native")

val buildCamountJs by tasks.registering(Exec::class) {
  group = "build"
  description = "Builds @yuridenison/camount (dist/) used by the React sample."
  workingDir = camountJsDir.asFile
  commandLine("npm", "run", "build")
  inputs.dir(camountJsDir.dir("src"))
  inputs.file(camountJsDir.file("package.json"))
  inputs.file(camountJsDir.file("tsup.config.ts"))
  inputs.file(camountJsDir.file("tsconfig.json"))
  outputs.dir(camountJsDir.dir("dist"))
}

val installWebReactDeps by tasks.registering(Exec::class) {
  group = "build"
  description = "Installs dependencies for the React sample."
  workingDir = webReactDir.asFile
  commandLine("npm", "install", "--no-audit", "--no-fund")
  inputs.file(webReactDir.file("package.json"))
  outputs.dir(webReactDir.dir("node_modules"))
  dependsOn(buildCamountJs)
}

val buildWebReactSample by tasks.registering(Exec::class) {
  group = "build"
  description = "Builds the React sample into web resources."
  workingDir = webReactDir.asFile
  commandLine("npm", "run", "build")
  inputs.dir(webReactDir.dir("src"))
  inputs.file(webReactDir.file("package.json"))
  inputs.file(webReactDir.file("vite.config.ts"))
  inputs.file(webReactDir.file("tsconfig.json"))
  inputs.file(webReactDir.file("index.html"))
  inputs.dir(camountJsDir.dir("dist"))
  outputs.dir(nativeResourcesDir)
  dependsOn(installWebReactDeps)
}

tasks.matching { task ->
  task.name.startsWith("wasmJsBrowser") ||
    task.name == "assembleWasmJsMainResources" ||
    task.name == "wasmJsJar"
}.configureEach {
  dependsOn(buildWebReactSample)
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
