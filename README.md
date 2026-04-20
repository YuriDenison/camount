# Camount

An animated currency/amount formatter widget with per-character stack animations, field-aware styling, and a real input pipeline.

**▶ [Live demo (Compose for Web)](https://yuridenison.github.io/camount/)** — the same widget, compiled to Wasm.

The library ships in two flavors that share the same behavior and visual language:

- **Kotlin Multiplatform / Jetpack Compose** — Android, iOS, Desktop (JVM), and Web (Wasm/JS) via `:camount`, plus `:camount-view` / `:camount-view-databinding` for Android Views.
- **Swift Package (iOS 16+)** — a native iOS port at [`/camount-swift`](./camount-swift) with SwiftUI (`AmountText`, `AmountField`) backed by a Core Animation rendering pipeline. Matches the Compose renderer: per-glyph stack animations, cursor, gradient fills, field-colored placeholder/fraction, Material FastOutSlowIn easing.

## Repository Layout

- [`/camount`](./camount) — core Compose Multiplatform library (common, Android, iOS, JVM, Web).
- [`/camount-view`](./camount-view) — Android Views binding.
- [`/camount-view-databinding`](./camount-view-databinding) — Android data-binding adapters.
- [`/camount-swift`](./camount-swift) — Swift Package (native iOS).
- [`/samples/shared-compose`](./samples/shared-compose) — cross-platform Compose demo screen reused by Android/iOS/Desktop/Web samples.
- [`/samples/android`](./samples/android) — Android sample app.
- [`/samples/desktop`](./samples/desktop) — Desktop (JVM) sample app.
- [`/samples/web`](./samples/web) — Compose for Web (Wasm/JS) sample — served as the live demo above.
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

Run locally:

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
- Or integrate as a Swift package by pointing at `./camount-swift` (`swift-tools-version:5.9`, iOS 16+).

Package tests:

```shell
cd camount-swift && swift test
```

## API parity

See [`camount-swift/PARITY.md`](./camount-swift/PARITY.md) for the public-API mapping between the Compose widgets and their Swift counterparts.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform).
