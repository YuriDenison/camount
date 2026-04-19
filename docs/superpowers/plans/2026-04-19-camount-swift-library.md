# Camount-Swift Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `camount-swift/` — a standalone Swift Package that reimplements camount's `AmountText` and `AmountField` natively in SwiftUI + Core Animation with full parity to the Compose implementation, for iOS 16+, with no Kotlin dependency.

**Architecture:** Single SPM product (`Camount`) organized into Core (Money, formatter, config), Rendering (painter, cell layer, cursor, diff), and SwiftUI (views + environment modifiers). Rendering uses a tree of `CALayer`s driven by `CABasicAnimation` for hardware-accelerated, off-main-thread animation. `UIViewRepresentable` bridges the layer tree into SwiftUI.

**Tech Stack:** Swift 5.9+, SwiftUI, UIKit, Core Animation, Core Text. iOS 16 minimum. No third-party dependencies. Tests use XCTest.

**Spec:** `docs/superpowers/specs/2026-04-19-ios-native-support-design.md` §3.1, §4, §6, §7.

**Prerequisite:** None of this depends on plan 1 (samples reorg). The new package stands alone; integration with `samples/ios` is plan 3.

---

## File Structure

```
camount-swift/
  Package.swift
  PARITY.md
  Sources/
    Camount/
      Core/
        Money.swift                    # Struct, isPositive/isZero/zero/compareTo
        FieldRange.swift               # Struct, range + AmountField + AmountFieldPositions
        AmountConfig.swift             # Formatter config + digit/separator predicates
        CurrencyInfo.swift             # NumberFormatter-backed, sanitizeBidi
        AmountFormatter.swift          # format(Money), format(source,start,end,...), parse, cursor/field tracking
      Rendering/
        DiffCalculator.swift           # Protocol + Edit + Levenshtein calculators
        GlyphCache.swift               # Caches CGImage per (char, font, color)
        AmountCellLayer.swift          # CALayer subclass; animatable `appearance`; stack of symbols
        SymbolCell.swift               # Wrapper: replace/delete/setTargetBounds; owns AmountCellLayer
        CursorCell.swift               # CALayer + blink loop; setTargetBounds
        AmountPainter.swift            # Orchestrates cells + cursor; setText/setBounds/updateStyle/setDensity
        AmountHostView.swift           # UIView root; owns painter; hosts hidden UITextField for AmountField
      SwiftUI/
        AmountStyle.swift              # AmountStyle + CursorStyle structs
        Enums.swift                    # ShowSign, FractionPolicy, AmountAlignment
        EnvironmentKeys.swift          # EnvironmentValues extensions + view modifiers
        AmountText.swift               # UIViewRepresentable
        AmountField.swift              # UIViewRepresentable + Binding
  Tests/
    CamountTests/
      MoneyTests.swift
      AmountConfigTests.swift
      AmountFormatterTests.swift
      DiffCalculatorTests.swift
      SanitizeInputTests.swift
      CurrencyInfoTests.swift
```

Internal visibility by default; only the SwiftUI types + `Money` + `AmountStyle` + `CursorStyle` + the three enums + the modifier functions are `public`.

---

## Prerequisites and Invariants

- Use a feature branch (not `main`).
- TDD where practical: write the XCTest first for every Core/Rendering component that has deterministic behavior. UI bridging (UIViewRepresentable, UITextField delegate) is exercised by the `samples/ios` app in plan 3; no automated UI tests in this plan.
- Every task ends with a verification step — run it and confirm it passes before proceeding.
- Use exact staging paths in `git add`; never `git add -A`.
- All Swift source files use 2-space indentation and `// MARK:` section comments to match iOS community norm. This repo has no pre-existing Swift code to conflict with.
- Do not introduce third-party dependencies.

---

## Task 1: Create the `camount-swift` SPM package

**Files:**
- Create: `camount-swift/Package.swift`
- Create: `camount-swift/Sources/Camount/Camount.swift` (empty umbrella; deleted once real sources exist)
- Create: `camount-swift/Tests/CamountTests/PlaceholderTests.swift` (deleted once real tests exist)

- [ ] **Step 1: Create the directory structure**

From repo root:
```bash
mkdir -p camount-swift/Sources/Camount/Core
mkdir -p camount-swift/Sources/Camount/Rendering
mkdir -p camount-swift/Sources/Camount/SwiftUI
mkdir -p camount-swift/Tests/CamountTests
```

- [ ] **Step 2: Write `camount-swift/Package.swift`**

```swift
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "Camount",
  platforms: [
    .iOS(.v16),
  ],
  products: [
    .library(name: "Camount", targets: ["Camount"]),
  ],
  targets: [
    .target(
      name: "Camount",
      path: "Sources/Camount"
    ),
    .testTarget(
      name: "CamountTests",
      dependencies: ["Camount"],
      path: "Tests/CamountTests"
    ),
  ]
)
```

- [ ] **Step 3: Write a placeholder target file so the package resolves**

`camount-swift/Sources/Camount/Camount.swift`:

```swift
// Placeholder — deleted once real sources land.
enum CamountPackageMarker {}
```

- [ ] **Step 4: Write a placeholder test**

`camount-swift/Tests/CamountTests/PlaceholderTests.swift`:

```swift
import XCTest
@testable import Camount

final class PlaceholderTests: XCTestCase {
  func testPackageBuilds() {
    XCTAssertEqual(String(describing: CamountPackageMarker.self), "CamountPackageMarker")
  }
}
```

- [ ] **Step 5: Verify the package builds and the test runs**

```bash
cd camount-swift
swift build
swift test
cd ..
```

Expected: `Build complete!` and `Test Suite 'All tests' passed`.

If `swift build` reports `target ... has no sources`, confirm Step 3's file is in the exact path `camount-swift/Sources/Camount/Camount.swift`.

- [ ] **Step 6: Commit**

```bash
git add camount-swift/Package.swift \
        camount-swift/Sources/Camount/Camount.swift \
        camount-swift/Tests/CamountTests/PlaceholderTests.swift
git commit -m "Scaffold camount-swift SPM package (iOS 16+)"
```

---

## Task 2: Implement `Money`

**Files:**
- Create: `camount-swift/Sources/Camount/Core/Money.swift`
- Create: `camount-swift/Tests/CamountTests/MoneyTests.swift`
- Delete: `camount-swift/Sources/Camount/Camount.swift` (placeholder no longer needed once real source lands)

- [ ] **Step 1: Write the failing test**

`camount-swift/Tests/CamountTests/MoneyTests.swift`:

```swift
import XCTest
@testable import Camount

final class MoneyTests: XCTestCase {

  func testIsZero() {
    XCTAssertTrue(Money(units: 0, nanos: 0, currencyCode: "USD").isZero)
    XCTAssertFalse(Money(units: 1, nanos: 0, currencyCode: "USD").isZero)
    XCTAssertFalse(Money(units: 0, nanos: 1, currencyCode: "USD").isZero)
  }

  func testIsPositive() {
    XCTAssertTrue(Money(units: 1, nanos: 0, currencyCode: "USD").isPositive)
    XCTAssertTrue(Money(units: 0, nanos: 1, currencyCode: "USD").isPositive)
    XCTAssertFalse(Money(units: 0, nanos: 0, currencyCode: "USD").isPositive)
    XCTAssertFalse(Money(units: -1, nanos: 0, currencyCode: "USD").isPositive)
    XCTAssertFalse(Money(units: 0, nanos: -1, currencyCode: "USD").isPositive)
  }

  func testZeroFactory() {
    XCTAssertEqual(Money.zero("EUR"), Money(units: 0, nanos: 0, currencyCode: "EUR"))
  }

  func testCompare() {
    let a = Money(units: 1, nanos: 0, currencyCode: "USD")
    let b = Money(units: 1, nanos: 500_000_000, currencyCode: "USD")
    let c = Money(units: 2, nanos: 0, currencyCode: "USD")
    XCTAssertLessThan(a, b)
    XCTAssertLessThan(b, c)
    XCTAssertEqual(a, Money(units: 1, nanos: 0, currencyCode: "USD"))
  }

  func testHashable() {
    let set: Set<Money> = [
      Money(units: 1, nanos: 0, currencyCode: "USD"),
      Money(units: 1, nanos: 0, currencyCode: "USD"),
    ]
    XCTAssertEqual(set.count, 1)
  }
}
```

- [ ] **Step 2: Run the test — verify it fails**

```bash
cd camount-swift && swift test --filter MoneyTests && cd ..
```

Expected: compilation failure (`cannot find 'Money' in scope`).

- [ ] **Step 3: Implement `Money`**

`camount-swift/Sources/Camount/Core/Money.swift`:

```swift
import Foundation

public struct Money: Equatable, Hashable, Comparable, Sendable {
  public let units: Int64
  public let nanos: Int32
  public let currencyCode: String

  public init(units: Int64, nanos: Int32, currencyCode: String) {
    self.units = units
    self.nanos = nanos
    self.currencyCode = currencyCode
  }

  public static func zero(_ currencyCode: String) -> Money {
    Money(units: 0, nanos: 0, currencyCode: currencyCode)
  }

  public var isZero: Bool { units == 0 && nanos == 0 }

  public var isPositive: Bool {
    units > 0 || (units == 0 && nanos > 0)
  }

  public static func < (lhs: Money, rhs: Money) -> Bool {
    if lhs.units != rhs.units { return lhs.units < rhs.units }
    return lhs.nanos < rhs.nanos
  }
}

extension Money {
  var absoluteUnits: Int64 { units < 0 ? -units : units }
  var absoluteNanos: Int32 { nanos < 0 ? -nanos : nanos }
}
```

- [ ] **Step 4: Delete the placeholder source now that a real source exists**

```bash
rm camount-swift/Sources/Camount/Camount.swift
```

Also delete the placeholder test file:

```bash
rm camount-swift/Tests/CamountTests/PlaceholderTests.swift
```

- [ ] **Step 5: Run tests**

```bash
cd camount-swift && swift test --filter MoneyTests && cd ..
```

Expected: `Test Suite 'MoneyTests' passed`.

- [ ] **Step 6: Commit**

```bash
git add camount-swift/Sources/Camount/Core/Money.swift \
        camount-swift/Tests/CamountTests/MoneyTests.swift
git rm camount-swift/Sources/Camount/Camount.swift \
       camount-swift/Tests/CamountTests/PlaceholderTests.swift
git commit -m "Add Money value type"
```

---

## Task 3: Implement `FieldRange`, `AmountField`, `AmountFieldPositions`

**Files:**
- Create: `camount-swift/Sources/Camount/Core/FieldRange.swift`

No test file yet — these types are trivial data holders covered indirectly by `AmountFormatterTests` in Task 5.

- [ ] **Step 1: Write `FieldRange.swift`**

```swift
import Foundation

struct FieldRange: Equatable {
  var beginIndex: Int
  var endIndex: Int

  init(beginIndex: Int = 0, endIndex: Int = 0) {
    self.beginIndex = beginIndex
    self.endIndex = endIndex
  }

  var length: Int { isValid ? endIndex - beginIndex : 0 }
  var isValid: Bool { beginIndex < endIndex && beginIndex >= 0 }

  mutating func clear() {
    beginIndex = 0
    endIndex = 0
  }

  mutating func offset(by value: Int) {
    guard isValid else { return }
    beginIndex += value
    endIndex += value
  }

  func contains(_ index: Int) -> Bool {
    index >= beginIndex && index < endIndex
  }
}

enum AmountField {
  case fixedFraction
  case zeroNotation
  case currencySuffix
}

struct AmountFieldPositions {
  let cursorPosition: Int
  let fixedFraction: FieldRange
  let zeroNotation: FieldRange

  static let empty = AmountFieldPositions(
    cursorPosition: -1,
    fixedFraction: FieldRange(),
    zeroNotation: FieldRange()
  )
}
```

- [ ] **Step 2: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Sources/Camount/Core/FieldRange.swift
git commit -m "Add FieldRange, AmountField, AmountFieldPositions"
```

---

## Task 4: Implement `AmountConfig` with tests

**Files:**
- Create: `camount-swift/Sources/Camount/Core/AmountConfig.swift`
- Create: `camount-swift/Tests/CamountTests/AmountConfigTests.swift`

- [ ] **Step 1: Write the failing test**

`camount-swift/Tests/CamountTests/AmountConfigTests.swift`:

```swift
import XCTest
@testable import Camount

final class AmountConfigTests: XCTestCase {

  private func euro(maxNotation: Int = 5) -> AmountConfig {
    AmountConfig(
      maximumNotationDigits: maxNotation,
      decimalSeparator: ",",
      groupingSeparator: " ",
      prefix: "",
      suffix: " €",
      groupingSize: 3,
      maximumFractionDigits: 2
    )
  }

  func testDigitPredicate() {
    let c = euro()
    for ch: Character in "0123456789" { XCTAssertTrue(c.isDigit(ch)) }
    for ch: Character in "abc,. €" { XCTAssertFalse(c.isDigit(ch)) }
  }

  func testZeroIsFirstDigit() {
    let c = euro()
    XCTAssertEqual(c.zero, "0")
    XCTAssertTrue(c.isZero("0"))
    XCTAssertFalse(c.isZero("1"))
  }

  func testInputSeparator() {
    let c = euro()
    XCTAssertTrue(c.isInputSeparator("."))
    XCTAssertTrue(c.isInputSeparator(","))
    XCTAssertFalse(c.isInputSeparator(" "))
  }

  func testDecimalSeparator() {
    let c = euro()
    XCTAssertTrue(c.isDecimalSeparator(","))
    XCTAssertFalse(c.isDecimalSeparator("."))
  }

  func testGroupingSeparator() {
    let c = euro()
    XCTAssertTrue(c.isGroupingSeparator(" "))
    XCTAssertFalse(c.isGroupingSeparator(","))
  }

  func testGroupingSeparatorFalseWhenGroupingSizeZero() {
    let c = AmountConfig(
      maximumNotationDigits: 5,
      decimalSeparator: ".",
      groupingSeparator: ",",
      prefix: "",
      suffix: "",
      groupingSize: 0,
      maximumFractionDigits: 0
    )
    XCTAssertFalse(c.isGroupingSeparator(","))
  }

  func testMaximumFormattedSymbols() {
    // prefix(0) + notation(5) + groupingSeps((5-1)/3=1) + decimalSep(1) + fraction(2) + suffix(2) = 11
    XCTAssertEqual(euro().maximumFormattedSymbols, 11)
  }
}
```

- [ ] **Step 2: Run the test — verify it fails**

```bash
cd camount-swift && swift test --filter AmountConfigTests && cd ..
```

Expected: compilation failure.

- [ ] **Step 3: Implement `AmountConfig`**

`camount-swift/Sources/Camount/Core/AmountConfig.swift`:

```swift
import Foundation

struct AmountConfig: Equatable {
  let maximumNotationDigits: Int
  let decimalSeparator: Character
  let groupingSeparator: Character
  let prefix: String
  let suffix: String
  let groupingSize: Int
  let maximumFractionDigits: Int

  private static let digitChars: [Character] =
    ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]

  var zero: Character { Self.digitChars[0] }

  var maximumFormattedSymbols: Int {
    let groupingSeparators = groupingSize == 0
      ? 0
      : (maximumNotationDigits - 1) / groupingSize
    return prefix.count +
      maximumNotationDigits +
      groupingSeparators +
      1 + maximumFractionDigits +
      suffix.count
  }

  func isDigit(_ c: Character) -> Bool { Self.digitChars.contains(c) }
  func isZero(_ c: Character) -> Bool { zero == c }
  func isInputSeparator(_ c: Character) -> Bool { c == "." || c == "," }
  func isDecimalSeparator(_ c: Character) -> Bool { decimalSeparator == c }
  func isGroupingSeparator(_ c: Character) -> Bool {
    groupingSize > 0 && groupingSeparator == c
  }

  func digit(at index: Int) -> Character { Self.digitChars[index] }
}
```

- [ ] **Step 4: Run tests**

```bash
cd camount-swift && swift test --filter AmountConfigTests && cd ..
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add camount-swift/Sources/Camount/Core/AmountConfig.swift \
        camount-swift/Tests/CamountTests/AmountConfigTests.swift
git commit -m "Add AmountConfig with digit/separator predicates"
```

---

## Task 5: Implement `AmountFormatter`

This task is large — the formatter is the biggest pure-logic port. Work step-by-step; keep the Kotlin source (`camount/src/commonMain/kotlin/io/denison/camount/formatter/AmountFormatter.kt`) open for reference.

**Files:**
- Create: `camount-swift/Sources/Camount/Core/AmountFormatter.swift`
- Create: `camount-swift/Tests/CamountTests/AmountFormatterTests.swift`

- [ ] **Step 1: Write the initial failing tests**

`camount-swift/Tests/CamountTests/AmountFormatterTests.swift`:

```swift
import XCTest
@testable import Camount

final class AmountFormatterTests: XCTestCase {

  // MARK: - Test configs

  private let euroConfig = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ",",
    groupingSeparator: " ",
    prefix: "",
    suffix: " €",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  private let usdConfig = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "$",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  private let yenConfig = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "¥",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 0
  )

  // MARK: - format(Money) — positive cases

  func testFormatSmallInteger() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 42, nanos: 0, currencyCode: "USD")))
    XCTAssertEqual(result, "$42")
  }

  func testFormatWithFraction() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 1, nanos: 500_000_000, currencyCode: "USD")))
    XCTAssertEqual(result, "$1.50")
  }

  func testFormatSuffixCurrency() {
    let f = AmountFormatter(config: euroConfig)
    let result = String(f.format(Money(units: 1234, nanos: 560_000_000, currencyCode: "EUR")))
    XCTAssertEqual(result, "1234,56 €")
  }

  func testFormatZeroFractionCurrency() {
    let f = AmountFormatter(config: yenConfig)
    let result = String(f.format(Money(units: 12345, nanos: 0, currencyCode: "JPY")))
    XCTAssertEqual(result, "¥12345")
  }

  func testFormatNegativeIsUnsignedHere() {
    // AmountFormatter produces unsigned output; sign handling happens in AmountText.
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: -42, nanos: 0, currencyCode: "USD")))
    XCTAssertEqual(result, "$42")
  }

  func testFormatNanosPaddedToNine() {
    // 1 unit + 50_000_000 nanos = 1.05 (fraction = "050000000"[0..<2] = "05")
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 1, nanos: 50_000_000, currencyCode: "USD")))
    XCTAssertEqual(result, "$1.05")
  }

  func testFormatOverflowNotationTruncated() {
    let f = AmountFormatter(config: usdConfig)
    let result = String(f.format(Money(units: 1_234_567, nanos: 0, currencyCode: "USD")))
    // maximumNotationDigits = 5, so "1234567" → "12345"
    XCTAssertEqual(result, "$12345")
  }

  func testFormatZeroNanosAndFixedFractionTrue() {
    let f = AmountFormatter(config: usdConfig, withFixedFractionLength: true)
    let result = String(f.format(Money(units: 10, nanos: 0, currencyCode: "USD")))
    // No nanos ⇒ no fraction emitted by format(Money); it only emits when nanos != 0.
    XCTAssertEqual(result, "$10")
  }

  // MARK: - parse

  func testParseUnitsOnly() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("42", currencyCode: "USD"),
                   Money(units: 42, nanos: 0, currencyCode: "USD"))
  }

  func testParseWithFraction() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("1.5", currencyCode: "USD"),
                   Money(units: 1, nanos: 500_000_000, currencyCode: "USD"))
  }

  func testParseNegative() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("-2", currencyCode: "USD"),
                   Money(units: -2, nanos: 0, currencyCode: "USD"))
  }

  func testParseIgnoresGroupingAndCurrency() {
    let f = AmountFormatter(config: euroConfig)
    XCTAssertEqual(f.parse("1 234,56 €", currencyCode: "EUR"),
                   Money(units: 1234, nanos: 560_000_000, currencyCode: "EUR"))
  }

  func testParseEmpty() {
    let f = AmountFormatter(config: usdConfig)
    XCTAssertEqual(f.parse("", currencyCode: "USD"),
                   Money(units: 0, nanos: 0, currencyCode: "USD"))
  }

  // MARK: - format(source, start, end, text, textStart, textEnd) input flow

  func testFormatInputFlowSimpleDigit() {
    let f = AmountFormatter(
      config: usdConfig,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true
    )
    // Empty source, insert "5" at start → "5"
    let result = String(f.format(source: "", start: 0, end: 0, text: "5", textStart: 0, textEnd: 1))
    XCTAssertEqual(result, "5")
  }

  func testFormatInputFlowDecimalSeparator() {
    let f = AmountFormatter(
      config: usdConfig,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true
    )
    // Source "5", insert "." at position 1 → "5."
    let result = String(f.format(source: "5", start: 1, end: 1, text: ".", textStart: 0, textEnd: 1))
    // The formatter will emit decimalSeparator (config is USD → ".")
    XCTAssertTrue(result.hasPrefix("5.") || result.hasPrefix("5,"),
                  "Expected '5.' or '5,' got '\(result)'")
  }
}
```

- [ ] **Step 2: Run — verify compilation failure**

```bash
cd camount-swift && swift test --filter AmountFormatterTests && cd ..
```

Expected: compilation failure — `AmountFormatter` not defined.

- [ ] **Step 3: Implement `AmountFormatter`**

`camount-swift/Sources/Camount/Core/AmountFormatter.swift`:

```swift
import Foundation

final class AmountFormatter {

  private let config: AmountConfig
  private let withCurrency: Bool
  private let withGroupingSeparators: Bool
  private let withFixedFractionLength: Bool
  private let withFixedZeroNotation: Bool

  private var fixedFractionPosition = FieldRange()
  private var zeroNotationPosition = FieldRange()
  private var currencySuffixPosition = FieldRange()
  private var cursorPosition = 0

  private var notation = ""
  private var separatorFound = false
  private var duplicateSeparator = false
  private var fraction = ""
  private var resultBuffer = ""

  init(
    config: AmountConfig,
    withCurrency: Bool = true,
    withGroupingSeparators: Bool = true,
    withFixedFractionLength: Bool = true,
    withFixedZeroNotation: Bool = true
  ) {
    self.config = config
    self.withCurrency = withCurrency
    self.withGroupingSeparators = withGroupingSeparators
    self.withFixedFractionLength = withFixedFractionLength
    self.withFixedZeroNotation = withFixedZeroNotation
  }

  // MARK: - Public API

  func format(_ money: Money) -> String {
    reset()

    let unitsStr = String(money.absoluteUnits)
    let units = unitsStr.count <= config.maximumNotationDigits
      ? unitsStr
      : String(unitsStr.prefix(config.maximumNotationDigits))
    resultBuffer.append(units)

    if config.maximumFractionDigits > 0 && money.nanos != 0 {
      let nanosPadded = String(repeating: "0",
                               count: max(0, 9 - String(money.absoluteNanos).count))
        + String(money.absoluteNanos)
      let nanos = nanosPadded.count <= config.maximumFractionDigits
        ? nanosPadded
        : String(nanosPadded.prefix(config.maximumFractionDigits))

      if withFixedFractionLength || nanos.contains(where: { $0 != config.zero }) {
        resultBuffer.append(config.decimalSeparator)
        resultBuffer.append(nanos)
      }
    }

    appendCurrency()
    return resultBuffer
  }

  func format(
    source: String,
    start: Int,
    end: Int,
    text: String,
    textStart: Int,
    textEnd: Int
  ) -> String {
    reset()

    _ = appendRange(source, from: 0, upTo: start, withInputSeparator: false)
    let afterChangeCount = appendRange(text, from: textStart, upTo: textEnd, withInputSeparator: true)
    _ = appendRange(source, from: end, upTo: source.count, withInputSeparator: false)

    if duplicateSeparator {
      cursorPosition = end
      return source
    }
    let out = buildResult()
    cursorPosition = findSelection(in: out, selection: afterChangeCount)
    return out
  }

  var lastCursorPosition: Int { cursorPosition }

  func fieldPositions() -> AmountFieldPositions {
    AmountFieldPositions(
      cursorPosition: cursorPosition,
      fixedFraction: FieldRange(
        beginIndex: fixedFractionPosition.beginIndex,
        endIndex: fixedFractionPosition.endIndex
      ),
      zeroNotation: FieldRange(
        beginIndex: zeroNotationPosition.beginIndex,
        endIndex: zeroNotationPosition.endIndex
      )
    )
  }

  func parse(_ raw: String, currencyCode: String) -> Money {
    var negative = false
    var separator = false
    var hasDigits = false
    var integer = ""
    var fractionDigits = ""

    for c in raw {
      if c == "-" && !hasDigits {
        negative = true
      } else if config.isInputSeparator(c) {
        if !separator { separator = true }
      } else if config.isDigit(c) {
        hasDigits = true
        if separator { fractionDigits.append(c) } else { integer.append(c) }
      }
    }

    if !hasDigits { return Money(units: 0, nanos: 0, currencyCode: currencyCode) }

    let units = Int64(integer.isEmpty ? "0" : integer) ?? 0
    let nanoDigits = (fractionDigits
      + String(repeating: "0", count: max(0, 9 - fractionDigits.count))).prefix(9)
    let nanos = Int32(nanoDigits) ?? 0

    let signedUnits = negative ? -units : units
    let signedNanos = negative ? -nanos : nanos
    return Money(units: signedUnits, nanos: signedNanos, currencyCode: currencyCode)
  }

  // MARK: - Internals

  private func reset() {
    notation.removeAll(keepingCapacity: true)
    separatorFound = false
    duplicateSeparator = false
    fraction.removeAll(keepingCapacity: true)
    resultBuffer.removeAll(keepingCapacity: true)
    fixedFractionPosition.clear()
    zeroNotationPosition.clear()
    currencySuffixPosition.clear()
    cursorPosition = 0
  }

  private func appendRange(
    _ source: String,
    from start: Int,
    upTo end: Int,
    withInputSeparator: Bool
  ) -> Int {
    var count = 0
    let chars = Array(source)
    let clampedStart = max(0, min(start, chars.count))
    let clampedEnd = max(clampedStart, min(end, chars.count))
    var i = clampedStart
    while i < clampedEnd {
      if duplicateSeparator { break }
      let c = chars[i]
      if withInputSeparator && config.isInputSeparator(c) {
        count += ensureSeparator()
      } else if !withInputSeparator && config.isDecimalSeparator(c) {
        count += ensureSeparator()
      } else if config.isDigit(c) {
        count += appendDigit(c)
      }
      i += 1
    }
    return count
  }

  private func ensureSeparator() -> Int {
    if separatorFound {
      duplicateSeparator = true
      return 0
    }
    separatorFound = true
    return 1
  }

  private func appendDigit(_ c: Character) -> Int {
    if separatorFound {
      if fraction.count < config.maximumFractionDigits {
        fraction.append(c)
        return 1
      }
    } else {
      if notation.count < config.maximumNotationDigits {
        if notation.count == 1 && config.isZero(notation.first!) {
          if !config.isZero(c) {
            notation.removeFirst()
            notation.append(c)
          }
        } else {
          notation.append(c)
          return 1
        }
      }
    }
    return 0
  }

  private func buildResult() -> String {
    appendNotation()
    appendFraction()
    appendCurrency()
    return resultBuffer
  }

  private func appendNotation() {
    if !notation.isEmpty {
      resultBuffer.append(notation)
      if withGroupingSeparators {
        let groupLength = config.groupingSize
        if groupLength > 0 {
          let notationLength = notation.count
          if notationLength > groupLength {
            var offset = notationLength - groupLength
            while offset >= 1 {
              let insertIndex = resultBuffer.index(resultBuffer.startIndex, offsetBy: offset)
              resultBuffer.insert(config.groupingSeparator, at: insertIndex)
              offset -= groupLength
            }
          }
        }
      }
    } else if withFixedZeroNotation {
      savePosition(&zeroNotationPosition) {
        resultBuffer.append(config.zero)
      }
    }
  }

  private func appendFraction() {
    guard separatorFound else { return }
    guard config.maximumFractionDigits > 0 else { return }

    zeroNotationPosition.clear()

    if resultBuffer.isEmpty { resultBuffer.append(config.zero) }

    resultBuffer.append(config.decimalSeparator)
    resultBuffer.append(fraction)

    if withFixedFractionLength {
      savePosition(&fixedFractionPosition) {
        let pad = config.maximumFractionDigits - fraction.count
        if pad > 0 {
          resultBuffer.append(String(repeating: String(config.zero), count: pad))
        }
      }
    }
  }

  private func appendCurrency() {
    guard withCurrency else { return }

    if !config.prefix.trimmingCharacters(in: .whitespaces).isEmpty {
      resultBuffer = config.prefix + resultBuffer
      fixedFractionPosition.offset(by: config.prefix.count)
      zeroNotationPosition.offset(by: config.prefix.count)
    }

    if !config.suffix.trimmingCharacters(in: .whitespaces).isEmpty {
      savePosition(&currencySuffixPosition) {
        resultBuffer.append(config.suffix)
      }
    }
  }

  private func findSelection(in text: String, selection: Int) -> Int {
    var count = selection
    var index = text.count
      - currencySuffixPosition.length
      - fixedFractionPosition.length
      - zeroNotationPosition.length
    let chars = Array(text)
    while index > 0 && count > 0 {
      let c = chars[index - 1]
      if config.isDecimalSeparator(c) || config.isDigit(c) {
        count -= 1
      }
      index -= 1
    }
    return index
  }

  private func savePosition(_ position: inout FieldRange, _ block: () -> Void) {
    position.beginIndex = resultBuffer.count
    block()
    position.endIndex = resultBuffer.count
  }
}
```

- [ ] **Step 4: Run tests**

```bash
cd camount-swift && swift test --filter AmountFormatterTests && cd ..
```

Expected: all tests in `AmountFormatterTests` pass. If `testFormatInputFlowDecimalSeparator` fails, inspect the output and confirm the expectation matches what the formatter emits given `isInputSeparator("." or ",")` maps to `config.decimalSeparator`.

- [ ] **Step 5: Commit**

```bash
git add camount-swift/Sources/Camount/Core/AmountFormatter.swift \
        camount-swift/Tests/CamountTests/AmountFormatterTests.swift
git commit -m "Add AmountFormatter (format/parse) ported from Kotlin"
```

---

## Task 6: Implement `CurrencyInfo` and make `AmountConfig` derivable from a currency code

**Files:**
- Create: `camount-swift/Sources/Camount/Core/CurrencyInfo.swift`
- Create: `camount-swift/Tests/CamountTests/CurrencyInfoTests.swift`

- [ ] **Step 1: Write the failing test**

`camount-swift/Tests/CamountTests/CurrencyInfoTests.swift`:

```swift
import XCTest
@testable import Camount

final class CurrencyInfoTests: XCTestCase {

  func testUSDHasPrefixDollar() {
    let info = CurrencyInfo.forCurrency("USD")
    XCTAssertEqual(info.maximumFractionDigits, 2)
    XCTAssertFalse(info.prefix.isEmpty, "USD should have a currency prefix (\"$\" or similar)")
  }

  func testJPYHasZeroFractionDigits() {
    let info = CurrencyInfo.forCurrency("JPY")
    XCTAssertEqual(info.maximumFractionDigits, 0)
  }

  func testUnknownCurrencyStillReturns() {
    // Bogus code — NumberFormatter returns defaults, should not crash.
    let info = CurrencyInfo.forCurrency("ZZZ")
    XCTAssertNotNil(info.decimalSeparator)
  }

  func testSanitizeBidiStripsControlChars() {
    let input = "\u{200E}$\u{202A}"
    XCTAssertEqual(CurrencyInfo._sanitizeBidi(input), "$")
  }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
cd camount-swift && swift test --filter CurrencyInfoTests && cd ..
```

Expected: compilation failure.

- [ ] **Step 3: Implement `CurrencyInfo`**

`camount-swift/Sources/Camount/Core/CurrencyInfo.swift`:

```swift
import Foundation

struct CurrencyInfo: Equatable {
  let decimalSeparator: Character
  let groupingSeparator: Character
  let prefix: String
  let suffix: String
  let groupingSize: Int
  let maximumFractionDigits: Int

  static func forCurrency(_ currencyCode: String) -> CurrencyInfo {
    let localeId = NSLocale.localeIdentifier(
      fromComponents: [NSLocale.Key.currencyCode.rawValue: currencyCode]
    )
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.locale = Locale(identifier: localeId)
    formatter.currencyCode = currencyCode

    let decimal: Character = formatter.currencyDecimalSeparator?.first ?? "."
    let grouping: Character = formatter.currencyGroupingSeparator?.first ?? ","

    return CurrencyInfo(
      decimalSeparator: decimal,
      groupingSeparator: grouping,
      prefix: _sanitizeBidi(formatter.positivePrefix ?? ""),
      suffix: _sanitizeBidi(formatter.positiveSuffix ?? ""),
      groupingSize: max(0, formatter.groupingSize),
      maximumFractionDigits: formatter.maximumFractionDigits
    )
  }

  static func _sanitizeBidi(_ s: String) -> String {
    var out = ""
    out.reserveCapacity(s.count)
    for scalar in s.unicodeScalars {
      switch scalar.value {
      case 0x200E, 0x200F,
           0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
           0x2066, 0x2067, 0x2068, 0x2069:
        continue
      default:
        out.unicodeScalars.append(scalar)
      }
    }
    return out
  }
}

extension AmountConfig {
  static func forCurrency(_ currencyCode: String, maximumNotationDigits: Int) -> AmountConfig {
    let info = CurrencyInfo.forCurrency(currencyCode)
    return AmountConfig(
      maximumNotationDigits: maximumNotationDigits,
      decimalSeparator: info.decimalSeparator,
      groupingSeparator: info.groupingSeparator,
      prefix: info.prefix,
      suffix: info.suffix,
      groupingSize: info.groupingSize,
      maximumFractionDigits: info.maximumFractionDigits
    )
  }
}
```

- [ ] **Step 4: Run tests**

```bash
cd camount-swift && swift test --filter CurrencyInfoTests && cd ..
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add camount-swift/Sources/Camount/Core/CurrencyInfo.swift \
        camount-swift/Tests/CamountTests/CurrencyInfoTests.swift
git commit -m "Add CurrencyInfo + AmountConfig.forCurrency factory"
```

---

## Task 7: Define Diff protocol and `EditDiffCalculator`

**Files:**
- Create: `camount-swift/Sources/Camount/Rendering/DiffCalculator.swift`

Port from `camount/src/commonMain/kotlin/io/denison/camount/compose/internal/DiffCalculator.kt`. For now, provide only the protocol, `DiffMode`, and `EditDiffCalculator`. `LevenshteinDiffCalculator` lands in Task 8. Tests for both arrive in Task 9 once we have a minimal `SymbolCell` stub to feed them.

- [ ] **Step 1: Write the file**

`camount-swift/Sources/Camount/Rendering/DiffCalculator.swift`:

```swift
import Foundation

enum DiffMode {
  case edit
  case levenshtein
}

protocol DiffCalculator {
  func diff(
    _ cells: inout [SymbolCell],
    text: String,
    positions: AmountFieldPositions
  )
}

func makeDiffCalculator(
  mode: DiffMode,
  config: AmountConfig,
  newCell: @escaping () -> SymbolCell
) -> DiffCalculator {
  switch mode {
  case .edit: return EditDiffCalculator(config: config, newCell: newCell)
  case .levenshtein: return LevenshteinDiffCalculator(config: config, newCell: newCell)
  }
}

private func fieldAt(_ positions: AmountFieldPositions, index: Int) -> AmountField? {
  if positions.zeroNotation.contains(index) { return .zeroNotation }
  if positions.fixedFraction.contains(index) { return .fixedFraction }
  return nil
}

final class EditDiffCalculator: DiffCalculator {
  private let config: AmountConfig
  private let newCell: () -> SymbolCell

  init(config: AmountConfig, newCell: @escaping () -> SymbolCell) {
    self.config = config
    self.newCell = newCell
  }

  func diff(
    _ cells: inout [SymbolCell],
    text: String,
    positions: AmountFieldPositions
  ) {
    var result: [SymbolCell] = []
    result.reserveCapacity(config.maximumFormattedSymbols)
    let chars = Array(text)
    var textIndex = 0

    var cellIndex = 0
    while cellIndex < cells.count {
      let curCell = cells[cellIndex]
      if curCell.isVisible || curCell.isRunning {
        var retry = true
        while retry {
          let cell: SymbolCell
          if textIndex >= chars.count {
            curCell.delete()
            cell = curCell
            retry = false
          } else {
            let s1 = curCell.currentChar
            let s2 = chars[textIndex]
            let field = fieldAt(positions, index: textIndex)
            if s1 == s2 {
              curCell.replace(char: s2, field: field)
              textIndex += 1
              cell = curCell
              retry = false
            } else if config.isGroupingSeparator(s1) || config.isDecimalSeparator(s1) {
              curCell.delete()
              cell = curCell
              retry = false
            } else if config.isGroupingSeparator(s2) || config.isDecimalSeparator(s2) {
              let fresh = newCell()
              fresh.replace(char: s2, field: field)
              textIndex += 1
              result.append(fresh)
              retry = true
              continue
            } else {
              let s1IsDigit = config.isDigit(s1)
              let s2IsDigit = config.isDigit(s2)
              if s1IsDigit {
                if s2IsDigit {
                  curCell.replace(char: s2, field: field)
                  textIndex += 1
                  cell = curCell
                  retry = false
                } else {
                  curCell.delete()
                  cell = curCell
                  retry = false
                }
              } else {
                if s2IsDigit {
                  let fresh = newCell()
                  fresh.replace(char: s2, field: field)
                  textIndex += 1
                  result.append(fresh)
                  retry = true
                  continue
                } else {
                  curCell.replace(char: s2, field: field)
                  textIndex += 1
                  cell = curCell
                  retry = false
                }
              }
            }
          }
          result.append(cell)
        }
      }
      cellIndex += 1
    }

    let restCount = min(chars.count, config.maximumFormattedSymbols)
    while textIndex < restCount {
      let s = chars[textIndex]
      let field = fieldAt(positions, index: textIndex)
      textIndex += 1
      let cell = newCell()
      cell.replace(char: s, field: field)
      result.append(cell)
    }

    cells = result
  }
}
```

**Note:** `SymbolCell` is referenced but not yet defined. Step 2 adds a minimal stub so the file compiles; the real implementation lands in Task 10.

- [ ] **Step 2: Add a temporary `SymbolCell` stub so the module compiles**

`camount-swift/Sources/Camount/Rendering/SymbolCell.swift`:

```swift
import Foundation

final class SymbolCell {
  private(set) var currentChar: Character = "\0"
  private(set) var isVisible: Bool = false
  var isRunning: Bool { false }

  func replace(char: Character, field: AmountField?) {
    currentChar = char
    isVisible = true
  }

  func delete() {
    isVisible = false
  }
}
```

(This stub will be replaced in Task 10.)

- [ ] **Step 3: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`. If `LevenshteinDiffCalculator` is referenced before definition, comment out its case in `makeDiffCalculator` with `fatalError("not yet implemented")`:

```swift
case .levenshtein: fatalError("LevenshteinDiffCalculator not yet implemented")
```

and remove the reference to `LevenshteinDiffCalculator` from this task. (Task 8 replaces the fatalError.)

- [ ] **Step 4: Commit**

```bash
git add camount-swift/Sources/Camount/Rendering/DiffCalculator.swift \
        camount-swift/Sources/Camount/Rendering/SymbolCell.swift
git commit -m "Add Diff protocol, EditDiffCalculator, SymbolCell stub"
```

---

## Task 8: Implement `LevenshteinDiffCalculator`

**Files:**
- Modify: `camount-swift/Sources/Camount/Rendering/DiffCalculator.swift`

- [ ] **Step 1: Append `LevenshteinDiffCalculator` to `DiffCalculator.swift`**

Add at the bottom of the file:

```swift
final class LevenshteinDiffCalculator: DiffCalculator {
  private let config: AmountConfig
  private let newCell: () -> SymbolCell

  init(config: AmountConfig, newCell: @escaping () -> SymbolCell) {
    self.config = config
    self.newCell = newCell
  }

  func diff(
    _ cells: inout [SymbolCell],
    text: String,
    positions: AmountFieldPositions
  ) {
    // Snapshot the "animating" cells — in Kotlin this is forEachAnimatingWithRetry
    // For read-only AmountText (the only consumer of this calculator) the initial cells
    // are exactly the visible/running set.
    var working = cells
    let chars = Array(text)
    let matrix = levenshteinMatrix(cells: working, text: chars)
    applyDiff(cells: &working, text: chars, matrix: matrix, positions: positions)
    cells = working
  }

  private func levenshteinMatrix(cells: [SymbolCell], text: [Character]) -> [[Int]] {
    let xLength = cells.count
    let yLength = text.count
    var dp = Array(repeating: Array(repeating: 0, count: yLength + 1), count: xLength + 1)
    for i in 0...xLength {
      for j in 0...yLength {
        if i == 0 {
          dp[i][j] = j
        } else if j == 0 {
          dp[i][j] = i
        } else {
          let c1 = cells[i - 1].currentChar
          let c2 = text[j - 1]
          let replaceCost = dp[i - 1][j - 1] + (c1 == c2 ? 0 : 1)
          let insertCost = dp[i][j - 1] + 1
          let deleteCost = dp[i - 1][j] + 1
          dp[i][j] = min(replaceCost, min(insertCost, deleteCost))
        }
      }
    }
    return dp
  }

  private func applyDiff(
    cells: inout [SymbolCell],
    text: [Character],
    matrix: [[Int]],
    positions: AmountFieldPositions
  ) {
    var i = cells.count
    var j = text.count
    while i >= 0 && j >= 0 {
      if i == 0 && j == 0 { break }
      if i == 0 && j > 0 {
        j -= 1
        insert(into: &cells, at: i, char: text[j], field: fieldAt(positions, index: j))
      } else if i > 0 && j == 0 {
        i -= 1
        cells[i].delete()
      } else {
        let replaceCost = matrix[i - 1][j - 1]
        let deleteCost = matrix[i - 1][j]
        let insertCost = matrix[i][j - 1]

        let from = cells[i - 1].currentChar
        let to = text[j - 1]
        let minCost = min(replaceCost, min(insertCost, deleteCost))

        if minCost == replaceCost {
          if isSeparator(from) && !isSeparator(to) {
            i -= 1
            cells[i].delete()
          } else if isSeparator(to) && !isSeparator(from) {
            j -= 1
            insert(into: &cells, at: i, char: to, field: fieldAt(positions, index: j))
          } else {
            i -= 1
            j -= 1
            cells[i].replace(char: to, field: fieldAt(positions, index: j))
          }
        } else if minCost == insertCost {
          j -= 1
          insert(into: &cells, at: i, char: to, field: fieldAt(positions, index: j))
        } else {
          i -= 1
          cells[i].delete()
        }
      }
    }
  }

  private func insert(into cells: inout [SymbolCell], at index: Int, char: Character, field: AmountField?) {
    let cell = newCell()
    cell.replace(char: char, field: field)
    cells.insert(cell, at: index)
  }

  private func isSeparator(_ c: Character) -> Bool {
    config.isDecimalSeparator(c) || config.isGroupingSeparator(c)
  }
}
```

- [ ] **Step 2: Restore the real `makeDiffCalculator`**

If Task 7 Step 3 replaced the `.levenshtein` case with `fatalError`, revert to:

```swift
case .levenshtein: return LevenshteinDiffCalculator(config: config, newCell: newCell)
```

- [ ] **Step 3: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 4: Commit**

```bash
git add camount-swift/Sources/Camount/Rendering/DiffCalculator.swift
git commit -m "Add LevenshteinDiffCalculator"
```

---

## Task 9: Diff calculator tests

**Files:**
- Create: `camount-swift/Tests/CamountTests/DiffCalculatorTests.swift`

These tests rely on `SymbolCell`'s stub behavior from Task 7: `replace` sets `currentChar` + `isVisible = true`; `delete` sets `isVisible = false`. The full animated cell lands in Task 10 and will preserve those two getters.

- [ ] **Step 1: Write tests**

`camount-swift/Tests/CamountTests/DiffCalculatorTests.swift`:

```swift
import XCTest
@testable import Camount

final class DiffCalculatorTests: XCTestCase {

  private let usd = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "$",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  private func newCell() -> () -> SymbolCell { { SymbolCell() } }

  private func visibleChars(_ cells: [SymbolCell]) -> String {
    cells.filter { $0.isVisible }.map { String($0.currentChar) }.joined()
  }

  func testEditInsertsIntoEmpty() {
    var cells: [SymbolCell] = []
    let diff = EditDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$42", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$42")
  }

  func testEditReplacesDigit() {
    var cells: [SymbolCell] = []
    let diff = EditDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$4", positions: .empty)
    diff.diff(&cells, text: "$5", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$5")
  }

  func testEditDeletesTrailing() {
    var cells: [SymbolCell] = []
    let diff = EditDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$42", positions: .empty)
    diff.diff(&cells, text: "$4", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$4")
  }

  func testLevenshteinInsertsFromEmpty() {
    var cells: [SymbolCell] = []
    let diff = LevenshteinDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$1.50", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$1.50")
  }

  func testLevenshteinReplacesInterior() {
    var cells: [SymbolCell] = []
    let diff = LevenshteinDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$1.50", positions: .empty)
    diff.diff(&cells, text: "$2.50", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$2.50")
  }

  func testLevenshteinShrinks() {
    var cells: [SymbolCell] = []
    let diff = LevenshteinDiffCalculator(config: usd, newCell: newCell())
    diff.diff(&cells, text: "$12.34", positions: .empty)
    diff.diff(&cells, text: "$1.23", positions: .empty)
    XCTAssertEqual(visibleChars(cells), "$1.23")
  }
}
```

- [ ] **Step 2: Run tests**

```bash
cd camount-swift && swift test --filter DiffCalculatorTests && cd ..
```

Expected: all pass.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Tests/CamountTests/DiffCalculatorTests.swift
git commit -m "Add DiffCalculator tests (Edit + Levenshtein)"
```

---

## Task 10: Replace the `SymbolCell` stub with the full `CALayer`-backed implementation

This is where rendering starts. From here on, all compiles and unit tests still pass, but visual behavior is only verifiable by running the host app in plan 3.

**Files:**
- Create: `camount-swift/Sources/Camount/Rendering/GlyphCache.swift`
- Create: `camount-swift/Sources/Camount/Rendering/AmountCellLayer.swift`
- Modify: `camount-swift/Sources/Camount/Rendering/SymbolCell.swift`

### Task 10a: `GlyphCache`

- [ ] **Step 1: Write `GlyphCache.swift`**

```swift
import UIKit
import CoreText

final class GlyphCache {
  private struct Key: Hashable {
    let char: Character
    let fontName: String
    let fontSize: CGFloat
    let colorRGBA: UInt64     // packed UInt16 channels for stable hashing
  }

  private var storage: [Key: CGImage] = [:]
  private var screenScale: CGFloat = UIScreen.main.scale

  func setScreenScale(_ scale: CGFloat) {
    if scale != screenScale {
      screenScale = scale
      storage.removeAll(keepingCapacity: true)
    }
  }

  func image(for char: Character, font: UIFont, color: UIColor) -> (CGImage, CGSize)? {
    let key = Key(
      char: char,
      fontName: font.fontName,
      fontSize: font.pointSize,
      colorRGBA: packColor(color)
    )
    if let cached = storage[key], let size = imageSize(cached) {
      return (cached, size)
    }
    guard let (image, size) = render(char: char, font: font, color: color) else { return nil }
    storage[key] = image
    return (image, size)
  }

  // MARK: - Rendering

  private func render(char: Character, font: UIFont, color: UIColor) -> (CGImage, CGSize)? {
    let str = String(char) as NSString
    let attributes: [NSAttributedString.Key: Any] = [
      .font: font,
      .foregroundColor: color
    ]
    let attrStr = NSAttributedString(string: str as String, attributes: attributes)
    let line = CTLineCreateWithAttributedString(attrStr)
    let bounds = CTLineGetBoundsWithOptions(line, .useGlyphPathBounds)

    let ascent = font.ascender
    let descent = -font.descender
    let lineHeight = ascent + descent
    let width = max(1, ceil(bounds.width))
    let height = max(1, ceil(lineHeight))

    let pixelW = Int(width * screenScale)
    let pixelH = Int(height * screenScale)
    let colorSpace = CGColorSpaceCreateDeviceRGB()
    let bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue
    guard let ctx = CGContext(
      data: nil,
      width: pixelW,
      height: pixelH,
      bitsPerComponent: 8,
      bytesPerRow: 0,
      space: colorSpace,
      bitmapInfo: bitmapInfo
    ) else { return nil }
    ctx.scaleBy(x: screenScale, y: screenScale)
    // Core Text uses a flipped coordinate system — origin at bottom.
    ctx.textMatrix = .identity
    ctx.translateBy(x: 0, y: descent)
    CTLineDraw(line, ctx)
    guard let image = ctx.makeImage() else { return nil }
    return (image, CGSize(width: width, height: height))
  }

  private func imageSize(_ image: CGImage) -> CGSize? {
    CGSize(width: CGFloat(image.width) / screenScale,
           height: CGFloat(image.height) / screenScale)
  }

  private func packColor(_ color: UIColor) -> UInt64 {
    var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
    color.getRed(&r, green: &g, blue: &b, alpha: &a)
    func q(_ x: CGFloat) -> UInt64 { UInt64(max(0, min(1, x)) * 65535) }
    return (q(r) << 48) | (q(g) << 32) | (q(b) << 16) | q(a)
  }
}
```

### Task 10b: `AmountCellLayer`

- [ ] **Step 2: Write `AmountCellLayer.swift`**

```swift
import UIKit
import QuartzCore

final class AmountCellLayer: CALayer {

  struct SymbolRender {
    let char: Character
    let image: CGImage
    let intrinsicSize: CGSize
    let baseline: CGFloat
  }

  // Up to 3 stacked symbols. Last entry is the "current" one.
  var symbols: [SymbolRender] = []
  var appearances: [Double] = []       // 0...1 per symbol, animated
  var tintColor: UIColor = .black

  static let animationDuration: CFTimeInterval = 0.120
  static let timingFunction = CAMediaTimingFunction(name: .default)

  @NSManaged var appearance: CGFloat   // not actually used for animation driver — each symbol has its own

  override class func needsDisplay(forKey key: String) -> Bool {
    if key == "appearance" { return true }
    return super.needsDisplay(forKey: key)
  }

  override func action(forKey key: String) -> CAAction? {
    if key == "appearance" {
      let anim = CABasicAnimation(keyPath: key)
      anim.duration = AmountCellLayer.animationDuration
      anim.timingFunction = AmountCellLayer.timingFunction
      if let presented = presentation() {
        anim.fromValue = presented.appearance
      }
      return anim
    }
    return super.action(forKey: key)
  }

  override init() {
    super.init()
    contentsScale = UIScreen.main.scale
    needsDisplayOnBoundsChange = true
  }

  override init(layer: Any) {
    super.init(layer: layer)
    if let source = layer as? AmountCellLayer {
      symbols = source.symbols
      appearances = source.appearances
      tintColor = source.tintColor
    }
  }

  required init?(coder: NSCoder) { fatalError() }

  override func draw(in ctx: CGContext) {
    guard !symbols.isEmpty else { return }
    let size = bounds.size
    guard size.width > 0, size.height > 0 else { return }

    // Core Graphics coords: flip so we can draw top-down.
    ctx.saveGState()
    ctx.translateBy(x: 0, y: size.height)
    ctx.scaleBy(x: 1, y: -1)

    // Normalize intrinsic size of current glyph vs. layer bounds.
    let last = symbols.last!
    let scaleX = last.intrinsicSize.width > 0 ? size.width / last.intrinsicSize.width : 1
    let scaleY = last.intrinsicSize.height > 0 ? size.height / last.intrinsicSize.height : 1

    ctx.scaleBy(x: scaleX, y: scaleY)

    for (i, sym) in symbols.enumerated() {
      let level = CGFloat(appearances[i])
      if level <= 0 { continue }
      let selfScale = 0.6 + (1.0 - 0.6) * level
      ctx.saveGState()
      let pivotX = sym.intrinsicSize.width * 0.5
      let pivotY = sym.baseline
      ctx.translateBy(x: pivotX, y: pivotY)
      ctx.scaleBy(x: selfScale, y: selfScale)
      ctx.translateBy(x: -pivotX, y: -pivotY)
      ctx.setAlpha(level)
      ctx.draw(sym.image, in: CGRect(origin: .zero, size: sym.intrinsicSize))
      ctx.restoreGState()
    }

    ctx.restoreGState()
  }
}
```

**Note on `appearance`:** We track per-symbol `appearances` in an array and drive redraws via `setNeedsDisplay()` directly, because per-symbol custom properties on a single layer aren't cleanly supported by Core Animation. Instead, each `SymbolCell` adds a `CABasicAnimation` on its cell layer's `appearance` key and, per frame tick, the cell layer reads its own `presentation()?.appearance` to compute a shared progress. For a fade-in of a single new symbol this works; for overlapping old+new fades, we animate both in the same pass by having SymbolCell update `appearances[i]` at animation start/end and relying on `setNeedsDisplay` + the layer's built-in display cycle. This keeps the layer API simple. If visible glitches appear during rapid changes, Task 10c covers a `CADisplayLink`-based fallback.

Actually — simpler and correct: per-symbol `CABasicAnimation`s driving per-symbol opacity via two-layer sandwich **does** work but complicates layering. For v1, implement with a single `appearance` animatable property per cell, tracking the most recent transition (fade new in, fade all prior out to the same progress). This matches Kotlin's coroutine setup where every symbol's `Animatable(0f)` runs independently — but in Swift we consolidate to one timer per cell and derive per-symbol alpha from a single progress value + which-symbol-is-target bookkeeping. Keep this design for now; revisit if rapid +1 shows glitches in sample testing (plan 3).

### Task 10c: `SymbolCell`

- [ ] **Step 3: Replace `SymbolCell.swift` with the full implementation**

Overwrite `camount-swift/Sources/Camount/Rendering/SymbolCell.swift`:

```swift
import UIKit
import QuartzCore

final class SymbolCell {

  static let maxStackSymbols = 3
  static let animationDurationMs = 120

  private(set) var currentChar: Character = "\0"
  private(set) var isVisible: Bool = false
  var isRunning: Bool {
    layer.animationKeys()?.isEmpty == false
  }
  var intrinsicWidth: CGFloat { layer.symbols.last?.intrinsicSize.width ?? 0 }
  var intrinsicHeight: CGFloat { layer.symbols.last?.intrinsicSize.height ?? 0 }
  var field: AmountField?

  let layer = AmountCellLayer()

  private let glyphCache: GlyphCache
  private var style: AmountStyle
  private var duration: TimeInterval

  init(glyphCache: GlyphCache, style: AmountStyle, durationMs: Int = SymbolCell.animationDurationMs) {
    self.glyphCache = glyphCache
    self.style = style
    self.duration = TimeInterval(durationMs) / 1000
    layer.tintColor = style.color
  }

  func setDurationMs(_ ms: Int) {
    duration = TimeInterval(ms) / 1000
  }

  func replace(char: Character, field: AmountField?) {
    let effectiveStyle = style.effectiveTextStyle(for: field)
    if let last = layer.symbols.last, last.char == char && self.field == field {
      // Same symbol re-shown — bump to visible.
      layer.appearances[layer.appearances.count - 1] = 1.0
      layer.setNeedsDisplay()
    } else {
      self.field = field
      guard let (image, size) = glyphCache.image(
        for: char,
        font: effectiveStyle.font,
        color: effectiveStyle.color
      ) else { return }
      let baseline = effectiveStyle.font.ascender
      let render = AmountCellLayer.SymbolRender(
        char: char, image: image, intrinsicSize: size, baseline: baseline
      )
      layer.symbols.append(render)
      layer.appearances.append(0)
      while layer.symbols.count > SymbolCell.maxStackSymbols {
        layer.symbols.removeFirst()
        layer.appearances.removeFirst()
      }
      // Fade out all prior; fade in the new one.
      for i in 0..<layer.appearances.count - 1 {
        animateAppearance(index: i, to: 0)
      }
      animateAppearance(index: layer.appearances.count - 1, to: 1)
    }
    currentChar = char
    isVisible = true
  }

  func delete() {
    if let lastIndex = layer.appearances.indices.last {
      animateAppearance(index: lastIndex, to: 0)
    }
    isVisible = false
  }

  func setTargetBounds(left: CGFloat, top: CGFloat, width: CGFloat, height: CGFloat) {
    let newFrame = CGRect(x: left, y: top, width: width, height: height)
    let isInitial = layer.bounds.size == .zero
    if isInitial {
      CATransaction.begin()
      CATransaction.setDisableActions(true)
      layer.frame = newFrame
      CATransaction.commit()
    } else {
      let anim = CABasicAnimation(keyPath: "position")
      anim.duration = duration
      anim.timingFunction = AmountCellLayer.timingFunction
      anim.fromValue = layer.presentation()?.position ?? layer.position
      layer.add(anim, forKey: "position")
      let sizeAnim = CABasicAnimation(keyPath: "bounds.size")
      sizeAnim.duration = duration
      sizeAnim.timingFunction = AmountCellLayer.timingFunction
      sizeAnim.fromValue = layer.presentation()?.bounds.size ?? layer.bounds.size
      layer.add(sizeAnim, forKey: "bounds.size")
      layer.frame = newFrame
    }
  }

  func updateStyle(_ style: AmountStyle) {
    self.style = style
    layer.tintColor = style.color
    layer.setNeedsDisplay()
  }

  // MARK: - Internals

  private func animateAppearance(index: Int, to target: Double) {
    layer.appearances[index] = target
    let displayLink = perCellDisplayLink
    if displayLink == nil {
      perCellDisplayLink = CADisplayLink(target: AnimationTicker(cell: self), selector: #selector(AnimationTicker.tick))
      perCellDisplayLink?.add(to: .main, forMode: .common)
    }
    layer.setNeedsDisplay()
    // Let the display tick for `duration` seconds before stopping.
    let deadline = DispatchTime.now() + .milliseconds(Int(duration * 1000) + 16)
    DispatchQueue.main.asyncAfter(deadline: deadline) { [weak self] in
      self?.maybeStopDisplayLink()
    }
  }

  private var perCellDisplayLink: CADisplayLink?

  private func maybeStopDisplayLink() {
    // Stop ticking if all appearances have reached their targets (no motion).
    // For simplicity (and since we don't interpolate continuously here), stop unconditionally —
    // animations on position/bounds are handled by CABasicAnimation on the layer and don't need this.
    perCellDisplayLink?.invalidate()
    perCellDisplayLink = nil
  }
}

private final class AnimationTicker: NSObject {
  weak var cell: SymbolCell?
  init(cell: SymbolCell) { self.cell = cell }
  @objc func tick() {
    cell?.layer.setNeedsDisplay()
  }
}
```

**Decision point:** Per the design doc, the appearance animation is meant to be driven by Core Animation's render server. The implementation above is a pragmatic compromise: we drive repaint via a `CADisplayLink` (main-thread tick) because animating a per-symbol stack via a single animatable property on `CALayer` runs into API friction. `position` and `bounds.size` still animate via `CABasicAnimation` off-thread — those are the large-motion cases, which matter most for perceived smoothness.

If profiling in the sample app (plan 3) shows this is too expensive on old hardware, refactor to one `CALayer` per stacked symbol with independent `opacity` + transform animations running entirely on the render server. Track this as a v1.1 follow-up item in `PARITY.md`.

- [ ] **Step 4: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 5: Re-run all tests (the cell change should not regress formatter/diff tests)**

```bash
cd camount-swift && swift test && cd ..
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add camount-swift/Sources/Camount/Rendering/GlyphCache.swift \
        camount-swift/Sources/Camount/Rendering/AmountCellLayer.swift \
        camount-swift/Sources/Camount/Rendering/SymbolCell.swift
git commit -m "Add GlyphCache, AmountCellLayer, full SymbolCell"
```

---

## Task 11: Define `AmountStyle`, `CursorStyle`, and enums

**Files:**
- Create: `camount-swift/Sources/Camount/SwiftUI/AmountStyle.swift`
- Create: `camount-swift/Sources/Camount/SwiftUI/Enums.swift`

- [ ] **Step 1: Write `AmountStyle.swift`**

```swift
import UIKit
import SwiftUI

public struct AmountStyle {
  public let font: UIFont
  public let color: UIColor
  public let gradient: Gradient?
  public let cursor: CursorStyle?
  public let zeroNotationColor: UIColor?
  public let fixedFractionColor: UIColor?

  public init(
    font: UIFont,
    color: UIColor = .label,
    gradient: Gradient? = nil,
    cursor: CursorStyle? = nil,
    zeroNotationColor: UIColor? = nil,
    fixedFractionColor: UIColor? = nil
  ) {
    self.font = font
    self.color = color
    self.gradient = gradient
    self.cursor = cursor
    self.zeroNotationColor = zeroNotationColor
    self.fixedFractionColor = fixedFractionColor
  }

  public static let `default` = AmountStyle(font: .systemFont(ofSize: 20))

  struct Effective {
    let font: UIFont
    let color: UIColor
  }

  func effectiveTextStyle(for field: AmountField?) -> Effective {
    switch field {
    case .zeroNotation:
      return Effective(font: font, color: zeroNotationColor ?? color)
    case .fixedFraction:
      return Effective(font: font, color: fixedFractionColor ?? color)
    case .currencySuffix, .none:
      return Effective(font: font, color: color)
    }
  }
}

public struct CursorStyle {
  public let color: UIColor
  public let width: CGFloat        // points
  public let heightFraction: CGFloat

  public init(color: UIColor, width: CGFloat = 2, heightFraction: CGFloat = 1.0) {
    precondition((0.0...1.0).contains(heightFraction), "heightFraction must be in 0...1")
    self.color = color
    self.width = width
    self.heightFraction = heightFraction
  }
}
```

- [ ] **Step 2: Write `Enums.swift`**

```swift
import Foundation

public enum ShowSign: Sendable { case ifNegative, always }
public enum FractionPolicy: Sendable { case fixed, ignoreZero }
public enum AmountAlignment: Sendable { case start, center, end }
```

- [ ] **Step 3: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 4: Commit**

```bash
git add camount-swift/Sources/Camount/SwiftUI/AmountStyle.swift \
        camount-swift/Sources/Camount/SwiftUI/Enums.swift
git commit -m "Add AmountStyle, CursorStyle, ShowSign, FractionPolicy, AmountAlignment"
```

---

## Task 12: Implement `CursorCell`

**Files:**
- Create: `camount-swift/Sources/Camount/Rendering/CursorCell.swift`

- [ ] **Step 1: Write the file**

```swift
import UIKit
import QuartzCore

final class CursorCell {

  static let blinkDurationMs = 530
  static let appearDurationMs = 500

  let layer = CALayer()
  private var style: CursorStyle
  private var durationMs: Int = SymbolCell.animationDurationMs
  private var blinkTimer: DispatchSourceTimer?
  private var cursorVisible = false

  init(style: CursorStyle) {
    self.style = style
    layer.backgroundColor = style.color.cgColor
    layer.cornerRadius = style.width / 2
    layer.opacity = 0
  }

  func setDurationMs(_ value: Int) {
    durationMs = value
  }

  func setTargetBounds(left: CGFloat, top: CGFloat, width: CGFloat, height: CGFloat) {
    let newFrame = CGRect(x: left, y: top, width: width, height: height)
    let isInitial = layer.bounds.size == .zero
    if isInitial {
      CATransaction.begin()
      CATransaction.setDisableActions(true)
      layer.frame = newFrame
      layer.cornerRadius = style.width / 2
      CATransaction.commit()
    } else {
      animate("position", from: layer.presentation()?.position ?? layer.position)
      animate("bounds.size", from: layer.presentation()?.bounds.size ?? layer.bounds.size)
      layer.frame = newFrame
    }
  }

  func setVisible(_ visible: Bool) {
    guard cursorVisible != visible else { return }
    cursorVisible = visible
    blinkTimer?.cancel()
    blinkTimer = nil

    if visible {
      animateOpacity(to: 1, duration: Self.appearDurationMs)
      let timer = DispatchSource.makeTimerSource(queue: .main)
      var on = true
      timer.schedule(
        deadline: .now() + .milliseconds(Self.blinkDurationMs),
        repeating: .milliseconds(Self.blinkDurationMs)
      )
      timer.setEventHandler { [weak self] in
        guard let self = self else { return }
        on.toggle()
        self.animateOpacity(to: on ? 1 : 0, duration: Self.appearDurationMs)
      }
      timer.resume()
      blinkTimer = timer
    } else {
      animateOpacity(to: 0, duration: Self.appearDurationMs)
    }
  }

  private func animateOpacity(to target: Float, duration ms: Int) {
    let anim = CABasicAnimation(keyPath: "opacity")
    anim.duration = TimeInterval(ms) / 1000
    anim.timingFunction = AmountCellLayer.timingFunction
    anim.fromValue = layer.presentation()?.opacity ?? layer.opacity
    anim.toValue = target
    layer.opacity = target
    layer.add(anim, forKey: "opacity")
  }

  private func animate(_ key: String, from value: Any?) {
    let anim = CABasicAnimation(keyPath: key)
    anim.duration = TimeInterval(durationMs) / 1000
    anim.timingFunction = AmountCellLayer.timingFunction
    anim.fromValue = value
    layer.add(anim, forKey: key)
  }

  deinit {
    blinkTimer?.cancel()
  }
}
```

- [ ] **Step 2: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Sources/Camount/Rendering/CursorCell.swift
git commit -m "Add CursorCell with blink loop"
```

---

## Task 13: Implement `AmountPainter`

**Files:**
- Create: `camount-swift/Sources/Camount/Rendering/AmountPainter.swift`

- [ ] **Step 1: Write the file**

```swift
import UIKit
import QuartzCore

final class AmountPainter {

  let rootLayer = CALayer()
  private var cells: [SymbolCell] = []
  private var cursor: CursorCell?

  private var style: AmountStyle
  private var config: AmountConfig
  private var mode: DiffMode
  private var alignment: AmountAlignment = .center

  private var containerSize: CGSize = .zero
  private var densityScale: CGFloat = 1
  private var lastRenderedText: String?
  private var cursorPositionIndex: Int = -1

  private let glyphCache = GlyphCache()
  private lazy var diffCalculator: DiffCalculator = {
    makeDiffCalculator(mode: mode, config: config, newCell: { [unowned self] in self.newCell() })
  }()

  var intrinsicWidth: CGFloat = 0
  var intrinsicHeight: CGFloat = 0

  init(style: AmountStyle, mode: DiffMode, config: AmountConfig) {
    self.style = style
    self.config = config
    self.mode = mode
    rootLayer.masksToBounds = false
    if let cs = style.cursor {
      cursor = CursorCell(style: cs)
      rootLayer.addSublayer(cursor!.layer)
    }
  }

  // MARK: - Inputs

  func updateStyle(style: AmountStyle, config: AmountConfig, alignment: AmountAlignment) {
    let configChanged = config != self.config
    let styleChanged = !isEqual(self.style, style)
    let alignmentChanged = alignment != self.alignment
    let cursorChanged = !isEqual(self.style.cursor, style.cursor)

    self.style = style
    self.alignment = alignment
    if configChanged { self.config = config }
    if cursorChanged {
      cursor?.layer.removeFromSuperlayer()
      cursor = style.cursor.map { CursorCell(style: $0) }
      if let c = cursor { rootLayer.addSublayer(c.layer) }
    }
    if configChanged || styleChanged {
      diffCalculator = makeDiffCalculator(
        mode: mode, config: self.config,
        newCell: { [unowned self] in self.newCell() }
      )
    }
    // Propagate to existing cells.
    for cell in cells { cell.updateStyle(style) }
    if alignmentChanged { layout() }
  }

  func setText(_ text: String, positions: AmountFieldPositions) {
    if lastRenderedText == text { return }
    lastRenderedText = text
    diffCalculator.diff(&cells, text: text, positions: positions)
    // Ensure every cell's layer is parented on the root, in order.
    syncLayerTree()
    cursorPositionIndex = positions.cursorPosition
    calculateIntrinsic()
    layout()
  }

  func setBounds(width: CGFloat, height: CGFloat) {
    let newSize = CGSize(width: width, height: height)
    guard containerSize != newSize else { return }
    containerSize = newSize
    layout()
  }

  func setCursorVisible(_ visible: Bool) {
    cursor?.setVisible(visible)
  }

  func setDensity(_ density: CGFloat) {
    guard density != densityScale else { return }
    densityScale = density
    glyphCache.setScreenScale(density)
    // Force re-rasterization of cells by clearing their cached images indirectly.
    for cell in cells { cell.updateStyle(style) }
    layout()
  }

  // MARK: - Internals

  private func newCell() -> SymbolCell {
    let cell = SymbolCell(glyphCache: glyphCache, style: style, durationMs: SymbolCell.animationDurationMs)
    rootLayer.addSublayer(cell.layer)
    return cell
  }

  private func syncLayerTree() {
    // Ensure cell layers are attached and in order; detached cells (delete with invisible) keep their layer for fade-out.
    for cell in cells where cell.layer.superlayer !== rootLayer {
      rootLayer.addSublayer(cell.layer)
    }
    if let cursorLayer = cursor?.layer {
      cursorLayer.removeFromSuperlayer()
      rootLayer.addSublayer(cursorLayer)      // cursor always on top
    }
  }

  private func calculateIntrinsic() {
    var w: CGFloat = 0
    var h: CGFloat = 0
    for cell in cells where cell.isVisible {
      w += cell.intrinsicWidth
      h = max(h, cell.intrinsicHeight)
    }
    intrinsicWidth = w
    intrinsicHeight = h
  }

  private func layout() {
    guard containerSize.width > 0, containerSize.height > 0 else { return }

    var visibleWidth: CGFloat = 0
    var visibleHeight: CGFloat = 0
    for cell in cells where cell.isVisible {
      visibleWidth += cell.intrinsicWidth
      visibleHeight = max(visibleHeight, cell.intrinsicHeight)
    }
    let cursorStyle = style.cursor
    if let cs = cursorStyle {
      let cursorW = cs.width
      let cursorH = visibleHeight * cs.heightFraction
      visibleWidth += cursorW
      visibleHeight = max(visibleHeight, cursorH)
    }

    let scale: CGFloat = (containerSize.width < visibleWidth && visibleWidth > 0)
      ? containerSize.width / visibleWidth
      : 1
    let scaledWidth = visibleWidth * scale
    let scaledHeight = visibleHeight * scale

    let top = (containerSize.height - scaledHeight) / 2
    var left: CGFloat = {
      switch alignment {
      case .start:  return 0
      case .center: return (containerSize.width - scaledWidth) / 2
      case .end:    return containerSize.width - scaledWidth
      }
    }()

    var cursorLeft = left
    var visibleIndex = 0

    for cell in cells where cell.isVisible {
      let w = cell.intrinsicWidth * scale
      let h = cell.intrinsicHeight * scale
      cell.setTargetBounds(left: left, top: top, width: w, height: h)
      left += w
      visibleIndex += 1
      if visibleIndex == cursorPositionIndex { cursorLeft = left }
    }

    if let c = cursor, let cs = cursorStyle {
      let cursorW = cs.width * scale
      let cursorH = visibleHeight * cs.heightFraction * scale
      let cursorTop = top + (scaledHeight - cursorH) / 2
      c.setTargetBounds(left: cursorLeft, top: cursorTop, width: cursorW, height: cursorH)
    }
  }

  private func isEqual(_ a: AmountStyle, _ b: AmountStyle) -> Bool {
    a.font == b.font && a.color == b.color &&
    isEqual(a.cursor, b.cursor) &&
    a.zeroNotationColor == b.zeroNotationColor &&
    a.fixedFractionColor == b.fixedFractionColor
    // gradient: compared by reference identity fallback — Gradient is not Equatable.
    && (a.gradient == nil) == (b.gradient == nil)
  }

  private func isEqual(_ a: CursorStyle?, _ b: CursorStyle?) -> Bool {
    switch (a, b) {
    case (nil, nil): return true
    case let (x?, y?): return x.color == y.color && x.width == y.width && x.heightFraction == y.heightFraction
    default: return false
    }
  }
}
```

- [ ] **Step 2: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Sources/Camount/Rendering/AmountPainter.swift
git commit -m "Add AmountPainter orchestrating cells and cursor"
```

---

## Task 14: `AmountHostView` (read-only, no text field yet)

**Files:**
- Create: `camount-swift/Sources/Camount/Rendering/AmountHostView.swift`

- [ ] **Step 1: Write the file**

```swift
import UIKit

final class AmountHostView: UIView {

  private let painter: AmountPainter

  var onMoneyChange: ((Money) -> Void)?   // populated by AmountField; nil for AmountText

  init(style: AmountStyle, mode: DiffMode, config: AmountConfig) {
    self.painter = AmountPainter(style: style, mode: mode, config: config)
    super.init(frame: .zero)
    layer.addSublayer(painter.rootLayer)
    painter.setDensity(traitCollection.displayScale)
  }

  required init?(coder: NSCoder) { fatalError() }

  override func layoutSubviews() {
    super.layoutSubviews()
    painter.rootLayer.frame = bounds
    painter.setBounds(width: bounds.width, height: bounds.height)
  }

  override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
    super.traitCollectionDidChange(previousTraitCollection)
    painter.setDensity(traitCollection.displayScale)
  }

  // MARK: - API consumed by UIViewRepresentable bridges

  func configure(
    style: AmountStyle,
    config: AmountConfig,
    alignment: AmountAlignment
  ) {
    painter.updateStyle(style: style, config: config, alignment: alignment)
  }

  func setText(_ text: String, positions: AmountFieldPositions = .empty) {
    painter.setText(text, positions: positions)
  }

  func setCursorVisible(_ visible: Bool) {
    painter.setCursorVisible(visible)
  }
}
```

- [ ] **Step 2: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Sources/Camount/Rendering/AmountHostView.swift
git commit -m "Add AmountHostView hosting the CALayer tree"
```

---

## Task 15: Environment keys + view modifiers

**Files:**
- Create: `camount-swift/Sources/Camount/SwiftUI/EnvironmentKeys.swift`

- [ ] **Step 1: Write the file**

```swift
import SwiftUI

private struct AmountStyleKey: EnvironmentKey {
  static let defaultValue: AmountStyle = .default
}

private struct ShowSignKey: EnvironmentKey {
  static let defaultValue: ShowSign = .ifNegative
}

private struct FractionPolicyKey: EnvironmentKey {
  static let defaultValue: FractionPolicy = .fixed
}

private struct MaximumNotationDigitsKey: EnvironmentKey {
  static let defaultValue: Int = 5
}

private struct AmountAlignmentKey: EnvironmentKey {
  static let defaultValue: AmountAlignment = .center
}

extension EnvironmentValues {
  var amountStyle: AmountStyle {
    get { self[AmountStyleKey.self] }
    set { self[AmountStyleKey.self] = newValue }
  }
  var amountShowSign: ShowSign {
    get { self[ShowSignKey.self] }
    set { self[ShowSignKey.self] = newValue }
  }
  var amountFractionPolicy: FractionPolicy {
    get { self[FractionPolicyKey.self] }
    set { self[FractionPolicyKey.self] = newValue }
  }
  var amountMaximumNotationDigits: Int {
    get { self[MaximumNotationDigitsKey.self] }
    set { self[MaximumNotationDigitsKey.self] = newValue }
  }
  var amountAlignment: AmountAlignment {
    get { self[AmountAlignmentKey.self] }
    set { self[AmountAlignmentKey.self] = newValue }
  }
}

public extension View {
  func amountStyle(_ style: AmountStyle) -> some View { environment(\.amountStyle, style) }
  func showSign(_ value: ShowSign) -> some View { environment(\.amountShowSign, value) }
  func fractionPolicy(_ value: FractionPolicy) -> some View { environment(\.amountFractionPolicy, value) }
  func maximumNotationDigits(_ value: Int) -> some View { environment(\.amountMaximumNotationDigits, value) }
  func amountAlignment(_ value: AmountAlignment) -> some View { environment(\.amountAlignment, value) }
}
```

- [ ] **Step 2: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Sources/Camount/SwiftUI/EnvironmentKeys.swift
git commit -m "Add EnvironmentValues extensions and modifier functions"
```

---

## Task 16: `AmountText` SwiftUI view

**Files:**
- Create: `camount-swift/Sources/Camount/SwiftUI/AmountText.swift`

- [ ] **Step 1: Write the file**

```swift
import SwiftUI

public struct AmountText: View {
  private let amount: Money

  public init(_ amount: Money) { self.amount = amount }

  public var body: some View {
    Representable(
      amount: amount
    )
  }

  fileprivate struct Representable: UIViewRepresentable {
    let amount: Money
    @Environment(\.amountStyle) var style
    @Environment(\.amountShowSign) var showSign
    @Environment(\.amountFractionPolicy) var fractionPolicy
    @Environment(\.amountMaximumNotationDigits) var maxNotationDigits
    @Environment(\.amountAlignment) var alignment

    func makeUIView(context: Context) -> AmountHostView {
      let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
      let view = AmountHostView(style: style, mode: .levenshtein, config: config)
      view.configure(style: style, config: config, alignment: alignment)
      return view
    }

    func updateUIView(_ view: AmountHostView, context: Context) {
      let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
      view.configure(style: style, config: config, alignment: alignment)
      let formatter = AmountFormatter(
        config: config,
        withCurrency: true,
        withGroupingSeparators: true,
        withFixedFractionLength: fractionPolicy == .fixed,
        withFixedZeroNotation: true
      )
      let base = formatter.format(amount)
      let rendered: String
      if amount.isZero {
        rendered = base
      } else if !amount.isPositive {
        rendered = "-" + base
      } else if showSign == .always {
        rendered = "+" + base
      } else {
        rendered = base
      }
      view.setText(rendered, positions: .empty)
    }
  }
}
```

- [ ] **Step 2: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Sources/Camount/SwiftUI/AmountText.swift
git commit -m "Add AmountText SwiftUI view"
```

---

## Task 17: `sanitizeInput` helper

**Files:**
- Create: `camount-swift/Sources/Camount/Core/SanitizeInput.swift`
- Create: `camount-swift/Tests/CamountTests/SanitizeInputTests.swift`

- [ ] **Step 1: Write the failing tests**

`camount-swift/Tests/CamountTests/SanitizeInputTests.swift`:

```swift
import XCTest
@testable import Camount

final class SanitizeInputTests: XCTestCase {

  private let usd = AmountConfig(
    maximumNotationDigits: 5,
    decimalSeparator: ".",
    groupingSeparator: ",",
    prefix: "",
    suffix: "",
    groupingSize: 3,
    maximumFractionDigits: 2
  )

  func testStripsLetters() {
    let result = sanitizeInput(text: "1a2b3", cursor: 5, config: usd)
    XCTAssertEqual(result.text, "123")
    XCTAssertEqual(result.cursor, 3)
  }

  func testEnforcesMaxNotation() {
    let result = sanitizeInput(text: "123456", cursor: 6, config: usd)
    XCTAssertEqual(result.text, "12345")
    XCTAssertEqual(result.cursor, 5)
  }

  func testEnforcesMaxFraction() {
    let result = sanitizeInput(text: "1.234", cursor: 5, config: usd)
    XCTAssertEqual(result.text, "1.23")
    XCTAssertEqual(result.cursor, 4)
  }

  func testOnlyOneSeparator() {
    let result = sanitizeInput(text: "1.2.3", cursor: 5, config: usd)
    XCTAssertEqual(result.text, "1.23")
    XCTAssertEqual(result.cursor, 4)
  }

  func testCommaMappedToDecimalSeparator() {
    let result = sanitizeInput(text: "1,2", cursor: 3, config: usd)
    XCTAssertEqual(result.text, "1.2")
  }
}
```

- [ ] **Step 2: Run — verify compilation failure**

```bash
cd camount-swift && swift test --filter SanitizeInputTests && cd ..
```

Expected: compilation failure — `sanitizeInput` not defined.

- [ ] **Step 3: Implement `SanitizeInput.swift`**

```swift
import Foundation

struct SanitizedInput {
  let text: String
  let cursor: Int
}

func sanitizeInput(text: String, cursor: Int, config: AmountConfig) -> SanitizedInput {
  let src = Array(text)
  var builder = ""
  var separatorSeen = false
  var integerDigits = 0
  var fractionDigits = 0
  let originalCursor = max(0, min(cursor, src.count))
  var mappedCursor = 0

  for i in 0..<src.count {
    let c = src[i]
    var kept = false
    if config.isDigit(c) {
      let underLimit = separatorSeen
        ? fractionDigits < config.maximumFractionDigits
        : integerDigits < config.maximumNotationDigits
      if underLimit {
        builder.append(c)
        if separatorSeen { fractionDigits += 1 } else { integerDigits += 1 }
        kept = true
      }
    } else if config.isInputSeparator(c) && !separatorSeen && config.maximumFractionDigits > 0 {
      separatorSeen = true
      builder.append(config.decimalSeparator)
      kept = true
    }
    if kept && i < originalCursor { mappedCursor += 1 }
  }

  return SanitizedInput(text: builder, cursor: min(mappedCursor, builder.count))
}
```

- [ ] **Step 4: Run tests**

```bash
cd camount-swift && swift test --filter SanitizeInputTests && cd ..
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add camount-swift/Sources/Camount/Core/SanitizeInput.swift \
        camount-swift/Tests/CamountTests/SanitizeInputTests.swift
git commit -m "Add sanitizeInput helper for AmountField input flow"
```

---

## Task 18: Extend `AmountHostView` with editable mode (hidden UITextField)

**Files:**
- Modify: `camount-swift/Sources/Camount/Rendering/AmountHostView.swift`

- [ ] **Step 1: Update `AmountHostView`**

Replace the contents of `camount-swift/Sources/Camount/Rendering/AmountHostView.swift` with:

```swift
import UIKit

final class AmountHostView: UIView, UITextFieldDelegate {

  private let painter: AmountPainter
  private var currentConfig: AmountConfig
  private var currentCurrencyCode: String = ""

  // Formatters — lazily recreated when config changes.
  private var inputFormatter: AmountFormatter
  private var displayFormatter: AmountFormatter

  private let editable: Bool
  private let hiddenField: UITextField?

  var onMoneyChange: ((Money) -> Void)?
  private var lastParsedMoney: Money?

  init(style: AmountStyle, mode: DiffMode, config: AmountConfig, editable: Bool) {
    self.painter = AmountPainter(style: style, mode: mode, config: config)
    self.currentConfig = config
    self.inputFormatter = Self.makeInputFormatter(config: config)
    self.displayFormatter = Self.makeDisplayFormatter(config: config)
    self.editable = editable
    self.hiddenField = editable ? UITextField() : nil
    super.init(frame: .zero)
    layer.addSublayer(painter.rootLayer)
    painter.setDensity(traitCollection.displayScale)
    if let f = hiddenField {
      f.alpha = 0.01
      f.tintColor = .clear
      f.keyboardType = .decimalPad
      f.textContentType = nil
      f.autocorrectionType = .no
      f.spellCheckingType = .no
      f.delegate = self
      f.addTarget(self, action: #selector(editingBegan), for: .editingDidBegin)
      f.addTarget(self, action: #selector(editingEnded), for: .editingDidEnd)
      addSubview(f)
    }
  }

  required init?(coder: NSCoder) { fatalError() }

  override func layoutSubviews() {
    super.layoutSubviews()
    painter.rootLayer.frame = bounds
    painter.setBounds(width: bounds.width, height: bounds.height)
    hiddenField?.frame = bounds
  }

  override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
    super.traitCollectionDidChange(previousTraitCollection)
    painter.setDensity(traitCollection.displayScale)
  }

  // MARK: - Read-only API

  func configure(style: AmountStyle, config: AmountConfig, alignment: AmountAlignment, currencyCode: String) {
    if config != currentConfig {
      currentConfig = config
      inputFormatter = Self.makeInputFormatter(config: config)
      displayFormatter = Self.makeDisplayFormatter(config: config)
    }
    currentCurrencyCode = currencyCode
    painter.updateStyle(style: style, config: config, alignment: alignment)
  }

  func setText(_ text: String, positions: AmountFieldPositions = .empty) {
    painter.setText(text, positions: positions)
  }

  func setCursorVisible(_ visible: Bool) {
    painter.setCursorVisible(visible)
  }

  // MARK: - Editable API

  /// Called by the AmountField bridge when the parent Money changes externally.
  func applyExternalMoney(_ money: Money) {
    guard editable else { return }
    if let last = lastParsedMoney, last == money { return }
    let input = String(inputFormatter.format(money))
    hiddenField?.text = input
    let positions = currentInputPositions(for: input)
    pushFormattedDisplay(forInputText: input, positions: positions)
    lastParsedMoney = money
  }

  // MARK: - UITextFieldDelegate

  func textField(
    _ textField: UITextField,
    shouldChangeCharactersIn range: NSRange,
    replacementString string: String
  ) -> Bool {
    let current = (textField.text ?? "") as NSString
    let proposed = current.replacingCharacters(in: range, with: string)
    let cursor = range.location + string.count
    let sanitized = sanitizeInput(text: proposed, cursor: cursor, config: currentConfig)
    textField.text = sanitized.text
    if let pos = textField.position(from: textField.beginningOfDocument, offset: sanitized.cursor) {
      textField.selectedTextRange = textField.textRange(from: pos, to: pos)
    }
    let positions = currentInputPositions(for: sanitized.text)
    pushFormattedDisplay(forInputText: sanitized.text, positions: positions)
    let parsed = displayFormatter.parse(sanitized.text, currencyCode: currentCurrencyCode)
    if parsed != lastParsedMoney {
      lastParsedMoney = parsed
      onMoneyChange?(parsed)
    }
    return false   // we already applied the change
  }

  @objc private func editingBegan() { painter.setCursorVisible(true) }
  @objc private func editingEnded() { painter.setCursorVisible(false) }

  // MARK: - Helpers

  private func pushFormattedDisplay(forInputText input: String, positions: AmountFieldPositions) {
    let formatted = String(displayFormatter.format(
      source: input,
      start: input.count,
      end: input.count,
      text: input,
      textStart: input.count,
      textEnd: input.count
    ))
    painter.setText(formatted, positions: displayFormatter.fieldPositions())
  }

  private func currentInputPositions(for input: String) -> AmountFieldPositions {
    // Re-run the input formatter to capture field positions for the *visible* render.
    _ = inputFormatter.format(
      source: input,
      start: input.count,
      end: input.count,
      text: input,
      textStart: input.count,
      textEnd: input.count
    )
    return inputFormatter.fieldPositions()
  }

  private static func makeInputFormatter(config: AmountConfig) -> AmountFormatter {
    AmountFormatter(
      config: config,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true
    )
  }

  private static func makeDisplayFormatter(config: AmountConfig) -> AmountFormatter {
    AmountFormatter(config: config)
  }
}
```

- [ ] **Step 2: Update `AmountText.swift`** to pass the new `editable:` parameter and `currencyCode:` to `configure`:

```swift
func makeUIView(context: Context) -> AmountHostView {
  let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
  let view = AmountHostView(style: style, mode: .levenshtein, config: config, editable: false)
  view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
  return view
}

func updateUIView(_ view: AmountHostView, context: Context) {
  let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
  view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
  // ... (existing formatting code unchanged)
}
```

- [ ] **Step 3: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 4: Commit**

```bash
git add camount-swift/Sources/Camount/Rendering/AmountHostView.swift \
        camount-swift/Sources/Camount/SwiftUI/AmountText.swift
git commit -m "Extend AmountHostView with editable mode + hidden UITextField"
```

---

## Task 19: `AmountField` SwiftUI view

**Files:**
- Create: `camount-swift/Sources/Camount/SwiftUI/AmountField.swift`

- [ ] **Step 1: Write the file**

```swift
import SwiftUI

public struct AmountField: View {
  @Binding private var amount: Money

  public init(_ amount: Binding<Money>) { self._amount = amount }

  public var body: some View {
    Representable(amount: $amount)
  }

  fileprivate struct Representable: UIViewRepresentable {
    @Binding var amount: Money
    @Environment(\.amountStyle) var style
    @Environment(\.amountMaximumNotationDigits) var maxNotationDigits
    @Environment(\.amountAlignment) var alignment

    func makeUIView(context: Context) -> AmountHostView {
      let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
      let view = AmountHostView(style: style, mode: .edit, config: config, editable: true)
      view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
      view.onMoneyChange = { [binding = $amount] new in
        if binding.wrappedValue != new { binding.wrappedValue = new }
      }
      view.applyExternalMoney(amount)
      return view
    }

    func updateUIView(_ view: AmountHostView, context: Context) {
      let config = AmountConfig.forCurrency(amount.currencyCode, maximumNotationDigits: maxNotationDigits)
      view.configure(style: style, config: config, alignment: alignment, currencyCode: amount.currencyCode)
      view.applyExternalMoney(amount)
    }
  }
}
```

- [ ] **Step 2: Build**

```bash
cd camount-swift && swift build && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Commit**

```bash
git add camount-swift/Sources/Camount/SwiftUI/AmountField.swift
git commit -m "Add AmountField SwiftUI view"
```

---

## Task 20: Write `PARITY.md`

**Files:**
- Create: `camount-swift/PARITY.md`

- [ ] **Step 1: Write the file**

`camount-swift/PARITY.md`:

```markdown
# Camount Swift Parity Specification

This document is the behavioral contract between the Kotlin (`camount/`) and
Swift (`camount-swift/`) implementations. When they diverge, either the spec
is updated with intent or the implementation is brought back in line.

## Scope

Covered: formatter semantics, diff algorithms, animation timing, cell stack,
cursor blink, keystroke sanitization, field-position semantics.
Not covered by v1: pixel-for-pixel bitmap match, easing-curve coefficients,
Dynamic Type, RTL mirroring.

## Formatter

- `Money` uses `units: Int64`, `nanos: Int32`, `currencyCode: String`. Sign is
  carried on both fields.
- `AmountFormatter.format(Money)` returns the absolute-value rendering. The
  consumer (`AmountText`) prepends `-` / `+` per `ShowSign`.
- Overflow of notation/fraction is truncated, not errored.
- `format(source, start, end, text, textStart, textEnd)` is the input-flow
  entry point used by `AmountField`. Duplicate decimal separator preserves
  the source and bails out.
- `parse` ignores anything that is not a digit, `-`, or input separator.
  Input separators `.` and `,` are both accepted regardless of locale.

## Diff

Two modes:

- **Edit** — consumed by `AmountField`. Keeps existing cells where possible;
  spawns new cells for incoming separators and new digits when the source
  was a non-digit.
- **Levenshtein** — consumed by `AmountText`. Classic edit-distance matrix,
  unwound to produce insert/delete/replace operations.

Separator-awareness: replace costs involving a separator on only one side
are split into an explicit delete+insert pair (Kotlin `applyDiff`).

`AmountField` enum members: `fixedFraction`, `zeroNotation`, `currencySuffix`.
Kotlin's `AmountFieldPositions` exposes `fixedFraction` and `zeroNotation`
ranges plus cursor position.

## Cells

- `MAX_STACK_SYMBOLS = 3`. When the stack grows past 3, the oldest symbol
  is dropped.
- Per-symbol animation: fade-in new, fade-out all prior.
- Per-symbol scale: `0.6 + (1.0 - 0.6) * appearance`.
- Cell bounds animate in both position and size.

## Animation parameters

- Duration: **120ms** (`DIFF_ANIMATION_DURATION_MS`).
- Easing:
  - Android (Compose): `tween()` default = Material `FastOutSlowInEasing`
    ≈ cubic-bezier `(0.4, 0.0, 0.2, 1.0)`.
  - iOS: `CAMediaTimingFunction(name: .default)` — the native iOS curve.
  - **Explicit divergence**: accepted in v1 for iOS-native feel. Do not
    attempt to match coefficients.

## Cursor

- Fade-in duration: 500ms.
- Blink interval: 530ms.
- Corner radius: `width / 2`.

## Keystroke sanitization

- Only digits and a single input separator are retained.
- Notation digits capped at `maximumNotationDigits`; fraction digits capped
  at `maximumFractionDigits`.
- Input separators `.` and `,` both accepted; the first one seen is
  converted to `config.decimalSeparator` in the sanitized buffer.
- Cursor position is remapped as characters are filtered.

## Currency info

- Swift: `NumberFormatter(.currency)` with explicit `currencyCode` and
  locale derived from that code.
- Both sides strip Unicode bidi control chars from prefix/suffix:
  U+200E, U+200F, U+202A–U+202E, U+2066–U+2069.

## Explicit out-of-scope divergences (v1)

- Easing curve coefficients (see "Animation parameters").
- Dynamic Type / accessibility scaling.
- RTL layout mirroring.
- Customizable animation duration or easing through public API.
```

- [ ] **Step 2: Commit**

```bash
git add camount-swift/PARITY.md
git commit -m "Add PARITY.md specifying Kotlin ↔ Swift behavioral contract"
```

---

## Task 21: Final verification

- [ ] **Step 1: Run all tests**

```bash
cd camount-swift && swift test && cd ..
```

Expected: every test passes.

- [ ] **Step 2: Confirm the package builds**

```bash
cd camount-swift && swift build -c release && cd ..
```

Expected: `Build complete!`.

- [ ] **Step 3: Walk the source tree**

```bash
find camount-swift -name "*.swift" -o -name "*.md" | sort
```

Expected: `Package.swift`, `PARITY.md`, 12 source files under `Sources/Camount/`, 6 test files under `Tests/CamountTests/`.

- [ ] **Step 4: Confirm `git status` is clean**

```bash
git status
```

Expected: `nothing to commit, working tree clean`.

---

## Self-review checklist (executor)

- [ ] All tests pass (`swift test`).
- [ ] `swift build -c release` is green.
- [ ] `PARITY.md` exists and mentions every divergence.
- [ ] No placeholder files remain (`Camount.swift`, `PlaceholderTests.swift` both deleted).
- [ ] Public API surface matches the spec: `Money`, `AmountStyle`, `CursorStyle`, `ShowSign`, `FractionPolicy`, `AmountAlignment`, `AmountText`, `AmountField`, modifier functions.
- [ ] No third-party dependencies added.
- [ ] `Package.swift` targets iOS 16 minimum.
- [ ] Every commit stages specific paths (no `git add -A`).

## Known issues to track (for v1.1)

Captured here so plan 3 / field testing can flag them if they show up:

1. **Per-symbol stacked-fade animation driver uses a main-thread `CADisplayLink`** rather than Core Animation off-thread. If sample-app profiling shows frame drops on iPhone 8-class hardware, refactor to one sublayer per stacked symbol with independent `opacity` animations running on the render server.
2. **Gradient text rendering** — Task 10's `AmountCellLayer` doesn't yet apply `AmountStyle.gradient`. The style field is captured for API parity with Kotlin; gradient rendering is deferred to a follow-up since it requires a layer mask with a `CAGradientLayer`, and the spec tolerated that as a plan-2 follow-up item. If a sample demands gradient, add it inline during plan 3's sample work or deferred to v1.1.
