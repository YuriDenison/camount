# Camount

An animated currency/amount formatter widget with per-character stack animations, field-aware styling, and a real input pipeline.

**▶ [Live demo (Compose for Web)](https://yuridenison.github.io/camount/)** — the same widget, compiled to Wasm.

The library ships in three flavors that share the same behavior and visual language:

- **Kotlin Multiplatform / Jetpack Compose** — Android, iOS, Desktop (JVM), and Web (Wasm/JS) via `:camount`, plus `:camount-view` / `:camount-view-databinding` for Android Views.
- **Swift Package (iOS 16+)** — a native iOS port at [`/camount-swift`](./camount-swift) with SwiftUI (`AmountText`, `AmountField`) backed by a Core Animation rendering pipeline. Matches the Compose renderer: per-glyph stack animations, cursor, gradient fills, field-colored placeholder/fraction, Material FastOutSlowIn easing.
- **TypeScript / Web Components + React** — a framework-agnostic port at [`/camount-js`](./camount-js) shipping `<camount-text>` / `<camount-field>` custom elements and a thin React wrapper. Renders with DOM + CSS transforms; no canvas, no heavy runtime.

## Repository Layout

- [`/camount`](./camount) — core Compose Multiplatform library (common, Android, iOS, JVM, Web).
- [`/camount-view`](./camount-view) — Android Views binding.
- [`/camount-view-databinding`](./camount-view-databinding) — Android data-binding adapters.
- [`/camount-swift`](./camount-swift) — Swift Package (native iOS).
- [`/camount-js`](./camount-js) — TypeScript package (`@yuridenison/camount`): Web Components + React wrapper.
- [`/samples/shared-compose`](./samples/shared-compose) — cross-platform Compose demo screen reused by Android/iOS/Desktop/Web samples.
- [`/samples/android`](./samples/android) — Android sample app.
- [`/samples/desktop`](./samples/desktop) — Desktop (JVM) sample app.
- [`/samples/web`](./samples/web) — Web sample (Wasm/JS) — served as the live demo above. Hosts two tabs: `Native` (React + `@yuridenison/camount`) and `CMP` (Compose for Web).
- [`/samples/web-react`](./samples/web-react) — React + Vite demo app bundled into `/samples/web` as the `Native` tab.
- [`/samples/ios`](./samples/ios) — iOS sample app (shows both the Compose demo and the native SwiftUI demo side by side).

## Build and Run — Compose / Kotlin targets

### Android

```shell
./gradlew :samples:android:assembleDebug
```

### Desktop (JVM)

```shell
./gradlew :samples:desktop:run
```

To produce a native installer (dmg/msi/deb):

```shell
./gradlew :samples:desktop:packageDistributionForCurrentOS
```

### Web (Wasm)

Live demo: https://yuridenison.github.io/camount/ (auto-deployed from `main` via [`.github/workflows/deploy-web.yml`](./.github/workflows/deploy-web.yml)).

The web sample hosts two tabs sharing one page:
- **Native** — React + `<camount-text>`/`<camount-field>` from `@yuridenison/camount`, built from [`/samples/web-react`](./samples/web-react).
- **CMP** — the shared Compose screen compiled to Wasm.

Run locally (builds `camount-js`, installs the React sample, and serves both bundles):

```shell
./gradlew :samples:web:wasmJsBrowserDevelopmentRun
```

Produce a static distribution under `samples/web/build/dist/wasmJs/productionExecutable`:

```shell
./gradlew :samples:web:wasmJsBrowserDistribution
```

### iOS (Compose demo)

Open [`/samples/ios`](./samples/ios) in Xcode and run the app — the `CmpDemo` tab hosts the Compose-shared screen.

## Build and Run — Native iOS (camount-swift)

`camount-swift` is a Swift Package that can be consumed directly or through the sample app.

- Open [`/samples/ios`](./samples/ios) in Xcode and run — the `NativeDemo` tab hosts `AmountText` / `AmountField` rendered entirely by the Swift pipeline.

### Swift Package Manager

Add Camount as a dependency in your `Package.swift`:

```swift
.package(url: "https://github.com/yuridenison/camount.git", from: "0.9.1")
```

Then list it in your target's dependencies:

```swift
.product(name: "Camount", package: "camount")
```

Or via Xcode: **File → Add Package Dependencies → `https://github.com/yuridenison/camount`**, pinned to exact version `0.9.1`.

Swift and Compose Multiplatform releases share the same tag (`v0.9.1`). Minimum platform: iOS 16; `swift-tools-version:5.9`.

Package tests (from repo root):

```shell
swift test
```

## Build and Run — Web (camount-js)

`camount-js` is a TypeScript package published to npm as [`@yuridenison/camount`](https://www.npmjs.com/package/@yuridenison/camount).

```shell
cd camount-js
npm install
npm run typecheck
npm test
npm run build
```

Install in your app:

```shell
npm install @yuridenison/camount
```

Use the Web Components (`<camount-text>`, `<camount-field>`) in any framework, or the React wrapper via `@yuridenison/camount/react`. See [`camount-js/README.md`](./camount-js/README.md) for details.

## API parity

See [`camount-swift/PARITY.md`](./camount-swift/PARITY.md) for the public-API mapping between the Compose widgets and their Swift counterparts.

## Publishing

The Compose Multiplatform library modules (`:camount`, `:camount-view`, `:camount-view-databinding`) are published to Maven Central under `io.github.yuridenison`.

```kotlin
implementation("io.github.yuridenison:camount:0.9.1")
implementation("io.github.yuridenison:camount-view:0.9.1")
implementation("io.github.yuridenison:camount-view-databinding:0.9.1")
```

The TypeScript package is published to npm as [`@yuridenison/camount`](https://www.npmjs.com/package/@yuridenison/camount):

```shell
npm install @yuridenison/camount
```

All three flavors share the same release tag (`v0.9.1`).

### Releasing

CI publishes on tag push (`v*`):
- Maven Central — see [`.github/workflows/publish.yml`](./.github/workflows/publish.yml). Required secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` (Central Portal user token), `SIGNING_IN_MEMORY_KEY` (ASCII-armored GPG private key), `SIGNING_IN_MEMORY_KEY_PASSWORD`.
- npm — see [`.github/workflows/publish-js.yml`](./.github/workflows/publish-js.yml). Publishes `@yuridenison/camount` with provenance; required secret: `NPM_TOKEN`.

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

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform).
