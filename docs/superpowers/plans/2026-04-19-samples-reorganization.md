# Samples Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the single `sample/` module into `samples/shared-compose` (shared Compose UI, exports iOS framework) + `samples/android` (Android app), wire them into the build, redirect the existing `iosApp/` to the new framework location, and delete the old `sample/` module — while keeping Android and iOS samples runnable end-to-end.

**Architecture:** Stand up the two new modules in parallel with the old one (settings.gradle includes all three during the transition). Migrate consumers (iosApp Xcode project) to point at the new location. Only after Android sample + iosApp both build and run against the new modules do we remove the old `:sample` include and delete the directory.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.3, AGP 9.1.1, Gradle Kotlin DSL. Android app uses `androidApplication` + `kotlinAndroid` plugins (pure Android, not KMP). Shared Compose module uses `kotlinMultiplatform` + `composeMultiplatform` with `androidTarget`, `iosArm64`, `iosSimulatorArm64`.

**Spec:** `docs/superpowers/specs/2026-04-19-ios-native-support-design.md` §3.2.

**Out of scope for this plan:** Creating the new `samples/ios` SwiftUI project, creating the `camount-swift` package, deleting root `iosApp/`. Those land in separate plans.

---

## File Structure

**New (by the end of this plan):**
- `samples/shared-compose/build.gradle.kts` — KMP module, depends on `:camount`, exports `SampleShared` iOS framework, keeps Compose resources.
- `samples/shared-compose/src/commonMain/kotlin/io/denison/camount/sample/CamountSampleScreen.kt` — moved from `sample/src/commonMain/kotlin/...`.
- `samples/shared-compose/src/commonMain/composeResources/font/manrope_medium.ttf` — moved from `sample/src/commonMain/composeResources/...`.
- `samples/shared-compose/src/iosMain/kotlin/io/denison/camount/sample/MainViewController.kt` — moved from `sample/src/iosMain/kotlin/...`.
- `samples/android/build.gradle.kts` — Android application, depends on `:samples:shared-compose` + `:camount-view`.
- `samples/android/src/main/AndroidManifest.xml` — moved from `sample/src/androidMain/AndroidManifest.xml`.
- `samples/android/src/main/kotlin/io/denison/camount/sample/SampleActivity.kt` — moved from `sample/src/androidMain/kotlin/...`.
- `samples/android/src/main/res/layout/view_amount_edit.xml` — moved from `sample/src/androidMain/res/layout/...`.
- `samples/android/src/main/res/layout/view_amount_text.xml` — moved from `sample/src/androidMain/res/layout/...`.
- `samples/android/src/main/res/values/styles.xml` — moved.
- `samples/android/src/main/res/values/themes.xml` — moved.

**Modified:**
- `settings.gradle.kts` — remove `include(":sample")`, add `include(":samples:shared-compose")` and `include(":samples:android")`.
- `iosApp/iosApp.xcodeproj/project.pbxproj` — swap `:sample:embedAndSignAppleFrameworkForXcode` → `:samples:shared-compose:embedAndSignAppleFrameworkForXcode` in the Run Script build phase, and update framework search paths.

**Deleted (at end):**
- The entire `sample/` directory at the repo root.

---

## Prerequisites and Invariants

- Work must be done on a branch, not directly on `main`.
- After every numbered task that ends with "verify", the stated check MUST pass before moving to the next task. If it fails, stop and fix before continuing.
- Do NOT use `git add -A` or `git add .` anywhere in this plan — always stage the specific paths listed in each commit step.
- The old `sample/` directory stays in place until Task 11. During Tasks 1–10, `:sample`, `:samples:shared-compose`, and `:samples:android` coexist in `settings.gradle.kts`. This is deliberate: it lets iosApp and the new Android sample each cut over independently with rollback possible at every step.
- Never force-push or use destructive git commands.

---

## Task 1: Create the `samples/shared-compose` module skeleton

**Files:**
- Create: `samples/shared-compose/build.gradle.kts`

- [ ] **Step 1: Create the directory**

Run from repo root:
```bash
mkdir -p samples/shared-compose/src/commonMain/kotlin/io/denison/camount/sample
mkdir -p samples/shared-compose/src/commonMain/composeResources/font
mkdir -p samples/shared-compose/src/iosMain/kotlin/io/denison/camount/sample
```

- [ ] **Step 2: Write `samples/shared-compose/build.gradle.kts`**

Exact content (mirrors the iOS-framework + Compose-resources parts of the current `sample/build.gradle.kts`, stripped of Android-application bits):

```kotlin
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
```

- [ ] **Step 3: Do NOT register the module in settings yet**

Registering before the source files are copied would fail the build. Registration happens in Task 3.

- [ ] **Step 4: Commit**

```bash
git add samples/shared-compose/build.gradle.kts
git commit -m "Scaffold samples/shared-compose module"
```

---

## Task 2: Copy shared Compose sources into `samples/shared-compose`

**Files:**
- Copy from `sample/src/commonMain/kotlin/io/denison/camount/sample/CamountSampleScreen.kt`
  to `samples/shared-compose/src/commonMain/kotlin/io/denison/camount/sample/CamountSampleScreen.kt`
- Copy from `sample/src/commonMain/composeResources/font/manrope_medium.ttf`
  to `samples/shared-compose/src/commonMain/composeResources/font/manrope_medium.ttf`
- Copy from `sample/src/iosMain/kotlin/io/denison/camount/sample/MainViewController.kt`
  to `samples/shared-compose/src/iosMain/kotlin/io/denison/camount/sample/MainViewController.kt`

This is a **copy**, not a move. The originals stay under `sample/` until Task 11 so the old `:sample` module keeps building during the transition.

- [ ] **Step 1: Copy `CamountSampleScreen.kt`**

Run from repo root:
```bash
cp sample/src/commonMain/kotlin/io/denison/camount/sample/CamountSampleScreen.kt \
   samples/shared-compose/src/commonMain/kotlin/io/denison/camount/sample/CamountSampleScreen.kt
```

No content changes required — the package declaration and imports remain valid in the new module.

- [ ] **Step 2: Copy the font resource**

```bash
cp sample/src/commonMain/composeResources/font/manrope_medium.ttf \
   samples/shared-compose/src/commonMain/composeResources/font/manrope_medium.ttf
```

- [ ] **Step 3: Copy `MainViewController.kt`**

```bash
cp sample/src/iosMain/kotlin/io/denison/camount/sample/MainViewController.kt \
   samples/shared-compose/src/iosMain/kotlin/io/denison/camount/sample/MainViewController.kt
```

- [ ] **Step 4: Verify files exist and are non-empty**

```bash
wc -l samples/shared-compose/src/commonMain/kotlin/io/denison/camount/sample/CamountSampleScreen.kt \
      samples/shared-compose/src/iosMain/kotlin/io/denison/camount/sample/MainViewController.kt
ls -l samples/shared-compose/src/commonMain/composeResources/font/manrope_medium.ttf
```

Expected: `CamountSampleScreen.kt` ≥ 500 lines, `MainViewController.kt` ≥ 5 lines, font file size > 0.

- [ ] **Step 5: Commit**

```bash
git add samples/shared-compose/src
git commit -m "Copy shared Compose sample sources to samples/shared-compose"
```

---

## Task 3: Register `:samples:shared-compose` and verify it builds

**Files:**
- Modify: `settings.gradle.kts` (add include; do NOT remove `:sample` yet)

- [ ] **Step 1: Add the include**

Edit `settings.gradle.kts`. Find the block of `include(...)` lines at the bottom. Add **after** the existing lines (keep `:sample` in place):

```kotlin
include(":camount")
include(":camount-view")
include(":camount-view-databinding")
include(":sample")
include(":samples:shared-compose")
```

- [ ] **Step 2: Sync and assemble the new module**

Run from repo root:
```bash
./gradlew :samples:shared-compose:assemble
```

Expected: `BUILD SUCCESSFUL`. The module compiles commonMain + androidMain + iosArm64 + iosSimulatorArm64.

If the build fails, the most likely cause is a typo in the Gradle file, a missing source directory, or the wrong `packageOfResClass`. Fix before moving on.

- [ ] **Step 3: Commit**

```bash
git add settings.gradle.kts
git commit -m "Register samples/shared-compose in settings.gradle.kts"
```

---

## Task 4: Create the `samples/android` module skeleton

**Files:**
- Create: `samples/android/build.gradle.kts`
- Create: `samples/android/src/main/AndroidManifest.xml` (empty shell; populated in Task 5)

- [ ] **Step 1: Create directories**

```bash
mkdir -p samples/android/src/main/kotlin/io/denison/camount/sample
mkdir -p samples/android/src/main/res/layout
mkdir -p samples/android/src/main/res/values
```

- [ ] **Step 2: Write `samples/android/build.gradle.kts`**

Exact content (pure Android app, not KMP — depends on `:samples:shared-compose` and `:camount-view`):

```kotlin
plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.composeCompiler)
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
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(project(":camount"))
  implementation(project(":camount-view"))
  implementation(project(":samples:shared-compose"))
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 3: Commit**

```bash
git add samples/android/build.gradle.kts
git commit -m "Scaffold samples/android module"
```

---

## Task 5: Copy Android app sources into `samples/android`

**Files:**
- Copy `sample/src/androidMain/AndroidManifest.xml` → `samples/android/src/main/AndroidManifest.xml`
- Copy `sample/src/androidMain/kotlin/io/denison/camount/sample/SampleActivity.kt` → `samples/android/src/main/kotlin/io/denison/camount/sample/SampleActivity.kt`
- Copy `sample/src/androidMain/res/layout/view_amount_edit.xml` → `samples/android/src/main/res/layout/view_amount_edit.xml`
- Copy `sample/src/androidMain/res/layout/view_amount_text.xml` → `samples/android/src/main/res/layout/view_amount_text.xml`
- Copy `sample/src/androidMain/res/values/styles.xml` → `samples/android/src/main/res/values/styles.xml`
- Copy `sample/src/androidMain/res/values/themes.xml` → `samples/android/src/main/res/values/themes.xml`

- [ ] **Step 1: Copy the files**

```bash
cp sample/src/androidMain/AndroidManifest.xml samples/android/src/main/AndroidManifest.xml
cp sample/src/androidMain/kotlin/io/denison/camount/sample/SampleActivity.kt \
   samples/android/src/main/kotlin/io/denison/camount/sample/SampleActivity.kt
cp sample/src/androidMain/res/layout/view_amount_edit.xml samples/android/src/main/res/layout/
cp sample/src/androidMain/res/layout/view_amount_text.xml samples/android/src/main/res/layout/
cp sample/src/androidMain/res/values/styles.xml samples/android/src/main/res/values/
cp sample/src/androidMain/res/values/themes.xml samples/android/src/main/res/values/
```

No content edits required — package `io.denison.camount.sample` and imports (including `import io.denison.camount.sample.CamountSampleScreen` from `:samples:shared-compose`) are still correct.

- [ ] **Step 2: Verify files landed**

```bash
ls samples/android/src/main/
ls samples/android/src/main/kotlin/io/denison/camount/sample/
ls samples/android/src/main/res/layout/
ls samples/android/src/main/res/values/
```

Expected: `AndroidManifest.xml` present at top; `SampleActivity.kt` under kotlin path; both XML layouts and both values files present.

- [ ] **Step 3: Commit**

```bash
git add samples/android/src
git commit -m "Copy Android sample sources to samples/android"
```

---

## Task 6: Register `:samples:android` and verify it builds and runs

**Files:**
- Modify: `settings.gradle.kts` (add include)

- [ ] **Step 1: Add the include**

Edit `settings.gradle.kts`. Add below the `:samples:shared-compose` line:

```kotlin
include(":camount")
include(":camount-view")
include(":camount-view-databinding")
include(":sample")
include(":samples:shared-compose")
include(":samples:android")
```

- [ ] **Step 2: Assemble the debug APK**

Run from repo root:
```bash
./gradlew :samples:android:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, APK produced at `samples/android/build/outputs/apk/debug/android-debug.apk`.

If the build fails on a missing symbol from shared-compose, confirm Task 3 succeeded (the shared module produces an Android Library AAR that `:samples:android` consumes).

- [ ] **Step 3: Install and launch on a connected device or emulator**

```bash
./gradlew :samples:android:installDebug
adb shell am start -n io.denison.camount.sample/.SampleActivity
```

Manual verification: the sample screen renders, the CMP and classic-View amount widgets both animate on value changes, currency picker works, Shuffle/+1/Reset buttons work.

If no device is available, skip the adb step but confirm `installDebug` succeeds against an emulator or document that this sub-step was deferred — do NOT mark the plan complete.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts
git commit -m "Register samples/android in settings.gradle.kts"
```

---

## Task 7: Point `iosApp` at the new shared-compose framework

**Files:**
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj`

The Xcode project has a Run Script build phase that invokes `./gradlew :sample:embedAndSignAppleFrameworkForXcode`. That must be changed to invoke `:samples:shared-compose:embedAndSignAppleFrameworkForXcode`. Framework search paths and the linked framework name (`SampleShared`) do NOT change, because Task 1's `build.gradle.kts` keeps the same `baseName = "SampleShared"`.

- [ ] **Step 1: Locate the Run Script build phase**

Run:
```bash
grep -n 'sample:embedAndSignAppleFrameworkForXcode' iosApp/iosApp.xcodeproj/project.pbxproj
```

Expected: one or more matches in a shellScript entry.

- [ ] **Step 2: Replace the module path**

Edit `iosApp/iosApp.xcodeproj/project.pbxproj`. For every occurrence of the string `:sample:embedAndSignAppleFrameworkForXcode`, replace with `:samples:shared-compose:embedAndSignAppleFrameworkForXcode`.

Also check for a plain reference to `:sample:` in shell scripts and framework search paths. If any other path-qualified `:sample:` appears (e.g. in a `FRAMEWORK_SEARCH_PATHS` or a custom shell command), replace with `:samples:shared-compose:` analogously.

- [ ] **Step 3: Verify the substitution**

```bash
grep -n ':sample:' iosApp/iosApp.xcodeproj/project.pbxproj
```

Expected: no output (no remaining references to the old module path in Xcode).

```bash
grep -n ':samples:shared-compose:' iosApp/iosApp.xcodeproj/project.pbxproj
```

Expected: at least one match confirming the replacement took effect.

- [ ] **Step 4: Produce the framework under the new path**

```bash
./gradlew :samples:shared-compose:embedAndSignAppleFrameworkForXcode \
  -PXCODE_CONFIGURATION=Debug \
  -PXCODE_PLATFORM_NAME=iphonesimulator \
  -PXCODE_ARCHS=arm64
```

Expected: `BUILD SUCCESSFUL` and `SampleShared.framework` appearing under `samples/shared-compose/build/xcode-frameworks/Debug/iphonesimulator/`.

If this task invocation fails because the Xcode env variables aren't set the same way Xcode sets them, it's acceptable to defer the real verification to Step 5 below (building from Xcode).

- [ ] **Step 5: Build `iosApp` from Xcode or via `xcodebuild`**

Either:
- Open `iosApp/iosApp.xcodeproj` in Xcode, select a simulator target, press ⌘B. Expected: clean build.
- Or from the terminal (requires Xcode tools):

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build
```

Expected: `BUILD SUCCEEDED`.

- [ ] **Step 6: Commit**

```bash
git add iosApp/iosApp.xcodeproj/project.pbxproj
git commit -m "Point iosApp at :samples:shared-compose framework"
```

---

## Task 8: Remove `:sample` from `settings.gradle.kts`

**Files:**
- Modify: `settings.gradle.kts` (remove include)

- [ ] **Step 1: Remove the include line**

Edit `settings.gradle.kts`. Remove the line `include(":sample")`. The remaining includes should be:

```kotlin
include(":camount")
include(":camount-view")
include(":camount-view-databinding")
include(":samples:shared-compose")
include(":samples:android")
```

- [ ] **Step 2: Verify the project still configures**

```bash
./gradlew projects
```

Expected: `BUILD SUCCESSFUL`. The `projects` task lists exactly the five modules above, with no `:sample`.

- [ ] **Step 3: Re-verify everything still builds**

```bash
./gradlew :samples:shared-compose:assemble :samples:android:assembleDebug
```

Expected: both succeed.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts
git commit -m "Unregister :sample in favor of :samples:* modules"
```

---

## Task 9: Delete the old `sample/` directory

**Files:**
- Delete: entire `sample/` directory

- [ ] **Step 1: Remove the directory**

```bash
git rm -r sample
```

- [ ] **Step 2: Confirm it's gone**

```bash
ls sample 2>&1
```

Expected: `ls: sample: No such file or directory`.

- [ ] **Step 3: Verify Android sample still builds and runs**

```bash
./gradlew :samples:android:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

Reinstall on device/emulator to confirm runtime behavior:

```bash
./gradlew :samples:android:installDebug
adb shell am start -n io.denison.camount.sample/.SampleActivity
```

Expected: same sample UI as before the reorg; currency picker, shuffle, +1, reset, and the Compose/View amount widgets all work.

- [ ] **Step 4: Verify iosApp still builds**

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build
```

Expected: `BUILD SUCCEEDED`.

(If an iOS simulator device is available, also run the app and confirm the Compose screen renders.)

- [ ] **Step 5: Commit**

```bash
git commit -m "Remove legacy sample/ module

All sources moved to samples/shared-compose (common + iosMain) and
samples/android (androidMain)."
```

---

## Task 10: Final verification

- [ ] **Step 1: Full clean build**

```bash
./gradlew clean
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. No warnings newly introduced by this plan (pre-existing warnings are fine).

- [ ] **Step 2: Walk through the tree**

```bash
ls samples/
ls samples/shared-compose/src
ls samples/android/src/main
find sample -type f 2>&1
```

Expected:
- `samples/` contains `shared-compose` and `android`.
- `samples/shared-compose/src` contains `commonMain` and `iosMain`.
- `samples/android/src/main` contains `AndroidManifest.xml`, `kotlin/`, `res/`.
- `find sample` errors out (directory gone).

- [ ] **Step 3: Smoke-test both samples by hand one last time**

- Launch Android sample: confirm Shuffle, +1, Reset, currency picker, animated amount widgets.
- Launch iosApp in Xcode: confirm the Compose screen renders and is interactive.

If either fails, do NOT mark the plan complete. Investigate, fix, and commit the fix before closing out.

- [ ] **Step 4: Confirm a clean `git status`**

```bash
git status
```

Expected: `nothing to commit, working tree clean`. If there are stray files (e.g. accidental `build/` outputs), confirm they're in `.gitignore`; do not commit build artifacts.

---

## Self-review checklist (executor)

Before declaring the plan complete, tick off all of the following:

- [ ] `:sample` no longer appears in `settings.gradle.kts`.
- [ ] `sample/` directory does not exist on disk.
- [ ] `:samples:shared-compose` and `:samples:android` are registered.
- [ ] `./gradlew :samples:android:assembleDebug` is green.
- [ ] `./gradlew :samples:shared-compose:assemble` is green.
- [ ] iosApp builds and runs against `:samples:shared-compose`'s framework.
- [ ] The Android sample app behaves identically to before the reorg (spot-check: animated widgets, currency picker, buttons).
- [ ] No `:sample:` string remains in `iosApp/iosApp.xcodeproj/project.pbxproj`.
- [ ] All commits are staged deliberately (no `git add -A`).
