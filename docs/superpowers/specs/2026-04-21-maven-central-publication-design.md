# Maven Central Publication — Design

**Date:** 2026-04-21
**Status:** Draft
**Modules in scope:** `:camount`, `:camount-view`, `:camount-view-databinding`
**Target version:** `0.9.1`

## Goal

Publish the three Camount library modules to Maven Central (via the Sonatype Central
Portal) so downstream projects can consume them with standard Gradle/Maven dependency
declarations, e.g.:

```kotlin
implementation("io.github.yuridenison:camount:0.9.1")
implementation("io.github.yuridenison:camount-view:0.9.1")
implementation("io.github.yuridenison:camount-view-databinding:0.9.1")
```

The `:camount` module is Kotlin Multiplatform and must ship artifacts for all its
targets: `androidTarget`, `iosArm64`, `iosSimulatorArm64`, `desktop` (JVM), `wasmJs`.
The two `-view` modules are Android-only and ship a single `release` variant each.

Samples are **not** published. `camount-swift` is **not** in scope (distributed via
Swift Package Manager).

## Non-Goals

- Generating Dokka HTML/Javadoc. Empty placeholder javadoc jars are used to satisfy
  Central's requirement.
- Automatic release from staging to Central. First release is manual from the Portal
  UI; auto-release can be enabled later once the pipeline is proven.
- Publishing SNAPSHOT artifacts to Central's snapshot repository. Not required at
  this stage.
- Any change to `camount-swift` or samples.

## Coordinates

| Artifact                                | groupId                  | artifactId                  | version |
|-----------------------------------------|--------------------------|-----------------------------|---------|
| Core KMP library                        | `io.github.yuridenison`  | `camount`                   | `0.9.1` |
| Android Views binding                   | `io.github.yuridenison`  | `camount-view`              | `0.9.1` |
| Android data-binding adapters           | `io.github.yuridenison`  | `camount-view-databinding`  | `0.9.1` |

The `io.github.yuridenison` namespace is auto-verified by the Central Portal from
the GitHub account `yuridenison`, so no DNS TXT proof is required.

POM metadata (shared across all three modules):

- **Name:** per-module (see per-module `gradle.properties` below)
- **Description:** per-module
- **URL:** `https://github.com/yuridenison/camount`
- **License:** Apache-2.0
- **Developer:** Yuri Denison, `yuri.denison@gmail.com`, `https://github.com/yuridenison`
- **SCM:** `https://github.com/yuridenison/camount`

## Approach

Apply the [`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/)
Gradle plugin (version `0.36.0`) to each of the three library modules. The plugin:

- Auto-detects Kotlin Multiplatform vs. Android Library project layout and produces
  the correct publication set.
- Generates sources jars and (via `JavadocJar.Empty()`) empty placeholder javadoc jars.
- Signs all publications with GPG using the `signing` plugin's in-memory key support.
- Uploads to the Central Portal via its native HTTPS API (no `nexus-publish` needed).
- Reads POM metadata from `gradle.properties` files via well-known keys (`GROUP`,
  `VERSION_NAME`, `POM_*`), which lets us keep shared values at the root and
  per-module values in each submodule.

### Why this plugin

Rolling `maven-publish` + `signing` by hand for a KMP project with 5 targets and
two Android-only siblings is ~200 lines of boilerplate per module. Vanniktech's
plugin reduces that to ~5 lines per module plus a few property files. It is the
de-facto standard for KMP/Compose Multiplatform libraries (used by Ktor, Coil,
Voyager, etc.).

## File Changes

### 1. `gradle/libs.versions.toml`

Add the plugin coordinate:

```toml
[versions]
# ... existing ...
vanniktechMavenPublish = "0.36.0"

[plugins]
# ... existing ...
vanniktechMavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "vanniktechMavenPublish" }
```

### 2. Root `build.gradle.kts`

Add the plugin to the root `plugins {}` block with `apply false` so submodules can
opt in individually:

```kotlin
plugins {
  // ... existing ...
  alias(libs.plugins.vanniktechMavenPublish) apply false
}
```

### 3. Root `gradle.properties`

Append shared publication properties. All values here are safe to commit.

```properties
# Maven Central publication — shared metadata
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

### 4. Per-module `gradle.properties`

Create a new file in each library module containing only the artifact identity.

**`camount/gradle.properties`**

```properties
POM_ARTIFACT_ID=camount
POM_NAME=Camount
POM_DESCRIPTION=Animated currency/amount formatter widget for Compose Multiplatform (Android, iOS, Desktop, Web).
```

**`camount-view/gradle.properties`**

```properties
POM_ARTIFACT_ID=camount-view
POM_NAME=Camount View
POM_DESCRIPTION=Android Views binding for the Camount animated currency/amount widget.
```

**`camount-view-databinding/gradle.properties`**

```properties
POM_ARTIFACT_ID=camount-view-databinding
POM_NAME=Camount View DataBinding
POM_DESCRIPTION=Android data-binding adapters for the Camount animated currency/amount widget.
```

### 5. `camount/build.gradle.kts` (KMP)

Add the plugin and a `mavenPublishing` block:

```kotlin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar

plugins {
  // ... existing ...
  alias(libs.plugins.vanniktechMavenPublish)
}

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

Notes:

- As of plugin 0.33.0, the no-arg `publishToMavenCentral()` publishes through the
  Central Portal by default (the `SonatypeHost` enum is deprecated since the
  OSSRH shutdown on 2025-06-30).
- The new `com.android.kotlin.multiplatform.library` plugin (used by `:camount`
  via `androidLibrary { ... }`) already produces only a single release variant,
  so the default `androidVariantsToPublish = listOf("release")` is correct.

### 6. `camount-view/build.gradle.kts` (Android library)

```kotlin
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
  // ... existing ...
  alias(libs.plugins.vanniktechMavenPublish)
}

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

`JavadocJar.Empty()` produces an empty placeholder javadoc jar — which is what
Central requires when no Dokka plugin is applied.

### 7. `camount-view-databinding/build.gradle.kts` (Android library)

Identical structure to `camount-view` above.

### 8. `.github/workflows/publish.yml`

New workflow triggered on tag push matching `v*`:

```yaml
name: Publish to Maven Central

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: macos-latest  # needed for iOS KMP targets
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - uses: gradle/actions/setup-gradle@v4
      - name: Publish
        env:
          ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
          ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
          ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.SIGNING_IN_MEMORY_KEY }}
          ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_IN_MEMORY_KEY_PASSWORD }}
        run: ./gradlew publishToMavenCentral --no-configuration-cache
```

Notes:

- `macos-latest` is required because `:camount` has `iosArm64` and
  `iosSimulatorArm64` targets; the Kotlin/Native iOS compilers only run on macOS.
- `--no-configuration-cache` is required: the `signing` plugin is not
  configuration-cache compatible, and the root `gradle.properties` enables
  configuration cache for normal dev builds. This flag applies only to the publish
  command so everyday builds still benefit from the cache.
- The `ORG_GRADLE_PROJECT_*` env var prefix is how Gradle maps env vars onto
  project properties, which both `vanniktech-maven-publish` and the `signing`
  plugin read.

### 9. Local credential convention (documented, not committed)

For local publishing the same four properties can live in `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=<central portal token username>
mavenCentralPassword=<central portal token password>
signingInMemoryKey=<ascii-armored GPG private key, newlines escaped as \n>
signingInMemoryKeyPassword=<key passphrase>
```

This is documented in the new "Publishing" section of `README.md` added as part of
implementation (one short paragraph + code block).

## Credential / Secret Handling

- `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` — generated from the Central
  Portal user profile (token username + token password). Stored as GitHub Actions
  repository secrets.
- `SIGNING_IN_MEMORY_KEY` — ASCII-armored GPG private key (`gpg --armor --export-secret-keys <KEY_ID>`).
  Stored as a GitHub Actions secret.
- `SIGNING_IN_MEMORY_KEY_PASSWORD` — passphrase for the above key. Stored as a secret.

The GPG public key must be uploaded to a public keyserver (e.g.
`keyserver.ubuntu.com`) before the first publish so Central can verify signatures.

## Verification

The implementation is considered complete when **all** of the following hold:

1. `./gradlew publishToMavenLocal` succeeds with no signing/credential errors
   when signing secrets are configured locally.
2. Inspecting `~/.m2/repository/io/github/yuridenison/` shows:
   - `camount/0.9.1/` containing `camount-0.9.1.module`, `camount-0.9.1.pom`, and
     a javadoc+sources jar.
   - `camount-jvm/`, `camount-android/`, `camount-iosarm64/`, `camount-iossimulatorarm64/`,
     `camount-wasm-js/` each with their platform artifacts and sources jars.
   - `camount-view/0.9.1/` with AAR, sources jar, empty javadoc jar, POM.
   - `camount-view-databinding/0.9.1/` with AAR, sources jar, empty javadoc jar, POM.
   - All artifacts accompanied by `.asc` signatures when signing is configured.
3. The generated POMs contain the license block, developer block, SCM block, and
   a non-empty `<description>`.
4. The `publish.yml` workflow file passes GitHub Actions' YAML lint (workflow is
   not run until the first tag push).

End-to-end Central publication (namespace registration, GPG key upload, tag push,
manual release from Portal) is a follow-up manual step and is out of scope for the
automated work.

## Risks and Open Questions

- **Configuration cache** — Gradle 8+ + Kotlin 2.3 + AGP 9 may or may not work
  cleanly with `--no-configuration-cache` on the publish path. If surprises
  surface, a module-level `org.gradle.configuration-cache=false` override in a
  `~/.gradle.properties`-like fashion is the escape hatch; we will stay with the
  per-invocation flag until a problem appears.
- **iOS KMP artifacts signing on macos-latest** — the iOS tasks require a
  functioning Xcode on the runner. GitHub's `macos-latest` image ships with
  Xcode by default, so this should be a non-issue, but first CI run validates it.
- **Empty javadoc jar on Android modules** — Central accepts empty javadoc jars;
  Vanniktech's `AndroidSingleVariantLibrary` produces one automatically. If
  Central tightens its rules later, the fix is to switch to `JavadocJar.Dokka`.
- **`ktlint` on new `.kts` blocks** — the root build applies ktlint to
  subprojects. The new `mavenPublishing { ... }` blocks must conform; this is
  already covered by the plugin's own formatting conventions.
