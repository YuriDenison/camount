# iOS Sample App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up `samples/ios/` — a single SwiftUI Xcode project that embeds both CMP (via `samples/shared-compose`'s `SampleShared.framework`) and Native (via the `camount-swift` Swift Package) rendering of the same `Money` state, with a segmented picker that switches between modes at runtime. Delete the legacy root `iosApp/` once the new sample is green.

**Architecture:** One Xcode project, one SwiftUI `App` entry point. Root view owns a `@State Money` + `@State AppMode` (`.cmp` / `.native`) and renders either a `CmpDemo` (UIViewControllerRepresentable hosting `MainViewControllerKt.MainViewController()`) or a `NativeSampleScreen` (full SwiftUI port of `CamountSampleScreen` using the `camount-swift` widgets). The project links `SampleShared.framework` via the existing gradle Run Script build phase mechanism, and the `camount-swift` package via a local SPM path (`../../camount-swift`). The Manrope font is embedded as a bundled resource (registered via `UIAppFonts` in `Info.plist`). The legacy `iosApp/` directory is removed at the end; nothing else outside `samples/ios/` is modified.

**Tech Stack:** Swift 5.9+, SwiftUI, UIKit, Xcode 16.2, iOS 16 deployment target (matches `camount-swift`). Gradle for Kotlin framework assembly.

**Spec:** `docs/superpowers/specs/2026-04-19-ios-native-support-design.md` §3.2 (samples layout), §3.3 (runtime CMP/Native toggle), §4.10 (Swift API this plan consumes).

**Prerequisites:**
- Plan 1 (`2026-04-19-samples-reorganization.md`) is complete and merged. `:samples:shared-compose` exports `SampleShared.framework`; root `sample/` directory is already gone.
- Plan 2 (`2026-04-19-camount-swift-library.md`) is complete and merged. `camount-swift/` exists with `Package.swift` and the public API (`Money`, `AmountStyle`, `CursorStyle`, `ShowSign`, `FractionPolicy`, `AmountAlignment`, `AmountText`, `AmountField`, and the `.amountStyle`, `.showSign`, `.fractionPolicy`, `.maximumNotationDigits`, `.amountAlignment` modifiers).

**Out of scope:** Modifying `samples/shared-compose` or `camount-swift`. Introducing macOS/iPad-specific layouts. Automated UI tests.

---

## File Structure

**New (by the end of this plan):**
- `samples/ios/CamountSample.xcodeproj/project.pbxproj` — one target (`CamountSample`, SwiftUI app).
- `samples/ios/CamountSample/iOSApp.swift` — `@main` entry.
- `samples/ios/CamountSample/ContentView.swift` — root view, owns shared `Money`, renders mode picker + switched body.
- `samples/ios/CamountSample/AppMode.swift` — two-case enum `cmp` / `native` + `Picker` label strings.
- `samples/ios/CamountSample/CmpDemo.swift` — `UIViewControllerRepresentable` hosting `MainViewControllerKt.MainViewController()`. Mounts when `.cmp` is picked; presents the existing CMP sample in full.
- `samples/ios/CamountSample/NativeDemo/NativeSampleScreen.swift` — SwiftUI port of `CamountSampleScreen`. Root of the native demo; mirrors the layout (header + controls card + AmountText section + AmountField section) and drives the shared `Money` binding.
- `samples/ios/CamountSample/NativeDemo/CurrencyPickerSheet.swift` — SwiftUI sheet with the same currency list.
- `samples/ios/CamountSample/NativeDemo/NativeDemoTheme.swift` — color constants, font helpers.
- `samples/ios/CamountSample/Assets.xcassets/…` — empty app icon + accent color placeholders so Xcode doesn't warn.
- `samples/ios/CamountSample/Resources/manrope_medium.ttf` — copied from `samples/shared-compose/src/commonMain/composeResources/font/`.
- `samples/ios/CamountSample/Info.plist` — registers `manrope_medium.ttf` under `UIAppFonts`, sets `CADisableMinimumFrameDurationOnPhone` true.
- `samples/ios/Configuration/Config.xcconfig` — `TEAM_ID`, product id, version (cloned from `iosApp/Configuration/Config.xcconfig`).

**Modified:** none outside `samples/ios/`.

**Deleted (at end):** the entire `iosApp/` directory at the repo root.

---

## Prerequisites and Invariants

- Work must be done on a feature branch (not `main`).
- After every numbered task, run the stated verification before moving on.
- Do NOT use `git add -A` or `git add .`. Always stage the specific paths in the commit step.
- The legacy `iosApp/` directory stays in place until Task 14. During Tasks 1–13 both `samples/ios/` and `iosApp/` coexist. Only remove `iosApp/` after `samples/ios/` is fully green on a simulator.
- When creating the Xcode project, use Xcode's command-line tooling where possible, but accept that `project.pbxproj` content is hand-written here. The pbxproj in this plan is a full, known-good scaffold — do not improvise the structure.
- Minimum iOS 16.0. Keep `IPHONEOS_DEPLOYMENT_TARGET = 16.0` in every config entry we write (Apple's own default template uses 18.2; we override because `camount-swift` targets 16).
- Never force-push or use destructive git commands.
- The `CamountSample` target uses file-system-synchronized root groups, matching the pattern the legacy `iosApp` project uses — every file dropped under `samples/ios/CamountSample/` is automatically part of the target.

---

## Task 1: Create the samples/ios directory skeleton and copy the font resource

**Files:**
- Create: `samples/ios/CamountSample/Resources/manrope_medium.ttf` (copy)
- Create: various empty directories under `samples/ios/`

- [ ] **Step 1: Create the directory tree**

From repo root:
```bash
mkdir -p samples/ios/CamountSample/NativeDemo
mkdir -p samples/ios/CamountSample/Resources
mkdir -p samples/ios/CamountSample/Assets.xcassets/AppIcon.appiconset
mkdir -p samples/ios/CamountSample/Assets.xcassets/AccentColor.colorset
mkdir -p "samples/ios/CamountSample/Preview Content/Preview Assets.xcassets"
mkdir -p samples/ios/CamountSample.xcodeproj/project.xcworkspace
mkdir -p samples/ios/Configuration
```

- [ ] **Step 2: Copy the Manrope font**

```bash
cp samples/shared-compose/src/commonMain/composeResources/font/manrope_medium.ttf \
   samples/ios/CamountSample/Resources/manrope_medium.ttf
```

- [ ] **Step 3: Verify**

```bash
ls samples/ios/CamountSample/Resources/manrope_medium.ttf
```

Expected: the file exists and is non-empty.

- [ ] **Step 4: Commit**

```bash
git add samples/ios/CamountSample/Resources/manrope_medium.ttf
git commit -m "Scaffold samples/ios directory and bundle Manrope font"
```

---

## Task 2: Write Info.plist and Config.xcconfig

**Files:**
- Create: `samples/ios/CamountSample/Info.plist`
- Create: `samples/ios/Configuration/Config.xcconfig`

- [ ] **Step 1: Write `samples/ios/CamountSample/Info.plist`**

Exact content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CADisableMinimumFrameDurationOnPhone</key>
    <true/>
    <key>UIAppFonts</key>
    <array>
        <string>manrope_medium.ttf</string>
    </array>
</dict>
</plist>
```

- [ ] **Step 2: Write `samples/ios/Configuration/Config.xcconfig`**

Exact content:
```
TEAM_ID=

PRODUCT_NAME=CamountSample
PRODUCT_BUNDLE_IDENTIFIER=io.denison.camount.sample$(TEAM_ID)

CURRENT_PROJECT_VERSION=1
MARKETING_VERSION=1.0
```

- [ ] **Step 3: Commit**

```bash
git add samples/ios/CamountSample/Info.plist samples/ios/Configuration/Config.xcconfig
git commit -m "Add Info.plist and Config.xcconfig for samples/ios"
```

---

## Task 3: Write Asset Catalog scaffolding

**Files:**
- Create: `samples/ios/CamountSample/Assets.xcassets/Contents.json`
- Create: `samples/ios/CamountSample/Assets.xcassets/AppIcon.appiconset/Contents.json`
- Create: `samples/ios/CamountSample/Assets.xcassets/AccentColor.colorset/Contents.json`
- Create: `samples/ios/CamountSample/Preview Content/Preview Assets.xcassets/Contents.json`

No AppIcon image is required for the sample — Xcode will warn but build successfully. If we later want the Camount app icon, it can be copied from `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` in a follow-up.

- [ ] **Step 1: Write `samples/ios/CamountSample/Assets.xcassets/Contents.json`**

```json
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

- [ ] **Step 2: Write `samples/ios/CamountSample/Assets.xcassets/AppIcon.appiconset/Contents.json`**

```json
{
  "images" : [
    {
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

- [ ] **Step 3: Write `samples/ios/CamountSample/Assets.xcassets/AccentColor.colorset/Contents.json`**

```json
{
  "colors" : [
    {
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

- [ ] **Step 4: Write `samples/ios/CamountSample/Preview Content/Preview Assets.xcassets/Contents.json`**

```json
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

- [ ] **Step 5: Commit**

```bash
git add samples/ios/CamountSample/Assets.xcassets \
        "samples/ios/CamountSample/Preview Content"
git commit -m "Add asset catalog scaffolding for samples/ios"
```

---

## Task 4: Write the App entry point and AppMode enum

**Files:**
- Create: `samples/ios/CamountSample/iOSApp.swift`
- Create: `samples/ios/CamountSample/AppMode.swift`

- [ ] **Step 1: Write `samples/ios/CamountSample/iOSApp.swift`**

```swift
import SwiftUI

@main
struct CamountSampleApp: App {
  var body: some Scene {
    WindowGroup {
      ContentView()
    }
  }
}
```

- [ ] **Step 2: Write `samples/ios/CamountSample/AppMode.swift`**

```swift
import Foundation

enum AppMode: String, CaseIterable, Identifiable {
  case native
  case cmp

  var id: String { rawValue }

  var label: String {
    switch self {
    case .native: return "Native"
    case .cmp:    return "CMP"
    }
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add samples/ios/CamountSample/iOSApp.swift samples/ios/CamountSample/AppMode.swift
git commit -m "Add App entry point and AppMode enum"
```

---

## Task 5: Write the root ContentView (mode picker + mode switching)

**Files:**
- Create: `samples/ios/CamountSample/ContentView.swift`

The root view owns the single `Money` state. Both modes read and write that same binding — the whole point of the sample is to demonstrate parity on identical input.

- [ ] **Step 1: Write the file**

```swift
import Camount
import SwiftUI

struct ContentView: View {
  @State private var mode: AppMode = .native
  @State private var money = Money(units: 1234, nanos: 560_000_000, currencyCode: "EUR")

  var body: some View {
    VStack(spacing: 0) {
      Picker("Mode", selection: $mode) {
        ForEach(AppMode.allCases) { m in
          Text(m.label).tag(m)
        }
      }
      .pickerStyle(.segmented)
      .padding(.horizontal, 16)
      .padding(.top, 8)
      .padding(.bottom, 4)

      Divider()

      switch mode {
      case .native:
        NativeSampleScreen(money: $money)
      case .cmp:
        CmpDemo()
          .ignoresSafeArea(edges: [.horizontal, .bottom])
      }
    }
  }
}
```

> Note: `CmpDemo` does not share the `money` `@State` — CMP mode hosts its own Compose state inside `MainViewController()`. The picker is purely a render-mode toggle, not a bidirectional state bridge. This matches the spec (§3.3: "one build, instant A/B comparison"); a state bridge across the Kotlin/Swift divide is explicitly out of scope for v1.

- [ ] **Step 2: Commit**

```bash
git add samples/ios/CamountSample/ContentView.swift
git commit -m "Add root ContentView with mode picker"
```

---

## Task 6: Write the CMP mode hosting view

**Files:**
- Create: `samples/ios/CamountSample/CmpDemo.swift`

This file bridges to the `SampleShared` framework exported by `:samples:shared-compose`. Same pattern as the legacy `iosApp/iosApp/ContentView.swift` that we are replacing.

- [ ] **Step 1: Write the file**

```swift
import SampleShared
import SwiftUI
import UIKit

struct CmpDemo: UIViewControllerRepresentable {
  func makeUIViewController(context: Context) -> UIViewController {
    MainViewControllerKt.MainViewController()
  }

  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/ios/CamountSample/CmpDemo.swift
git commit -m "Add CmpDemo hosting shared-compose framework"
```

---

## Task 7: Write the NativeDemoTheme (colors + font helpers)

**Files:**
- Create: `samples/ios/CamountSample/NativeDemo/NativeDemoTheme.swift`

Mirrors the color constants in `CamountSampleScreen.kt` line-for-line. Keeping them centralized in one file makes the port trivial to diff against the Kotlin original.

- [ ] **Step 1: Write the file**

```swift
import SwiftUI
import UIKit

enum NativeDemoTheme {
  // Color values copied verbatim from CamountSampleScreen.kt (Compose hex literals).
  static let accent       = UIColor(red: 0x40/255.0, green: 0x49/255.0, blue: 0xFF/255.0, alpha: 1)
  static let accentAlt    = UIColor(red: 0xFF/255.0, green: 0x40/255.0, blue: 0x81/255.0, alpha: 1)
  static let ink          = UIColor(red: 0x0F/255.0, green: 0x10/255.0, blue: 0x24/255.0, alpha: 1)
  static let inkMuted     = UIColor(red: 0x5A/255.0, green: 0x5E/255.0, blue: 0x80/255.0, alpha: 1)
  static let canvas       = UIColor(red: 0xF7/255.0, green: 0xF7/255.0, blue: 0xFB/255.0, alpha: 1)
  static let fieldSurface = UIColor(red: 0xEE/255.0, green: 0xEF/255.0, blue: 0xF6/255.0, alpha: 1)
  static let placeholder  = UIColor(red: 0xB5/255.0, green: 0xB8/255.0, blue: 0xCC/255.0, alpha: 1)
  static let border       = UIColor(red: 0xE0/255.0, green: 0xE1/255.0, blue: 0xEC/255.0, alpha: 1)
  static let selectedRow  = UIColor(red: 0xEF/255.0, green: 0xF0/255.0, blue: 0xFF/255.0, alpha: 1)

  // SwiftUI-typed mirrors for layout use.
  static var canvasColor: Color       { Color(canvas) }
  static var inkColor: Color          { Color(ink) }
  static var inkMutedColor: Color     { Color(inkMuted) }
  static var accentColor: Color       { Color(accent) }
  static var fieldSurfaceColor: Color { Color(fieldSurface) }
  static var borderColor: Color       { Color(border) }
  static var selectedRowColor: Color  { Color(selectedRow) }

  // Manrope is registered in Info.plist (UIAppFonts). PostScript name resolves
  // via UIFont init.
  static let manropeFamilyName = "Manrope-Medium"

  static func manrope(_ size: CGFloat, weight: UIFont.Weight = .medium) -> UIFont {
    if let font = UIFont(name: manropeFamilyName, size: size) {
      // Manrope-Medium ships one weight; synthesize Bold/SemiBold via
      // UIFontDescriptor symbolic traits. For Medium/Regular we return as-is.
      switch weight {
      case .bold, .heavy, .black:
        let desc = font.fontDescriptor.withSymbolicTraits(.traitBold) ?? font.fontDescriptor
        return UIFont(descriptor: desc, size: size)
      default:
        return font
      }
    }
    return .systemFont(ofSize: size, weight: weight)
  }

  static func manropeFont(_ size: CGFloat, weight: Font.Weight = .medium) -> Font {
    if UIFont(name: manropeFamilyName, size: size) != nil {
      return .custom(manropeFamilyName, size: size).weight(weight)
    }
    return .system(size: size, weight: weight)
  }
}

let sampleCurrencies: [(code: String, name: String)] = [
  ("EUR", "Euro"),
  ("USD", "US Dollar"),
  ("GBP", "British Pound"),
  ("JPY", "Japanese Yen"),
  ("CHF", "Swiss Franc"),
  ("CAD", "Canadian Dollar"),
  ("AUD", "Australian Dollar"),
  ("SEK", "Swedish Krona"),
  ("NOK", "Norwegian Krone"),
  ("PLN", "Polish Zloty"),
  ("CNY", "Chinese Yuan"),
  ("INR", "Indian Rupee"),
]
```

- [ ] **Step 2: Commit**

```bash
git add samples/ios/CamountSample/NativeDemo/NativeDemoTheme.swift
git commit -m "Add NativeDemoTheme with color and font helpers"
```

---

## Task 8: Write CurrencyPickerSheet

**Files:**
- Create: `samples/ios/CamountSample/NativeDemo/CurrencyPickerSheet.swift`

Ports the `CurrencyPickerSheet` + `CurrencyRow` composables from `CamountSampleScreen.kt`. Uses SwiftUI's `.sheet(...)` presentation — the Compose version uses `ModalBottomSheet`; iOS idiom is the sheet presentation detent.

- [ ] **Step 1: Write the file**

```swift
import SwiftUI

struct CurrencyPickerSheet: View {
  let selected: String
  let onSelect: (String) -> Void

  @Environment(\.dismiss) private var dismiss

  var body: some View {
    NavigationStack {
      ScrollView {
        VStack(alignment: .leading, spacing: 0) {
          Text("Choose currency")
            .font(NativeDemoTheme.manropeFont(18, weight: .semibold))
            .foregroundColor(NativeDemoTheme.inkColor)
            .padding(.horizontal, 24)
            .padding(.vertical, 8)

          ForEach(sampleCurrencies, id: \.code) { item in
            CurrencyRow(
              code: item.code,
              name: item.name,
              isSelected: item.code == selected,
              onTap: {
                onSelect(item.code)
                dismiss()
              },
            )
          }
        }
        .padding(.bottom, 24)
      }
      .background(Color.white)
    }
    .presentationDetents([.medium, .large])
    .presentationDragIndicator(.visible)
  }
}

private struct CurrencyRow: View {
  let code: String
  let name: String
  let isSelected: Bool
  let onTap: () -> Void

  var body: some View {
    Button(action: onTap) {
      HStack(spacing: 16) {
        Text(code)
          .font(NativeDemoTheme.manropeFont(15, weight: .bold))
          .foregroundColor(isSelected ? NativeDemoTheme.accentColor : NativeDemoTheme.inkColor)
          .frame(width: 48, alignment: .leading)
        Text(name)
          .font(NativeDemoTheme.manropeFont(15, weight: .medium))
          .foregroundColor(NativeDemoTheme.inkColor)
          .frame(maxWidth: .infinity, alignment: .leading)
        if isSelected {
          Text("✓")
            .font(NativeDemoTheme.manropeFont(18, weight: .bold))
            .foregroundColor(NativeDemoTheme.accentColor)
        }
      }
      .padding(.horizontal, 24)
      .padding(.vertical, 14)
      .background(isSelected ? NativeDemoTheme.selectedRowColor : Color.clear)
      .contentShape(Rectangle())
    }
    .buttonStyle(.plain)
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/ios/CamountSample/NativeDemo/CurrencyPickerSheet.swift
git commit -m "Add CurrencyPickerSheet SwiftUI component"
```

---

## Task 9: Write NativeSampleScreen (layout + wiring)

**Files:**
- Create: `samples/ios/CamountSample/NativeDemo/NativeSampleScreen.swift`

Full SwiftUI port of `CamountSampleScreen.kt`. Layout stays as close to the Compose original as SwiftUI idioms allow: same card shapes, same spacings, same action buttons, same three `AmountText` variants, same three `AmountField` variants (default / gradient / compact).

`NativeSampleScreen` takes a `Binding<Money>` so the parent owns state (`ContentView`). Internally, the currency picker sheet is gated by a local `@State`.

`Money` is imported from the `Camount` module (the `camount-swift` package).

- [ ] **Step 1: Write the file**

```swift
import Camount
import SwiftUI
import UIKit

struct NativeSampleScreen: View {
  @Binding var money: Money

  @State private var pickerOpen = false

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 20) {
        header

        controlsCard

        amountTextSection

        amountFieldSection

        Spacer(minLength: 8).frame(height: 8)
      }
      .padding(.horizontal, 20)
      .padding(.top, 16)
      .padding(.bottom, 24)
    }
    .background(NativeDemoTheme.canvasColor.ignoresSafeArea())
    .sheet(isPresented: $pickerOpen) {
      CurrencyPickerSheet(
        selected: money.currencyCode,
        onSelect: { newCode in
          money = Money(units: money.units, nanos: money.nanos, currencyCode: newCode)
        },
      )
    }
  }

  // MARK: - Sections

  private var header: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text("Camount")
        .font(NativeDemoTheme.manropeFont(34, weight: .bold))
        .foregroundColor(NativeDemoTheme.inkColor)
        .tracking(-0.5)
      Text("Every widget below shares the same Money — change any, watch the rest animate in sync.")
        .font(NativeDemoTheme.manropeFont(14, weight: .medium))
        .foregroundColor(NativeDemoTheme.inkMutedColor)
    }
  }

  private var controlsCard: some View {
    ElevatedCard {
      VStack(spacing: 14) {
        currencyDropdown
        HStack(spacing: 10) {
          actionButton(label: "Shuffle") {
            money = randomMoney(currencyCode: money.currencyCode)
          }
          .frame(maxWidth: .infinity)

          plusOneButton {
            money = Money(units: money.units + 1, nanos: money.nanos, currencyCode: money.currencyCode)
          }
          .frame(maxWidth: .infinity)

          resetButton {
            money = Money.zero(money.currencyCode)
          }
          .frame(maxWidth: .infinity)
        }
      }
      .padding(16)
    }
  }

  private var currencyDropdown: some View {
    let name = sampleCurrencies.first { $0.code == money.currencyCode }?.name ?? money.currencyCode
    return Button(action: { pickerOpen = true }) {
      HStack(spacing: 12) {
        Text(money.currencyCode)
          .font(NativeDemoTheme.manropeFont(15, weight: .bold))
          .foregroundColor(NativeDemoTheme.inkColor)
        Text(name)
          .font(NativeDemoTheme.manropeFont(14, weight: .medium))
          .foregroundColor(NativeDemoTheme.inkMutedColor)
          .frame(maxWidth: .infinity, alignment: .leading)
        Text("▾")
          .font(NativeDemoTheme.manropeFont(16, weight: .medium))
          .foregroundColor(NativeDemoTheme.inkMutedColor)
      }
      .padding(.horizontal, 16)
      .frame(height: 56)
      .background(Color.white)
      .overlay(
        RoundedRectangle(cornerRadius: 14)
          .stroke(NativeDemoTheme.borderColor, lineWidth: 1),
      )
      .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private var amountTextSection: some View {
    SectionCard(title: "AmountText", subtitle: "SwiftUI (camount-swift)") {
      VStack(alignment: .leading, spacing: 10) {
        AmountText(money)
          .amountStyle(AmountStyle(
            font: NativeDemoTheme.manrope(44, weight: .bold),
            color: NativeDemoTheme.ink,
          ))
          .frame(maxWidth: .infinity, minHeight: 60, maxHeight: 60, alignment: .leading)

        HStack(spacing: 12) {
          LabeledAmount(label: "Always signed") {
            AmountText(money)
              .amountStyle(AmountStyle(
                font: NativeDemoTheme.manrope(22, weight: .medium),
                color: NativeDemoTheme.ink,
              ))
              .showSign(.always)
              .frame(maxWidth: .infinity, minHeight: 32, maxHeight: 32, alignment: .leading)
          }

          LabeledAmount(label: "No trailing zeros") {
            AmountText(money)
              .amountStyle(AmountStyle(
                font: NativeDemoTheme.manrope(18, weight: .regular),
                color: NativeDemoTheme.inkMuted,
              ))
              .fractionPolicy(.ignoreZero)
              .frame(maxWidth: .infinity, minHeight: 32, maxHeight: 32, alignment: .leading)
          }
        }
      }
    }
  }

  private var amountFieldSection: some View {
    SectionCard(title: "AmountField", subtitle: "SwiftUI (camount-swift)") {
      let defaultStyle = AmountStyle(
        font: NativeDemoTheme.manrope(36, weight: .semibold),
        color: NativeDemoTheme.ink,
        cursor: CursorStyle(color: NativeDemoTheme.accent),
        zeroNotationColor: NativeDemoTheme.placeholder,
        fixedFractionColor: NativeDemoTheme.placeholder,
      )
      let gradientStyle = AmountStyle(
        font: NativeDemoTheme.manrope(44, weight: .bold),
        color: NativeDemoTheme.ink,
        gradient: Gradient(colors: [Color(NativeDemoTheme.accent), Color(NativeDemoTheme.accentAlt)]),
        cursor: CursorStyle(color: NativeDemoTheme.accentAlt),
      )
      let compactStyle = AmountStyle(
        font: NativeDemoTheme.manrope(20, weight: .medium),
        color: NativeDemoTheme.ink,
        cursor: CursorStyle(color: NativeDemoTheme.ink),
      )

      VStack(spacing: 12) {
        FieldBox(height: 64) {
          AmountField($money)
            .amountStyle(defaultStyle)
            .frame(maxWidth: .infinity, minHeight: 56, maxHeight: 56)
        }
        FieldBox(height: 76) {
          AmountField($money)
            .amountStyle(gradientStyle)
            .frame(maxWidth: .infinity, minHeight: 64, maxHeight: 64)
        }
        FieldBox(height: 48) {
          AmountField($money)
            .amountStyle(compactStyle)
            .frame(maxWidth: .infinity, minHeight: 32, maxHeight: 32)
        }
      }
    }
  }

  // MARK: - Small helpers

  private func actionButton(label: String, action: @escaping () -> Void) -> some View {
    Button(action: action) {
      Text(label)
        .font(NativeDemoTheme.manropeFont(14, weight: .medium))
        .foregroundColor(NativeDemoTheme.inkColor)
        .frame(maxWidth: .infinity, minHeight: 44)
        .background(NativeDemoTheme.fieldSurfaceColor)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private func plusOneButton(action: @escaping () -> Void) -> some View {
    Button(action: action) {
      Text("+1")
        .font(NativeDemoTheme.manropeFont(15, weight: .semibold))
        .foregroundColor(.white)
        .frame(maxWidth: .infinity, minHeight: 44)
        .background(NativeDemoTheme.accentColor)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private func resetButton(action: @escaping () -> Void) -> some View {
    Button(action: action) {
      Text("Reset")
        .font(NativeDemoTheme.manropeFont(14, weight: .medium))
        .foregroundColor(NativeDemoTheme.inkColor)
        .frame(maxWidth: .infinity, minHeight: 44)
        .background(Color.white)
        .overlay(
          RoundedRectangle(cornerRadius: 14)
            .stroke(NativeDemoTheme.borderColor, lineWidth: 1),
        )
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    .buttonStyle(.plain)
  }

  private func randomMoney(currencyCode: String) -> Money {
    Money(
      units: Int64.random(in: 0..<99_999),
      nanos: Int32.random(in: 0..<1_000_000_000),
      currencyCode: currencyCode,
    )
  }
}

// MARK: - Layout primitives

private struct ElevatedCard<Content: View>: View {
  @ViewBuilder let content: () -> Content
  var body: some View {
    content()
      .frame(maxWidth: .infinity, alignment: .leading)
      .background(Color.white)
      .clipShape(RoundedRectangle(cornerRadius: 20))
      .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 2)
  }
}

private struct SectionCard<Content: View>: View {
  let title: String
  let subtitle: String
  @ViewBuilder let content: () -> Content
  var body: some View {
    ElevatedCard {
      VStack(alignment: .leading, spacing: 14) {
        HStack(alignment: .bottom, spacing: 8) {
          Text(title)
            .font(NativeDemoTheme.manropeFont(18, weight: .semibold))
            .foregroundColor(NativeDemoTheme.inkColor)
          Text(subtitle)
            .font(NativeDemoTheme.manropeFont(12, weight: .medium))
            .foregroundColor(NativeDemoTheme.inkMutedColor)
            .padding(.bottom, 2)
        }
        content()
      }
      .padding(18)
    }
  }
}

private struct LabeledAmount<Content: View>: View {
  let label: String
  @ViewBuilder let content: () -> Content
  var body: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(label.uppercased())
        .font(NativeDemoTheme.manropeFont(10, weight: .semibold))
        .foregroundColor(NativeDemoTheme.inkMutedColor)
        .tracking(0.8)
      content()
    }
    .frame(maxWidth: .infinity, alignment: .leading)
  }
}

private struct FieldBox<Content: View>: View {
  let height: CGFloat
  @ViewBuilder let content: () -> Content
  var body: some View {
    content()
      .padding(.horizontal, 16)
      .padding(.vertical, 4)
      .frame(maxWidth: .infinity, minHeight: height, maxHeight: height, alignment: .leading)
      .background(NativeDemoTheme.fieldSurfaceColor)
      .clipShape(RoundedRectangle(cornerRadius: 14))
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/ios/CamountSample/NativeDemo/NativeSampleScreen.swift
git commit -m "Add NativeSampleScreen SwiftUI port"
```

---

## Task 10: Write the Xcode project file (project.pbxproj)

**Files:**
- Create: `samples/ios/CamountSample.xcodeproj/project.pbxproj`
- Create: `samples/ios/CamountSample.xcodeproj/project.xcworkspace/contents.xcworkspacedata`

Modeled after `iosApp/iosApp.xcodeproj/project.pbxproj`: file-system-synchronized root groups, a Run Script phase that invokes gradle for the CMP framework, and a local-path SPM dependency for `camount-swift`. The pbxproj is deliberately hand-written so the agent doesn't have to generate one — it's a known-good scaffold.

Important Xcode specifics:
- `IPHONEOS_DEPLOYMENT_TARGET = 16.0` (matches `camount-swift`).
- `PRODUCT_BUNDLE_IDENTIFIER` comes from `Config.xcconfig`.
- `FRAMEWORK_SEARCH_PATHS` includes `$(SRCROOT)/../../samples/shared-compose/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` — matches the path that `:samples:shared-compose:embedAndSignAppleFrameworkForXcode` writes the framework to.
- `OTHER_LDFLAGS = -framework SampleShared` so the CMP framework is linked.
- Swift Package reference points at `../../camount-swift` as a local path.

- [ ] **Step 1: Write `samples/ios/CamountSample.xcodeproj/project.pbxproj`**

```
// !$*UTF8*$!
{
	archiveVersion = 1;
	classes = {
	};
	objectVersion = 77;
	objects = {

/* Begin PBXBuildFile section */
		CA0001A1 /* Camount in Frameworks */ = {isa = PBXBuildFile; productRef = CA0001A0 /* Camount */; };
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
		CA0000FF /* CamountSample.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = CamountSample.app; sourceTree = BUILT_PRODUCTS_DIR; };
/* End PBXFileReference section */

/* Begin PBXFileSystemSynchronizedBuildFileExceptionSet section */
		CA000101 /* Exceptions for "CamountSample" folder in "CamountSample" target */ = {
			isa = PBXFileSystemSynchronizedBuildFileExceptionSet;
			membershipExceptions = (
				Info.plist,
			);
			target = CA000200 /* CamountSample */;
		};
/* End PBXFileSystemSynchronizedBuildFileExceptionSet section */

/* Begin PBXFileSystemSynchronizedRootGroup section */
		CA000102 /* CamountSample */ = {
			isa = PBXFileSystemSynchronizedRootGroup;
			exceptions = (
				CA000101 /* Exceptions for "CamountSample" folder in "CamountSample" target */,
			);
			path = CamountSample;
			sourceTree = "<group>";
		};
		CA000103 /* Configuration */ = {
			isa = PBXFileSystemSynchronizedRootGroup;
			path = Configuration;
			sourceTree = "<group>";
		};
/* End PBXFileSystemSynchronizedRootGroup section */

/* Begin PBXFrameworksBuildPhase section */
		CA000300 /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
				CA0001A1 /* Camount in Frameworks */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		CA000104 = {
			isa = PBXGroup;
			children = (
				CA000103 /* Configuration */,
				CA000102 /* CamountSample */,
				CA000105 /* Products */,
			);
			sourceTree = "<group>";
		};
		CA000105 /* Products */ = {
			isa = PBXGroup;
			children = (
				CA0000FF /* CamountSample.app */,
			);
			name = Products;
			sourceTree = "<group>";
		};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		CA000200 /* CamountSample */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = CA000401 /* Build configuration list for PBXNativeTarget "CamountSample" */;
			buildPhases = (
				CA000301 /* Compile Kotlin Framework */,
				CA000302 /* Sources */,
				CA000300 /* Frameworks */,
				CA000303 /* Resources */,
			);
			buildRules = (
			);
			dependencies = (
			);
			fileSystemSynchronizedGroups = (
				CA000102 /* CamountSample */,
			);
			name = CamountSample;
			packageProductDependencies = (
				CA0001A0 /* Camount */,
			);
			productName = CamountSample;
			productReference = CA0000FF /* CamountSample.app */;
			productType = "com.apple.product-type.application";
		};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		CA000000 /* Project object */ = {
			isa = PBXProject;
			attributes = {
				BuildIndependentTargetsInParallel = 1;
				LastSwiftUpdateCheck = 1620;
				LastUpgradeCheck = 1620;
				TargetAttributes = {
					CA000200 = {
						CreatedOnToolsVersion = 16.2;
					};
				};
			};
			buildConfigurationList = CA000400 /* Build configuration list for PBXProject "CamountSample" */;
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = CA000104;
			minimizedProjectReferenceProxies = 1;
			packageReferences = (
				CA000500 /* XCLocalSwiftPackageReference "../../camount-swift" */,
			);
			preferredProjectObjectVersion = 77;
			productRefGroup = CA000105 /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				CA000200 /* CamountSample */,
			);
		};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		CA000303 /* Resources */ = {
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		CA000301 /* Compile Kotlin Framework */ = {
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputFileListPaths = (
			);
			inputPaths = (
			);
			name = "Compile Kotlin Framework";
			outputFileListPaths = (
			);
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "if [ \"YES\" = \"$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\" ]; then\n  echo \"Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \\\"YES\\\"\"\n  exit 0\nfi\ncd \"$SRCROOT/../..\"\n./gradlew :samples:shared-compose:embedAndSignAppleFrameworkForXcode\n";
		};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		CA000302 /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		CA000600 /* Debug */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReferenceAnchor = CA000103 /* Configuration */;
			baseConfigurationReferenceRelativePath = Config.xcconfig;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS = YES;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_COMMA = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
				CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
				CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
				CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;
				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;
				CLANG_WARN_STRICT_PROTOTYPES = YES;
				CLANG_WARN_SUSPICIOUS_MOVE = YES;
				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_TESTABILITY = YES;
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_OPTIMIZATION_LEVEL = 0;
				GCC_PREPROCESSOR_DEFINITIONS = (
					"DEBUG=1",
					"$(inherited)",
				);
				GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNDECLARED_SELECTOR = YES;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LOCALIZATION_PREFERS_STRING_CATALOGS = YES;
				MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
				MTL_FAST_MATH = YES;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = "DEBUG $(inherited)";
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
			};
			name = Debug;
		};
		CA000601 /* Release */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReferenceAnchor = CA000103 /* Configuration */;
			baseConfigurationReferenceRelativePath = Config.xcconfig;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS = YES;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_COMMA = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
				CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
				CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
				CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;
				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;
				CLANG_WARN_STRICT_PROTOTYPES = YES;
				CLANG_WARN_SUSPICIOUS_MOVE = YES;
				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_NS_ASSERTIONS = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNDECLARED_SELECTOR = YES;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LOCALIZATION_PREFERS_STRING_CATALOGS = YES;
				MTL_ENABLE_DEBUG_INFO = NO;
				MTL_FAST_MATH = YES;
				SDKROOT = iphoneos;
				SWIFT_COMPILATION_MODE = wholemodule;
				VALIDATE_PRODUCT = YES;
			};
			name = Release;
		};
		CA000602 /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ARCHS = arm64;
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
				CODE_SIGN_IDENTITY = "Apple Development";
				CODE_SIGN_STYLE = Automatic;
				DEVELOPMENT_ASSET_PATHS = "\"CamountSample/Preview Content\"";
				DEVELOPMENT_TEAM = "${TEAM_ID}";
				ENABLE_PREVIEWS = YES;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared-compose/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = CamountSample/Info.plist;
				INFOPLIST_KEY_UIApplicationSceneManifest_Generation = YES;
				INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents = YES;
				INFOPLIST_KEY_UILaunchScreen_Generation = YES;
				INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad = "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight";
				INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone = "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight";
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = (
					"$(inherited)",
					"-framework",
					"SampleShared",
				);
				SWIFT_EMIT_LOC_STRINGS = YES;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Debug;
		};
		CA000603 /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ARCHS = arm64;
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
				CODE_SIGN_IDENTITY = "Apple Development";
				CODE_SIGN_STYLE = Automatic;
				DEVELOPMENT_ASSET_PATHS = "\"CamountSample/Preview Content\"";
				DEVELOPMENT_TEAM = "${TEAM_ID}";
				ENABLE_PREVIEWS = YES;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared-compose/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = CamountSample/Info.plist;
				INFOPLIST_KEY_UIApplicationSceneManifest_Generation = YES;
				INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents = YES;
				INFOPLIST_KEY_UILaunchScreen_Generation = YES;
				INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad = "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight";
				INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone = "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight";
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = (
					"$(inherited)",
					"-framework",
					"SampleShared",
				);
				SWIFT_EMIT_LOC_STRINGS = YES;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Release;
		};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		CA000400 /* Build configuration list for PBXProject "CamountSample" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				CA000600 /* Debug */,
				CA000601 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		CA000401 /* Build configuration list for PBXNativeTarget "CamountSample" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				CA000602 /* Debug */,
				CA000603 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
/* End XCConfigurationList section */

/* Begin XCLocalSwiftPackageReference section */
		CA000500 /* XCLocalSwiftPackageReference "../../camount-swift" */ = {
			isa = XCLocalSwiftPackageReference;
			relativePath = ../../camount-swift;
		};
/* End XCLocalSwiftPackageReference section */

/* Begin XCSwiftPackageProductDependency section */
		CA0001A0 /* Camount */ = {
			isa = XCSwiftPackageProductDependency;
			productName = Camount;
		};
/* End XCSwiftPackageProductDependency section */
	};
	rootObject = CA000000 /* Project object */;
}
```

- [ ] **Step 2: Write `samples/ios/CamountSample.xcodeproj/project.xcworkspace/contents.xcworkspacedata`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Workspace
   version = "1.0">
   <FileRef
      location = "self:">
   </FileRef>
</Workspace>
```

- [ ] **Step 3: Commit**

```bash
git add samples/ios/CamountSample.xcodeproj
git commit -m "Add CamountSample Xcode project"
```

---

## Task 11: Verify the samples/ios project builds

**Files:** none modified here — this is a build verification task.

- [ ] **Step 1: Pre-build the Kotlin framework**

The Xcode build phase will invoke gradle, but on the first build it's worth proving the framework builds from the CLI too (surfaces gradle errors without an Xcode wrapper in the way).

```bash
./gradlew :samples:shared-compose:embedAndSignAppleFrameworkForXcode \
  -PXCODE_CONFIGURATION=Debug \
  -PXCODE_PLATFORM_NAME=iphonesimulator \
  -PXCODE_ARCHS=arm64
```

Expected: `BUILD SUCCESSFUL`. `SampleShared.framework` appears under `samples/shared-compose/build/xcode-frameworks/Debug/iphonesimulator/`.

If this step fails because the invocation isn't set up the same way Xcode sets it up, it's acceptable to skip to Step 2 and rely on Xcode to drive the gradle invocation. Document the skip in the commit message.

- [ ] **Step 2: Build the Xcode project for a simulator**

```bash
xcodebuild \
  -project samples/ios/CamountSample.xcodeproj \
  -scheme CamountSample \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug \
  build
```

Expected: `BUILD SUCCEEDED`.

Common failure modes:
- **"No scheme named CamountSample"** — Xcode auto-generates schemes only on first open in the IDE. Open the project in Xcode once (double-click the `.xcodeproj`) and it will materialize `CamountSample.xcscheme` under `xcuserdata`. Or pass `-list` to confirm.
- **"Framework not found SampleShared"** — the gradle script didn't run, or ran for a different SDK. Confirm Step 1 ran, or check that the Run Script phase output in Xcode says the gradle task ran.
- **"No such module 'Camount'"** — the SPM package path is wrong. Confirm `camount-swift/Package.swift` exists at `../../camount-swift` relative to the xcodeproj.

- [ ] **Step 3: No commit necessary** (no files changed).

Document any build log issues in the PR description rather than here.

---

## Task 12: Run on a simulator and smoke-test both modes

**Files:** none — this is a manual verification step.

- [ ] **Step 1: Boot a simulator**

Pick any iOS 17+ simulator (iPhone 15 works fine). Either:
- Open Xcode → select the CamountSample scheme → select a simulator → ⌘R.
- Or from the terminal:
  ```bash
  xcrun simctl boot "iPhone 15"
  xcodebuild \
    -project samples/ios/CamountSample.xcodeproj \
    -scheme CamountSample \
    -destination 'platform=iOS Simulator,name=iPhone 15' \
    -configuration Debug build install
  xcrun simctl launch booted io.denison.camount.sample
  open -a Simulator
  ```

- [ ] **Step 2: Smoke-test Native mode**

With the segmented picker on "Native":
- Header renders with Manrope font (if Manrope is missing, it falls back to system font — that's a red flag worth investigating).
- Currency dropdown opens a sheet; selecting a different code (e.g. `JPY`) updates both the dropdown label and the fraction behavior in the amount widgets (JPY shows zero fraction digits).
- `Shuffle` scrambles the amount; `+1` increments; `Reset` zeroes. All three animate the per-digit diff.
- All three `AmountField` variants (default, gradient, compact) accept keyboard input, show a blinking cursor, and write back into the shared `Money`.
- Typing in any `AmountField` updates the `AmountText` section above with animated digit morphs.

- [ ] **Step 3: Smoke-test CMP mode**

Tap the segmented picker to switch to "CMP". The Compose screen renders end-to-end using `samples/shared-compose`'s framework. Exercise the same controls (Shuffle, +1, Reset, currency picker, fields) — behavior should match Native mode.

Known intentional difference: the `Money` state is NOT shared across the picker. Switching from Native to CMP and back presents each implementation's independent in-memory state. This matches the spec (§3.3).

- [ ] **Step 4: If anything fails**

Do NOT mark the plan complete. Investigate, fix, commit the fix on this branch, and restart the smoke test.

- [ ] **Step 5: No commit** (purely a verification step).

---

## Task 13: Verify the repo still builds from the root

**Files:** none — sanity check on gradle wiring.

- [ ] **Step 1: Full gradle build**

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Confirm project listing**

```bash
./gradlew projects
```

Expected modules: `:camount`, `:camount-view`, `:camount-view-databinding`, `:samples:shared-compose`, `:samples:android`. No new gradle modules added in this plan — `samples/ios` is an Xcode project only.

- [ ] **Step 3: No commit** (no files changed).

---

## Task 14: Delete the legacy iosApp/ directory

**Files:**
- Delete: entire `iosApp/` directory at the repo root.

This step is irreversible (beyond git history). Only proceed if Tasks 11–13 are all green.

- [ ] **Step 1: Confirm samples/ios is fully functional**

Re-run Task 12's smoke test one more time. Both modes must be green.

- [ ] **Step 2: Remove the directory**

```bash
git rm -r iosApp
```

- [ ] **Step 3: Verify it's gone**

```bash
ls iosApp 2>&1
```

Expected: `ls: iosApp: No such file or directory`.

- [ ] **Step 4: Rebuild samples/ios to confirm it didn't depend on anything in iosApp/**

```bash
xcodebuild \
  -project samples/ios/CamountSample.xcodeproj \
  -scheme CamountSample \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build
```

Expected: `BUILD SUCCEEDED`.

- [ ] **Step 5: Commit**

```bash
git commit -m "Remove legacy iosApp/ — replaced by samples/ios

samples/ios hosts the same CMP demo (via :samples:shared-compose's
SampleShared.framework) plus a Native mode that renders camount-swift
widgets. No root Xcode project remains at iosApp/."
```

---

## Task 15: Final verification

- [ ] **Step 1: Tree walk**

```bash
ls samples/
ls samples/ios/
ls samples/ios/CamountSample/
ls samples/ios/CamountSample/NativeDemo/
find iosApp -type f 2>&1
```

Expected:
- `samples/` contains `shared-compose`, `android`, `ios`.
- `samples/ios/` contains `CamountSample`, `CamountSample.xcodeproj`, `Configuration`.
- `samples/ios/CamountSample/` contains `Assets.xcassets`, `ContentView.swift`, `iOSApp.swift`, `AppMode.swift`, `CmpDemo.swift`, `NativeDemo/`, `Preview Content/`, `Resources/`, `Info.plist`.
- `samples/ios/CamountSample/NativeDemo/` contains `NativeSampleScreen.swift`, `CurrencyPickerSheet.swift`, `NativeDemoTheme.swift`.
- `find iosApp` errors out.

- [ ] **Step 2: Gradle clean build + Xcode build**

```bash
./gradlew clean build
xcodebuild \
  -project samples/ios/CamountSample.xcodeproj \
  -scheme CamountSample \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build
```

Both expected: `BUILD SUCCESSFUL` / `BUILD SUCCEEDED`.

- [ ] **Step 3: Final smoke-test one more time**

- Native mode: currency picker, shuffle, +1, reset, three AmountField variants, three AmountText variants — all animate and update.
- CMP mode: same checks inside the embedded Compose screen.
- Switch the segmented picker at least three times; no crashes.

- [ ] **Step 4: Confirm clean git status**

```bash
git status
```

Expected: `nothing to commit, working tree clean`. Build artifacts under `samples/ios/build`, `DerivedData`, or `samples/shared-compose/build/xcode-frameworks` must be ignored — confirm `.gitignore` covers them. If `.gitignore` needs an addition, make it a separate commit with a focused message.

---

## Self-review checklist (executor)

Before declaring the plan complete, tick off all of the following:

- [ ] `samples/ios/CamountSample.xcodeproj` exists and opens in Xcode without errors.
- [ ] `xcodebuild ... build` succeeds for an iOS Simulator destination.
- [ ] The app launches on a simulator and both "Native" and "CMP" modes render.
- [ ] Three `AmountField` variants (default, gradient, compact) accept keyboard input and update the shared `Money` state in Native mode.
- [ ] Currency picker sheet lists all 12 currencies and switching to `JPY` changes the fraction behavior.
- [ ] The Manrope font renders (not system fallback) in Native mode's header.
- [ ] Root `iosApp/` directory no longer exists.
- [ ] `./gradlew projects` shows no `:iosApp` or `:sample` module.
- [ ] All commits were staged with explicit paths (no `git add -A`).
- [ ] No changes were made outside `samples/ios/` (apart from deleting `iosApp/`).

---

## Known issues to track (v1 → v1.1)

- **Manrope weight variants.** Only `manrope_medium.ttf` is bundled. Native-mode labels requesting `.semibold` / `.bold` / `.semiBold` synthesize boldness via `UIFontDescriptor`. This is intentional for v1 (matches resource availability) but visually differs from CMP mode, which uses proper weight-shaped glyphs the Compose font system ships. If/when additional Manrope TTFs land, add them to `Resources/` and `UIAppFonts`, and map each SwiftUI weight to the right PostScript name in `NativeDemoTheme.manrope`.
- **Shared Money across CMP/Native.** The spec explicitly scopes this out of v1. If a future iteration wants it, the bridge would be an `@ObservableObject` in Swift that the Kotlin side writes through via a setter exposed on `MainViewController(...)`. Not a v1 concern.
- **AppIcon.** We ship an empty appiconset. Xcode warns on build; it's a cosmetic issue, not a functional one. Copy the 1024-px icon from `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` before deleting `iosApp/` if we want to preserve it — worth a one-off commit at the very start of Task 14 if desired. Left as a tracked follow-up rather than a required step.
