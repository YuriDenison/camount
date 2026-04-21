# Maven Central Publication — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire up the three Camount library modules (`:camount`, `:camount-view`, `:camount-view-databinding`) for publication to Maven Central (Sonatype Central Portal) at version `0.9.1` under the `io.github.yuridenison` group, including a tag-triggered GitHub Actions workflow.

**Architecture:** Apply the `com.vanniktech.maven.publish` Gradle plugin (v0.36.0) on each library module. Shared POM metadata (group, version, license, developer, SCM) lives in the root `gradle.properties`; per-module identity (artifact id, name, description) lives in a module-level `gradle.properties`. All artifacts are GPG-signed using in-memory keys; credentials come from either `~/.gradle/gradle.properties` (local) or `ORG_GRADLE_PROJECT_*` env vars (CI).

**Tech Stack:** Gradle 8.x (Kotlin DSL), Kotlin 2.3.20, Compose Multiplatform 1.10.3, AGP 9.1.1, `com.vanniktech.maven.publish` 0.36.0, GitHub Actions.

**Verification philosophy:** Each module change is verified by running `./gradlew publishToMavenLocal` for that module and inspecting the output in `~/.m2/repository/io/github/yuridenison/`. This is the highest-fidelity check short of actually uploading to Central — it exercises the exact same artifact generation path, just with `mavenLocal` as the repository.

---

## File Structure

**Files to create:**
- `camount/gradle.properties` — per-module POM identity for core KMP lib
- `camount-view/gradle.properties` — per-module POM identity for Android Views binding
- `camount-view-databinding/gradle.properties` — per-module POM identity for data-binding adapters
- `.github/workflows/publish.yml` — tag-triggered CI publish workflow

**Files to modify:**
- `gradle/libs.versions.toml` — add `vanniktechMavenPublish` plugin alias
- `build.gradle.kts` (root) — declare plugin with `apply false`
- `gradle.properties` (root) — add shared POM metadata
- `camount/build.gradle.kts` — apply plugin, add `mavenPublishing {}` block
- `camount-view/build.gradle.kts` — apply plugin, add `mavenPublishing {}` block
- `camount-view-databinding/build.gradle.kts` — apply plugin, add `mavenPublishing {}` block
- `README.md` — short "Publishing" section covering local credential setup

---

## Task 1: Add plugin to version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1.1: Add plugin version and alias**

Open `gradle/libs.versions.toml`. In the `[versions]` block, append:

```toml
vanniktechMavenPublish = "0.36.0"
```

In the `[plugins]` block, append:

```toml
vanniktechMavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "vanniktechMavenPublish" }
```

After the edit the file should have both new lines alongside the existing `ktlintGradle`, `kotlinAndroid`, etc.

- [ ] **Step 1.2: Verify the catalog parses**

Run:

```bash
./gradlew help --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`. Any TOML or alias typo surfaces here with a `Could not resolve` error.

---

## Task 2: Declare plugin in root build, apply false

**Files:**
- Modify: `build.gradle.kts` (root)

- [ ] **Step 2.1: Add plugin alias to root plugins block**

Open `build.gradle.kts` at the project root. The `plugins {}` block currently ends with `alias(libs.plugins.ktlint) apply false`. Add one more line immediately above the closing brace:

```kotlin
  alias(libs.plugins.vanniktechMavenPublish) apply false
```

Full plugins block after the edit:

```kotlin
plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.androidKmpLibrary) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.kotlinAndroid) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.kotlinParcelize) apply false
  alias(libs.plugins.ktlint) apply false
  alias(libs.plugins.vanniktechMavenPublish) apply false
}
```

- [ ] **Step 2.2: Verify**

Run:

```bash
./gradlew help --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`. A dependency resolution failure here means the plugin coordinate is wrong.

---

## Task 3: Add shared POM metadata to root gradle.properties

**Files:**
- Modify: `gradle.properties` (root)

- [ ] **Step 3.1: Append shared publication block**

Open `gradle.properties` at project root. After the existing `#Android` block (last line `android.newDsl=false`), append:

```properties

#Publication — Maven Central (shared metadata)
GROUP=io.github.yuridenison
VERSION_NAME=0.9.1
POM_URL=https://github.com/yuridenison/camount
POM_SCM_URL=https://github.com/yuridenison/camount
POM_SCM_CONNECTION=scm:git:git://github.com/yuridenison/camount.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://git@github.com/yuridenison/camount.git
POM_LICENSE_NAME=The Apache Software License, Version 2.0
POM_LICENSE_URL=https://www.apache.org/licenses/LICENSE-2.0.txt
POM_LICENSE_DIST=repo
POM_DEVELOPER_ID=yuridenison
POM_DEVELOPER_NAME=Yuri Denison
POM_DEVELOPER_EMAIL=yuri.denison@gmail.com
POM_DEVELOPER_URL=https://github.com/yuridenison
```

- [ ] **Step 3.2: Verify properties load**

Run:

```bash
./gradlew properties --no-configuration-cache | grep -E "^(GROUP|VERSION_NAME|POM_URL):"
```

Expected output (order may vary):

```
GROUP: io.github.yuridenison
POM_URL: https://github.com/yuridenison/camount
VERSION_NAME: 0.9.1
```

---

## Task 4: Publish :camount (Kotlin Multiplatform)

**Files:**
- Create: `camount/gradle.properties`
- Modify: `camount/build.gradle.kts`

- [ ] **Step 4.1: Create `camount/gradle.properties`**

Create the file with this exact content:

```properties
POM_ARTIFACT_ID=camount
POM_NAME=Camount
POM_DESCRIPTION=Animated currency/amount formatter widget for Compose Multiplatform (Android, iOS, Desktop, Web).
```

- [ ] **Step 4.2: Apply plugin to `camount/build.gradle.kts`**

Open `camount/build.gradle.kts`. At the top, add one import line to the existing imports:

```kotlin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
```

In the `plugins {}` block, add one line after `alias(libs.plugins.composeCompiler)`:

```kotlin
  alias(libs.plugins.vanniktechMavenPublish)
```

Full plugins block after edit:

```kotlin
plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKmpLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.vanniktechMavenPublish)
}
```

- [ ] **Step 4.3: Add `mavenPublishing {}` block at the end of the file**

Append to the end of `camount/build.gradle.kts` (after the closing brace of the existing `kotlin { ... }` block):

```kotlin

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
  configure(
    KotlinMultiplatform(
      javadocJar = JavadocJar.Empty(),
      sourcesJar = SourcesJar.Sources(),
    ),
  )
}
```

- [ ] **Step 4.4: Publish to Maven Local**

Run:

```bash
./gradlew :camount:publishToMavenLocal --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`. This will compile all five targets (android, desktop, iosArm64, iosSimulatorArm64, wasmJs) and publish them.

Note on signing: `publishToMavenLocal` does not require signing to succeed even with `signAllPublications()` in the block — the Vanniktech plugin skips signing locally when no key is configured. Signing is enforced when publishing to Central (CI provides the key). If a signing-related error *does* appear locally, check that `signingInMemoryKey` is not partially set (either fully present or fully absent — partial env vars cause the plugin to attempt signing and fail).

- [ ] **Step 4.5: Inspect local artifacts**

Run:

```bash
ls ~/.m2/repository/io/github/yuridenison/camount/0.9.1/
```

Expected output (order varies):

```
camount-0.9.1-javadoc.jar
camount-0.9.1-sources.jar
camount-0.9.1.module
camount-0.9.1.pom
```

Then check the KMP target-specific publications:

```bash
ls -d ~/.m2/repository/io/github/yuridenison/camount-*/0.9.1/
```

Expected (one directory per target):

```
.../camount-android/0.9.1/
.../camount-iosarm64/0.9.1/
.../camount-iossimulatorarm64/0.9.1/
.../camount-jvm/0.9.1/   # "desktop" target publishes as camount-jvm
.../camount-wasm-js/0.9.1/
```

- [ ] **Step 4.6: Inspect the POM**

Run:

```bash
cat ~/.m2/repository/io/github/yuridenison/camount/0.9.1/camount-0.9.1.pom
```

Verify that it contains:
- `<groupId>io.github.yuridenison</groupId>`
- `<artifactId>camount</artifactId>`
- `<version>0.9.1</version>`
- `<name>Camount</name>`
- `<description>Animated currency/amount formatter widget for Compose Multiplatform (Android, iOS, Desktop, Web).</description>`
- `<licenses>` block with Apache-2.0
- `<developers>` block with Yuri Denison
- `<scm>` block pointing to `github.com/yuridenison/camount`

If `signAllPublications()` was temporarily commented out in Step 4.4, restore it now (it is required for Central uploads; CI will supply the key).

---

## Task 5: Publish :camount-view (Android library)

**Files:**
- Create: `camount-view/gradle.properties`
- Modify: `camount-view/build.gradle.kts`

- [ ] **Step 5.1: Create `camount-view/gradle.properties`**

```properties
POM_ARTIFACT_ID=camount-view
POM_NAME=Camount View
POM_DESCRIPTION=Android Views binding for the Camount animated currency/amount widget.
```

- [ ] **Step 5.2: Apply plugin to `camount-view/build.gradle.kts`**

Open `camount-view/build.gradle.kts`. Add imports at the top (after `import org.jetbrains.kotlin.gradle.dsl.JvmTarget`):

```kotlin
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
```

In the `plugins {}` block, add one line after `alias(libs.plugins.kotlinParcelize)`:

```kotlin
  alias(libs.plugins.vanniktechMavenPublish)
```

Full plugins block after edit:

```kotlin
plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinParcelize)
  alias(libs.plugins.vanniktechMavenPublish)
}
```

- [ ] **Step 5.3: Add `mavenPublishing {}` block at end of file**

Append after the existing `dependencies { ... }` block:

```kotlin

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
  configure(
    AndroidSingleVariantLibrary(
      javadocJar = JavadocJar.Empty(),
      sourcesJar = SourcesJar.Sources(),
      variant = "release",
    ),
  )
}
```

- [ ] **Step 5.4: Publish to Maven Local and inspect**

Run:

```bash
./gradlew :camount-view:publishToMavenLocal --no-configuration-cache
ls ~/.m2/repository/io/github/yuridenison/camount-view/0.9.1/
```

Expected:

```
camount-view-0.9.1-javadoc.jar   # empty
camount-view-0.9.1-sources.jar
camount-view-0.9.1.aar
camount-view-0.9.1.module
camount-view-0.9.1.pom
```

(Same signing note as Task 4.4 applies.)

- [ ] **Step 5.5: Verify POM**

```bash
cat ~/.m2/repository/io/github/yuridenison/camount-view/0.9.1/camount-view-0.9.1.pom
```

Confirm `<artifactId>camount-view</artifactId>`, `<name>Camount View</name>`, correct description, license, developer, SCM blocks (identical to :camount other than artifactId/name/description).

---

## Task 6: Publish :camount-view-databinding (Android library)

**Files:**
- Create: `camount-view-databinding/gradle.properties`
- Modify: `camount-view-databinding/build.gradle.kts`

- [ ] **Step 6.1: Create `camount-view-databinding/gradle.properties`**

```properties
POM_ARTIFACT_ID=camount-view-databinding
POM_NAME=Camount View DataBinding
POM_DESCRIPTION=Android data-binding adapters for the Camount animated currency/amount widget.
```

- [ ] **Step 6.2: Apply plugin to `camount-view-databinding/build.gradle.kts`**

Open `camount-view-databinding/build.gradle.kts`. Add imports at the top:

```kotlin
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
```

In the `plugins {}` block, add one line after `alias(libs.plugins.kotlinAndroid)`:

```kotlin
  alias(libs.plugins.vanniktechMavenPublish)
```

Full plugins block after edit:

```kotlin
plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.vanniktechMavenPublish)
}
```

- [ ] **Step 6.3: Add `mavenPublishing {}` block at end of file**

Append after the existing `dependencies { ... }` block:

```kotlin

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
  configure(
    AndroidSingleVariantLibrary(
      javadocJar = JavadocJar.Empty(),
      sourcesJar = SourcesJar.Sources(),
      variant = "release",
    ),
  )
}
```

- [ ] **Step 6.4: Publish to Maven Local and inspect**

Run:

```bash
./gradlew :camount-view-databinding:publishToMavenLocal --no-configuration-cache
ls ~/.m2/repository/io/github/yuridenison/camount-view-databinding/0.9.1/
```

Expected:

```
camount-view-databinding-0.9.1-javadoc.jar
camount-view-databinding-0.9.1-sources.jar
camount-view-databinding-0.9.1.aar
camount-view-databinding-0.9.1.module
camount-view-databinding-0.9.1.pom
```

- [ ] **Step 6.5: Verify POM**

```bash
cat ~/.m2/repository/io/github/yuridenison/camount-view-databinding/0.9.1/camount-view-databinding-0.9.1.pom
```

Confirm `<artifactId>camount-view-databinding</artifactId>`, correct name/description, identical license/developer/SCM blocks, and confirm the POM declares a dependency on `io.github.yuridenison:camount-view:0.9.1` (this comes from the existing `api(project(":camount-view"))` in `dependencies`).

---

## Task 7: Full-project publish verification

- [ ] **Step 7.1: Publish all three modules at once**

Run:

```bash
./gradlew publishToMavenLocal --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`. This invokes `publishToMavenLocal` on every subproject that has the plugin applied.

- [ ] **Step 7.2: Confirm every expected publication exists**

Run:

```bash
find ~/.m2/repository/io/github/yuridenison -name "*-0.9.1.pom" | sort
```

Expected (exactly these lines, order guaranteed by `sort`):

```
~/.m2/repository/io/github/yuridenison/camount-android/0.9.1/camount-android-0.9.1.pom
~/.m2/repository/io/github/yuridenison/camount-iosarm64/0.9.1/camount-iosarm64-0.9.1.pom
~/.m2/repository/io/github/yuridenison/camount-iossimulatorarm64/0.9.1/camount-iossimulatorarm64-0.9.1.pom
~/.m2/repository/io/github/yuridenison/camount-jvm/0.9.1/camount-jvm-0.9.1.pom
~/.m2/repository/io/github/yuridenison/camount-view-databinding/0.9.1/camount-view-databinding-0.9.1.pom
~/.m2/repository/io/github/yuridenison/camount-view/0.9.1/camount-view-0.9.1.pom
~/.m2/repository/io/github/yuridenison/camount-wasm-js/0.9.1/camount-wasm-js-0.9.1.pom
~/.m2/repository/io/github/yuridenison/camount/0.9.1/camount-0.9.1.pom
```

(The paths are expanded from `~` by `find`.)

Any missing line = a target didn't publish; go back to the relevant task.

- [ ] **Step 7.3: Sanity-check the core Kotlin metadata module**

```bash
head -40 ~/.m2/repository/io/github/yuridenison/camount/0.9.1/camount-0.9.1.module
```

Expected: JSON beginning with `"formatVersion": "1.1"` and listing variants for each target (common, jvm, android, iosArm64, iosSimulatorArm64, wasmJs). This is the Gradle Module Metadata file that wires up target-specific resolution for consumers.

---

## Task 8: Author the GitHub Actions publish workflow

**Files:**
- Create: `.github/workflows/publish.yml`

- [ ] **Step 8.1: Check existing workflows for conventions**

Run:

```bash
ls .github/workflows/
cat .github/workflows/deploy-web.yml
```

Note the existing runner, JDK version, and `setup-gradle` action usage so the new file matches style.

- [ ] **Step 8.2: Create `.github/workflows/publish.yml`**

```yaml
name: Publish to Maven Central

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch: {}

jobs:
  publish:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - uses: gradle/actions/setup-gradle@v4

      - name: Publish to Maven Central
        env:
          ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
          ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
          ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.SIGNING_IN_MEMORY_KEY }}
          ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_IN_MEMORY_KEY_PASSWORD }}
        run: ./gradlew publishToMavenCentral --no-configuration-cache
```

Notes for the implementing engineer:

- `macos-latest` is required: `:camount` has iOS Kotlin/Native targets that only compile on macOS.
- `workflow_dispatch` lets you kick off a publish from the Actions UI for debugging without creating a tag.
- `--no-configuration-cache` is required because the `signing` plugin is not configuration-cache compatible and the repo's root `gradle.properties` enables configuration cache for normal dev builds.

- [ ] **Step 8.3: Validate YAML**

Run:

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/publish.yml'))"
```

Expected: exits with no output (valid YAML).

If `python3` / `yaml` is unavailable:

```bash
python3 -c "import sys, re; t=open('.github/workflows/publish.yml').read(); sys.exit(0 if 'publishToMavenCentral' in t and 'secrets.SIGNING_IN_MEMORY_KEY' in t else 1)"
```

This is a minimal sanity check that the file was written with the expected task and secret names. Full YAML validation happens automatically the first time the workflow runs on GitHub.

---

## Task 9: Document local publishing in README

**Files:**
- Modify: `README.md`

- [ ] **Step 9.1: Add a "Publishing" section to README**

Open `README.md`. Find the horizontal rule `---` near the bottom (before the final "Learn more about Kotlin Multiplatform..." paragraph). Insert the following section *before* that rule:

```markdown
## Publishing

The Compose Multiplatform library modules (`:camount`, `:camount-view`, `:camount-view-databinding`) are published to Maven Central under `io.github.yuridenison`.

```kotlin
implementation("io.github.yuridenison:camount:0.9.1")
implementation("io.github.yuridenison:camount-view:0.9.1")
implementation("io.github.yuridenison:camount-view-databinding:0.9.1")
```

### Releasing

CI publishes on tag push (`v*`) — see [`.github/workflows/publish.yml`](./.github/workflows/publish.yml). Required repository secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` (Central Portal user token), `SIGNING_IN_MEMORY_KEY` (ASCII-armored GPG private key), `SIGNING_IN_MEMORY_KEY_PASSWORD`.

For a local publish (e.g. dry-run into `~/.m2`), place the same four values in `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=<central portal token username>
mavenCentralPassword=<central portal token password>
signingInMemoryKey=<ASCII-armored GPG private key; escape newlines as \n>
signingInMemoryKeyPassword=<key passphrase>
```

Then:

```shell
./gradlew publishToMavenLocal --no-configuration-cache
```
```

- [ ] **Step 9.2: Verify ktlint/formatting is happy**

Run:

```bash
./gradlew ktlintCheck --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`. `README.md` is not Kotlin, but this also covers the new Kotlin files we added.

---

## Task 10: End-to-end verification

- [ ] **Step 10.1: Wipe prior local publications**

Run:

```bash
rm -rf ~/.m2/repository/io/github/yuridenison/
```

This ensures the next step's output is a fresh, complete publication set.

- [ ] **Step 10.2: Full publish**

```bash
./gradlew clean publishToMavenLocal --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`. Full KMP compilation plus publication. First run may take several minutes.

- [ ] **Step 10.3: Verify full artifact inventory**

```bash
find ~/.m2/repository/io/github/yuridenison -name "*-0.9.1.pom" | sort | wc -l
```

Expected: `8` (one POM per publication: camount root + 5 KMP target-specific + camount-view + camount-view-databinding).

- [ ] **Step 10.4: Verify signing output is produced when signing keys are present**

If `signingInMemoryKey` is configured locally:

```bash
find ~/.m2/repository/io/github/yuridenison -name "*.asc" | wc -l
```

Expected: a non-zero count matching the number of published artifacts.

If not configured locally, this step is skipped — the CI workflow provides the key on tag push.

- [ ] **Step 10.5: Confirm `ktlintCheck` passes across the project**

```bash
./gradlew ktlintCheck --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10.6: Confirm a normal (non-publish) build still works**

```bash
./gradlew :samples:android:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. This is an existing command from the README; running it ensures we didn't break the regular dev/build path while adding publication wiring.

---

## Post-implementation manual steps (out of scope for this plan, informational only)

These are the follow-up actions the repo owner must perform before the first successful Central release. They are NOT implemented by this plan.

1. **Register the namespace** — log in to [central.sonatype.com](https://central.sonatype.com/), go to "Namespaces", add `io.github.yuridenison` (auto-verified via GitHub).
2. **Generate a Central Portal user token** — "Account → Generate User Token". Save username + password as GitHub Actions secrets `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD`.
3. **Generate/upload GPG key** —
   - `gpg --gen-key` (RSA 4096, real name/email matching developer POM entry).
   - `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`.
   - `gpg --armor --export-secret-keys <KEY_ID>` → save as `SIGNING_IN_MEMORY_KEY` secret.
   - Key passphrase → `SIGNING_IN_MEMORY_KEY_PASSWORD` secret.
4. **Tag and push** — `git tag v0.9.1 && git push origin v0.9.1`. The workflow uploads to a staging deployment.
5. **Release from Portal UI** — log in, find the pending deployment, click "Publish". (Automatic release can be enabled later.)
