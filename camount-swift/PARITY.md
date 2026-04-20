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

Field-tag enum (Swift: `AmountFieldKind`, Kotlin: `AmountField`) members:
`fixedFraction`, `zeroNotation`, `currencySuffix`. Kotlin's
`AmountFieldPositions` exposes `fixedFraction` and `zeroNotation` ranges plus
cursor position. Swift uses the same structure.

**Swift naming divergence:** the internal field-tag enum is named
`AmountFieldKind` to avoid clashing with the public `AmountField` SwiftUI
view. In Kotlin the two live in different packages.

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
- **Per-symbol stacked-fade animation driver** uses a main-thread
  `CADisplayLink` rather than Core Animation off-thread. If profiling
  shows frame drops, refactor to one sublayer per stacked symbol with
  independent `opacity` animations running on the render server.
- **Gradient text rendering** — `AmountStyle.gradient` is captured for API
  parity with Kotlin but not rendered yet. Requires a `CAGradientLayer`
  mask; deferred to v1.1.
