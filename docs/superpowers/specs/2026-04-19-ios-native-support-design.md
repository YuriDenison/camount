# iOS-Native Support for Camount — Design

**Date:** 2026-04-19
**Status:** Draft, pending implementation plan
**Goal:** Make camount a first-class UI library across Android (View + Compose), Compose Multiplatform, and iOS-native (SwiftUI) — with pixel-close, high-performance rendering on every platform.

---

## 1. Motivation

Camount today ships Compose widgets (`AmountText`, `AmountField`) plus Android View adapters. iOS consumers can only reach the library through Compose Multiplatform, which forces SwiftUI apps to embed Compose. That works but is not what a SwiftUI developer expects when they adopt a UI library.

This design adds a sibling package — **`camount-swift`** — a standalone Swift Package that reimplements the camount widgets natively in Swift + SwiftUI + Core Animation, with no Kotlin dependency at all. Behavior and visuals target parity with the Compose implementation; API shape and idiom match what a SwiftUI developer expects.

## 2. Goals and Non-Goals

### Goals

- Full feature parity with the Compose widgets in v1: `AmountText`, `AmountField`, plus the supporting `Money`, `AmountStyle`, `CursorStyle`, `ShowSign`, `FractionPolicy`, `AmountAlignment` API surface.
- Pixel-close rendering, including the per-character diff animation (digits morph in place when the value changes).
- 60fps on iPhone 8 / SE 2nd gen class hardware; 120Hz ProMotion support for free.
- Idiomatic SwiftUI API — views plus modifiers, not a literal port of the Kotlin API.
- Zero Kotlin runtime in the Swift package — pure Swift, distributed as an SPM package.
- A restructured `samples/` directory with an iOS sample that demos both CMP and native integration paths.

### Non-goals (v1)

- Automated cross-platform parity tests (screenshot or corpus). Parity is enforced by a written `PARITY.md` spec plus manual review.
- Dynamic Type / accessibility scaling. Text sizing is style-driven, matching Compose.
- Right-to-left layout mirroring.
- Customizable animation duration or easing curve.
- Desktop and web samples (separate future work).

## 3. High-Level Architecture

Two deliverables.

### 3.1 `camount-swift/` — a standalone Swift Package

```
camount-swift/
  Package.swift
  Sources/
    Camount/
      Core/
        Money.swift
        AmountConfig.swift
        CurrencyInfo.swift
        AmountFormatter.swift
        FieldRange.swift
      Rendering/
        AmountPainter.swift
        AmountCellLayer.swift
        SymbolCell.swift
        CursorCell.swift
        AmountHostView.swift
        DiffCalculator.swift
        GlyphCache.swift
      SwiftUI/
        AmountText.swift
        AmountField.swift
        AmountStyle.swift
        ShowSign.swift
        FractionPolicy.swift
        AmountAlignment.swift
        EnvironmentKeys.swift
  Tests/
    CamountTests/
      AmountFormatterTests.swift
      MoneyTests.swift
      DiffCalculatorTests.swift
      SanitizeInputTests.swift
      AmountConfigTests.swift
  PARITY.md
```

- Single SPM product, single module (`Camount`). Split later if a consumer needs core without UI.
- iOS 16 minimum. No macOS/tvOS/watchOS targets in v1.
- No third-party dependencies.
- Public API: `Money`, `AmountStyle`, `CursorStyle`, the enums, `AmountText`, `AmountField`, and the modifier functions. Everything else is `internal`.

### 3.2 Samples reorganization

```
samples/
  shared-compose/        # KMP module, commonMain-only for now
    src/commonMain/kotlin/.../CamountSampleScreen.kt
    src/commonMain/composeResources/font/manrope_medium.ttf
    build.gradle.kts     # depends on :camount

  android/               # Android app (moved from existing sample/)
    src/main/kotlin/.../SampleActivity.kt
    src/main/res/...
    build.gradle.kts     # depends on :samples:shared-compose, :camount-view

  ios/
    CamountSample.xcodeproj
    CamountSample/
      iOSApp.swift
      ContentView.swift                 # segmented control: CMP | Native
      CmpDemo.swift                     # hosts MainViewController from shared-compose
      NativeDemo/
        NativeSampleScreen.swift        # SwiftUI rewrite of CamountSampleScreen
        ...
```

The existing `sample/` and `iosApp/` directories at the repo root are removed. The library modules (`camount`, `camount-view`, `camount-view-databinding`) are unchanged.

Dependency graph:

```
samples/android          -> camount, camount-view, samples/shared-compose
samples/shared-compose   -> camount
samples/ios (CMP mode)   -> samples/shared-compose (as iOS framework)
samples/ios (Native)     -> camount-swift (SPM local path)
```

`samples/shared-compose` is a KMP module that, for iOS, exports a `CamountSampleShared` framework the iOS Xcode project links against in CMP mode. Same mechanism the current `sample` module uses today.

### 3.3 iOS sample — one app, runtime toggle

`samples/ios` is a single Xcode project. Its root view is a `TabView` or segmented picker with two modes:

- **CMP** — embeds `MainViewControllerKt.MainViewController()` from `samples/shared-compose`'s framework.
- **Native** — renders `NativeSampleScreen` (SwiftUI), which mirrors `CamountSampleScreen` layout-for-layout using `camount-swift`.

One build, instant A/B comparison during development.

## 4. Render Pipeline — Core Animation Port

The Compose rendering engine (`AmountPainter` + `SymbolCell` + `CursorCell` + `DiffCalculator`) ports to Swift with the same boundaries, renamed only where Swift conventions demand.

### 4.1 Layer ownership

`AmountHostView: UIView` owns everything rendered. It is wrapped by `AmountText` / `AmountField` via `UIViewRepresentable` — the only SwiftUI bridge. `AmountHostView` configures an `AmountPainter` (the Swift equivalent of the Compose painter) that manages a tree of `CALayer`s.

### 4.2 `AmountPainter`

A plain Swift class. State:

```swift
var cells: [SymbolCell]
var cursor: CursorCell?
var style: AmountStyle
var config: AmountConfig
var alignment: AmountAlignment
var containerSize: CGSize
var densityScale: CGFloat
var lastRenderedText: String?
```

Public methods mirror Kotlin:

- `setText(_ text: String, positions: AmountFieldPositions)` — runs the diff, updates cells, re-lays out.
- `setBounds(width: CGFloat, height: CGFloat)` — updates container, re-lays out.
- `setCursorVisible(_ visible: Bool)` — drives the cursor blink loop.
- `updateStyle(_ style: AmountStyle, config: AmountConfig, alignment: AmountAlignment)` — rebuilds the diff calculator when config or style changes; rebuilds the cursor cell when cursor style changes; re-lays out on alignment change.
- `setDensity(_ density: CGFloat)` — re-lays out when density changes.

One structural change from Compose: on iOS the scene graph **is** the render. There is no `draw(drawScope:)` — cells own `CALayer`s and update their properties directly. Core Animation's render server composites frames on its own thread.

`AmountPainter.setText` is the only entry point that can restart animations. On each call it checks `lastRenderedText == text`; if equal, it returns early. This prevents unrelated SwiftUI re-renders (which call `updateUIView` synchronously) from thrashing animations.

### 4.3 `SymbolCell` + `AmountCellLayer`

`SymbolCell` owns one `AmountCellLayer: CALayer`. The layer keeps a stack of up to 3 symbol layers (matches Kotlin's `MAX_STACK_SYMBOLS = 3`). Each stacked symbol tracks `char`, cached glyph bitmap, and its own animatable `appearance: Double` (0→1).

`AmountCellLayer.draw(in:)` composites the stack: for each symbol, read its presentation-layer `appearance` value, apply per-symbol scale `0.6 + (1.0 - 0.6) * appearance` around the glyph center, draw the cached `CGImage` with alpha `= appearance`. This matches the Compose `SymbolCell.draw` logic exactly.

To make `appearance` animatable: override `needsDisplay(forKey:)` to return `true` for `"appearance"`, and override `action(forKey:)` to return a `CABasicAnimation` configured with our duration + easing. This is the standard recipe for custom-property animation on `CALayer`.

`SymbolCell` API mirrors Kotlin 1:1:

- `replace(char:field:)` — appends to stack, cancels in-flight animations on prior symbols, fades prior symbols out, fades new symbol in.
- `delete()` — fades last symbol out, sets `isVisible = false`.
- `setTargetBounds(left:top:width:height:)` — animates the `CALayer.position` + `CALayer.bounds.size`.
- `setDuration(_:)`, `currentChar`, `isVisible`, `isRunning` — unchanged semantics.

### 4.4 Glyph caching (`GlyphCache`)

First-time rendering of `(char, fontDescriptor, fontSize, foregroundColor)` creates a `CGImage` via Core Text (`CTFontCreateWithName`, `CTFontDrawGlyphs`) into a bitmap context. Cached on the painter. Subsequent renders blit the cached image. This is the single biggest perf lever for old devices: we never re-rasterize glyphs during animations.

Gradient text (`AmountStyle.gradientBrush`) uses a per-painter `CAGradientLayer` mask over the cell layer tree, applied only to cells whose symbols are gradient-eligible (non-whitespace, non-null). Matches the Compose `isGradientTarget` rule.

### 4.5 `CursorCell`

Owns one `CALayer` with `cornerRadius = width / 2`. `setVisible(_:)` drives a blink loop: fade-in over `APPEAR_DURATION_MS = 500ms`, then alternate opacity 1↔0 every `BLINK_DURATION_MS = 530ms`. Implemented with a chained `CABasicAnimation` sequence keyed by `opacity`. Matches the Kotlin coroutine loop semantically; the driver is Core Animation, not a Swift task.

`setTargetBounds(...)` animates `position` + `bounds.size` on the cursor layer using the same 120ms duration.

### 4.6 Animation driver

All animations run via `CABasicAnimation` on `CALayer` properties:

- `position`, `bounds.size` — cell and cursor layout changes.
- `appearance` (custom property) — per-symbol fade + scale.
- `opacity` — cursor blink.

**Duration:** 120ms, matching Kotlin's `DIFF_ANIMATION_DURATION_MS`.
**Easing:** `CAMediaTimingFunction(name: .default)`. Closest native iOS fit; an explicit divergence from Android's Material easing, documented in `PARITY.md`.

Core Animation runs the interpolation on the render server, off-main-thread, hardware-accelerated. This is the key perf decision — it's how we hit 60fps on iPhone 8 under CPU load.

No `CADisplayLink` is required in steady state. The presentation-layer's animated `appearance` value is read inside `AmountCellLayer.draw(in:)` and is automatically interpolated by Core Animation.

### 4.7 Diff algorithms

`EditDiffCalculator` and `LevenshteinDiffCalculator` port line-for-line from Kotlin. Pure Swift, no platform calls, zero algorithmic changes. `AmountText` uses Levenshtein; `AmountField` uses Edit — same as Compose.

### 4.8 Formatter + config

`AmountFormatter`, `AmountConfig`, `FieldRange`, `AmountFieldPositions` port line-for-line from Kotlin.

`CurrencyInfo(currencyCode:)` on Swift uses `NumberFormatter`:

```swift
let fmt = NumberFormatter()
fmt.numberStyle = .currency
fmt.currencyCode = currencyCode
fmt.locale = Locale(identifier: NSLocale.localeIdentifier(
    fromComponents: [NSLocale.Key.currencyCode.rawValue: currencyCode]))
```

Then extract `currencyDecimalSeparator`, `currencyGroupingSeparator`, `positivePrefix`, `positiveSuffix`, `groupingSize`, `maximumFractionDigits`. The `sanitizeBidi` function from the Kotlin iOS actual also ports — same U+200E…U+2069 ranges stripped.

### 4.9 Input handling (`AmountField`)

`AmountHostView` contains a hidden `UITextField` (`alpha = 0.01`, `tintColor = .clear`, `keyboardType = .decimalPad`, `textContentType = nil`). The layer tree renders on top. The field exists solely for keyboard invocation and cursor position tracking.

`UITextFieldDelegate.textField(_:shouldChangeCharactersIn:replacementString:)` runs `sanitizeInput` (port of the Kotlin function), updates the text field, then:

1. `inputFormatter.format(source, start, end, text, textStart, textEnd)` → formatted + field positions.
2. `painter.setText(formatted, positions)` — visible rendering animates.
3. `displayFormatter.parse(sanitizedText, currency)` → `Money`.
4. If parsed money differs from last value sent out: `binding.wrappedValue = parsedMoney`.

Two formatters per field, same split as Compose:

- `inputFormatter` — `withCurrency = false`, `withGroupingSeparators = false`, `withFixedFractionLength = false`, `withFixedZeroNotation = true`. Drives the hidden `UITextField`.
- `displayFormatter` — default construction (full formatting). Drives the painter.

When the parent `Money` binding changes externally (e.g. Shuffle button), `updateUIView` compares the incoming `Money` to `lastParsedMoney`, and if different, pushes the newly formatted value through both the hidden field and the painter. This mirrors Compose's `LaunchedEffect(amount)`.

### 4.10 Public API (idiomatic SwiftUI)

```swift
public struct Money: Equatable, Hashable {
    public let units: Int64
    public let nanos: Int32
    public let currencyCode: String
    public init(units: Int64, nanos: Int32, currencyCode: String)
    public static func zero(_ currencyCode: String) -> Money
    public var isPositive: Bool { get }
    public var isZero: Bool { get }
}

public struct AmountStyle {
    public let font: UIFont
    public let color: UIColor
    public let gradient: Gradient?            // SwiftUI.Gradient
    public let cursor: CursorStyle?
    public let zeroNotationColor: UIColor?
    public let fixedFractionColor: UIColor?
    public init(...)
}

public struct CursorStyle {
    public let color: UIColor
    public let width: CGFloat                 // points
    public let heightFraction: CGFloat        // 0...1
}

public enum ShowSign { case ifNegative, always }
public enum FractionPolicy { case fixed, ignoreZero }
public enum AmountAlignment { case start, center, end }

public struct AmountText: View {
    public init(_ amount: Money)
    public var body: some View { /* UIViewRepresentable */ }
}

public struct AmountField: View {
    public init(_ amount: Binding<Money>)
    public var body: some View { /* UIViewRepresentable */ }
}

public extension View {
    func amountStyle(_ style: AmountStyle) -> some View
    func showSign(_ value: ShowSign) -> some View
    func fractionPolicy(_ value: FractionPolicy) -> some View
    func maximumNotationDigits(_ value: Int) -> some View
    func amountAlignment(_ value: AmountAlignment) -> some View
}
```

Modifiers are backed by `EnvironmentValues` extensions. Each `UIViewRepresentable` reads the environment in `updateUIView` and pushes the current style/flags into its `AmountHostView`. Standard SwiftUI pattern, identical to `.font` / `.foregroundColor`.

Notes:

- `showSign` and `fractionPolicy` are meaningful on `AmountText` only; `AmountField` silently ignores them (they don't apply to editing).
- `AmountStyle` uses `UIFont` and `UIColor` — not SwiftUI's `Font` / `Color`. Reason: we need concrete `UIFont` instances for Core Text glyph rasterization, and concrete `CGColor` for layer drawing. Users who work in SwiftUI idioms can bridge trivially (`UIFont.systemFont(...)`, `UIColor(Color.red)`). Future iteration may add SwiftUI-typed conveniences.

## 5. Data Flow

### 5.1 `AmountText` (read-only)

```
SwiftUI state (@State var money: Money)
    │
    ▼ value binding
AmountText(money) → UIViewRepresentable.updateUIView
    │
    ▼ AmountHostView.setAmount(money, showSign, fractionPolicy, ...)
AmountFormatter.format(money) → "1 234,56 €"
    │
    ▼ AmountPainter.setText(formatted, .empty)
DiffCalculator (Levenshtein) reconciles cells with new text
    │
    ▼ per-cell CABasicAnimation
Render server composites next frame
```

### 5.2 `AmountField` (editable)

```
User tap
    │
    ▼
Hidden UITextField becomes first responder → decimal keyboard
    │
    ▼ delegate shouldChangeCharactersIn(range, replacement)
sanitizeInput(candidate, config) → filtered string + cursor index
    │
    ▼ inputFormatter.format(source, start, end, text, textStart, textEnd)
     → formatted + AmountFieldPositions
AmountPainter.setText(formatted, positions)        (render)
    │
    └─→ displayFormatter.parse(sanitizedText, currency) → Money
            │
            ▼ if differs from lastParsedMoney: binding.wrappedValue = newMoney
```

### 5.3 External updates during editing

When the parent `Money` binding changes while the user is editing (e.g. Shuffle), `updateUIView` runs, detects `incomingMoney != lastParsedMoney`, reformats via `inputFormatter`, pushes the new text into the hidden `UITextField` (with selection at end), and `painter.setText` handles the visual transition. The cursor position resets to the end of the field.

### 5.4 Threading

Everything public is main-thread only (SwiftUI contract). The painter is main-thread only. Core Animation's render server runs on its own thread; we hand it a layer tree and stay out of its way.

## 6. Error Handling and Edge Cases

| Case | Behavior |
|------|----------|
| Unknown currency code | `NumberFormatter` returns sensible defaults. No throw. Zero fraction digits (e.g. JPY) is handled — decimal path skipped, matching Kotlin. |
| Invalid `Money` (mismatched sign) | Rendered as-is. No validation, no assertion. Matches Compose. |
| `units` overflows `maximumNotationDigits` | Truncated to max, per Kotlin `format(money)`. |
| `AmountField` paste of giant string | `sanitizeInput` enforces per-pass digit caps. Cursor mapped to new position. |
| Rapid value changes (+1 spam) | In-flight `CABasicAnimation` for affected layer key removed before new one added; starts from current presentation value; no snap. |
| SwiftUI state oscillation | Bridge tracks `lastParsedMoney` and only writes back when parsed value differs from the last one it emitted. Mirrors Compose's `LaunchedEffect` guard. |
| View recycling | `makeUIView` once per logical view; `updateUIView` on every re-render. All engine state lives on `AmountHostView` and persists across updates. |
| Keyboard dismissal | `UITextField.didEndEditing` → `painter.setCursorVisible(false)`. Cursor blink loop stops, fades out. |
| Dynamic Type | Out of scope v1. Text size is style-driven, identical to Compose behavior. |
| Dark mode | Caller-controlled via `AmountStyle`. Library ships no theme. |
| RTL layout | Out of scope v1. `sanitizeBidi` still applied (matches Kotlin actual). |
| ProMotion 120Hz | Automatic via `CABasicAnimation`. |

## 7. Testing

### 7.1 In scope

XCTest unit tests in `Tests/CamountTests`:

- `AmountFormatterTests` — `format(Money)`, `format(source, ...)`, `parse(raw, currency)` across representative currencies (EUR, USD, JPY with zero fractions, an RTL-suffix currency).
- `MoneyTests` — `isPositive`, `isZero`, `compareTo`, `zero(currencyCode:)`.
- `DiffCalculatorTests` — both calculators. Fed sequences of text states; assert each cell's `currentChar` + `isVisible` after each step. No animation-timing assertions.
- `SanitizeInputTests` — digit/separator filtering, cursor remapping, max-digit enforcement.
- `AmountConfigTests` — `maximumFormattedSymbols`, digit/separator predicates.

### 7.2 Out of scope (v1)

- Pixel-level snapshot tests.
- Animation-timing tests (trust Core Animation).
- `UIViewRepresentable` integration (exercised by the sample app).
- `CurrencyInfo` via `NumberFormatter` (Apple-owned data; test only if a specific regression appears).

### 7.3 `samples/ios` as integration harness

The CMP/Native toggle is a live parity check. During development: switch modes on device, verify same `Money` renders the same formatted string, animations behave equivalently.

### 7.4 `PARITY.md`

Ships in `camount-swift/`. Enumerates:

- Diff algorithm step-by-step (both modes).
- Animation parameters (120ms duration; iOS `.default` easing, explicit divergence from Android Material `FastOutSlowInEasing`).
- Cell stack depth (3); per-symbol scale curve (0.6 → 1.0).
- Field position semantics (`ZeroNotation`, `FixedFraction`).
- Keystroke sanitization rules.
- Explicit divergences: easing curve; Dynamic Type (off); RTL (off).

`PARITY.md` is the source of truth for behavior. Both implementations must match it; when they diverge, the spec wins or the spec is updated with intent.

## 8. Build Order

Suggested implementation sequence, each phase independently mergeable:

1. **Restructure samples.** Create `samples/shared-compose`, move `CamountSampleScreen` + font resources. Move `sample/` Android bits into `samples/android`. Verify Android app still builds and runs.
2. **Port core (pure Swift).** `Money`, `AmountConfig`, `FieldRange`, `CurrencyInfo`, `AmountFormatter`. XCTest unit tests pass.
3. **Port diff calculators.** `EditDiffCalculator`, `LevenshteinDiffCalculator`. Tests pass.
4. **Render engine — read-only.** `GlyphCache`, `AmountCellLayer`, `SymbolCell`, `AmountPainter`, `AmountHostView` (no text field yet). Build `AmountText` + modifiers + environment keys. A minimal throwaway iOS host app proves rendering + animation.
5. **Editable field.** Add hidden `UITextField`, delegate, `sanitizeInput`, `AmountField` view. Cursor cell + blink.
6. **`samples/ios` Xcode project.** SwiftUI app with segmented mode toggle; CMP mode hosts `shared-compose`'s framework; Native mode renders `NativeSampleScreen` using `camount-swift`.
7. **`PARITY.md`** written alongside phases 2–5 as each subsystem lands.
8. **Root `iosApp/` and `sample/` removed** once `samples/ios` and `samples/android` replace them.

## 9. Open Questions

None pending. All forks resolved during brainstorming:

- iOS-native path: standalone pure-Swift package (not Kotlin bridge).
- Minimum iOS: 16.
- API idiom: SwiftUI modifiers.
- Parity enforcement: written spec + discipline, no automated cross-platform test harness.
- Render pipeline: `CALayer` tree + `CABasicAnimation`, not SwiftUI `Canvas` or raw Core Text.
- Easing: iOS `.default`.
- Duration: 120ms.
- Sample reorg: `samples/{shared-compose, android, ios}` with a runtime CMP/Native toggle in the iOS sample.
