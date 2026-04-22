# Camount-JS — Design

**Date:** 2026-04-21
**Status:** Draft
**Target version:** `0.9.1` (shared with the Kotlin and Swift release tag `v0.9.1`)

## Goal

Add a first-class web port of Camount — a pure-TypeScript library published to
npm as `@yuridenison/camount`. The library must match the behavior and visual
language of the existing Compose Multiplatform and Swift ports:

- Per-character stack animations (vertical scroll between digit values)
- Field-aware styling (integer / fraction / currency symbol)
- Cursor, gradient fills, placeholder coloring
- Material FastOutSlowIn easing
- A real input pipeline (`AmountField` equivalent)

Consumers install it with:

```bash
npm install @yuridenison/camount
```

And use it either as a framework-agnostic Web Component:

```html
<camount-text amount="1234.56" currency="USD"></camount-text>
<camount-field currency="USD"></camount-field>
```

…or as a React component:

```tsx
import { AmountText, AmountField } from "@yuridenison/camount/react";
<AmountText amount={1234.56} currency="USD" />
```

## Non-Goals

- Reusing the Kotlin/Wasm build of `:camount` as the web library. The Wasm
  bundle drags in the Compose runtime and Skia and is not consumable as a small
  npm package.
- Canvas or WebGL rendering. DOM + CSS transforms are sufficient and far more
  accessible / debuggable.
- Vue, Svelte, Angular, or Solid adapters in v1. The Web Component already
  works in all of them; a React wrapper is the only explicit framework layer
  shipped in v1.
- Server-side rendering with animation state preservation. The component
  mounts and animates client-side only.
- A separate repo. `camount-js` lives in this monorepo under `camount-js/`.

## Coordinates

| Aspect | Value |
|---|---|
| npm package | `@yuridenison/camount` |
| Version | `0.9.1` (shared with Kotlin/Swift) |
| Language | TypeScript (strict) |
| Build | `tsup` — ESM + CJS + `.d.ts` |
| Tests | `vitest` + `happy-dom` |
| Node | 20.x (for CI / dev) |
| Browser targets | Evergreen (Chrome/Edge/Firefox/Safari last 2) |
| Entry points | `.` (core + web component), `./react` (React wrapper) |
| Peer dependency | `react >=18 <20` (optional, only for `./react`) |
| Repo path | `camount-js/` at repo root |
| Release tag | `v0.9.1` — same tag that releases Kotlin + Swift |

## Approach

**Three-layer architecture**, mirroring the Swift port and Compose common code:

1. **Core** (`src/core/`) — pure TypeScript, no DOM. Types, formatting, input
   sanitization, diff calculation, currency info. Directly portable from
   `camount-swift/Sources/Camount/Core/` and from the `commonMain` of
   `:camount`.
2. **Rendering** (`src/rendering/`) — framework-agnostic DOM + CSS. Builds the
   glyph stacks, drives per-character enter/exit animations via CSS transforms
   + `transitionend`, manages the cursor, applies field colors. No React, no
   Web Components at this layer — pure `HTMLElement` + CSS.
3. **Framework** (`src/elements/`, `src/react/`) — thin wrappers:
   - `elements/` defines `<camount-text>` and `<camount-field>` custom
     elements that own a rendering instance and map attributes/properties to
     it.
   - `react/` defines `<AmountText>` and `<AmountField>` React components that
     wrap the custom elements (so React state flows through attributes /
     refs).

This decomposition mirrors `camount-swift`'s Core / Rendering / SwiftUI split.
Tests live alongside in `src/**/__tests__` or `tests/`, organized to match.

### Why DOM + CSS (not Canvas)

Evaluated two approaches:

- **A.** Canvas: full pixel control, matches Skia rendering 1:1, but breaks
  copy/paste/a11y/text selection and needs custom focus handling for the
  input field.
- **B.** DOM + CSS (chosen): one stacked `<span>` column per character,
  `translateY` animates the active digit, `transition` provides easing.
  Native focus handling, native accessibility, smaller bundle, inspectable
  in DevTools.

DOM + CSS is the established pattern for this effect on the web and aligns
with how Compose/SwiftUI already decompose the animation (one layer per
glyph, each with its own transform). The per-character stack compiles to a
small DOM: `N` character columns × 11 rows (0–9 + blank) + a cursor span.

### Why Web Component + React wrapper (not React-only)

- **Web Component** gives framework-agnostic distribution — works in any HTML
  page, Vue, Svelte, Angular, plain JS — without a framework runtime.
- **React wrapper** exists because React doesn't pass non-string properties
  through attributes cleanly; a tiny wrapper exposes proper props typing and
  uses a ref to set properties.
- No other framework wrappers in v1. They can be added later without
  restructuring the package.

## Package Layout

```
camount-js/
  package.json
  tsconfig.json
  tsup.config.ts
  vitest.config.ts
  README.md
  src/
    index.ts                    # barrel: core + elements
    react.ts                    # barrel: React components
    core/
      types.ts                  # AmountConfig, Field, Money, etc.
      currencyInfo.ts           # port of CurrencyInfo.swift
      sanitizeInput.ts          # port of SanitizeInput.swift
      formatter.ts              # port of AmountFormatter.swift
      diffCalculator.ts         # port of DiffCalculator.swift
      index.ts
    rendering/
      painter.ts                # port of AmountPainter.swift
      hostView.ts               # port of AmountHostView.swift
      cellLayer.ts              # port of AmountCellLayer.swift
      symbolCell.ts             # port of SymbolCell.swift
      cursorCell.ts             # port of CursorCell.swift
      glyphCache.ts             # port of GlyphCache.swift
      styles.css                # keyframes, easing vars, base styles
      index.ts
    elements/
      amountText.ts             # <camount-text>
      amountField.ts            # <camount-field>
      index.ts
    react/
      AmountText.tsx
      AmountField.tsx
      index.ts
    tests/ (or colocated __tests__/)
      core.test.ts
      rendering.test.ts
      elements.test.ts
      react.test.tsx
```

### `package.json` — key fields

```jsonc
{
  "name": "@yuridenison/camount",
  "version": "0.9.1",
  "type": "module",
  "sideEffects": ["./dist/elements/*.js", "./dist/index.js"],
  "exports": {
    ".": {
      "types": "./dist/index.d.ts",
      "import": "./dist/index.js",
      "require": "./dist/index.cjs"
    },
    "./react": {
      "types": "./dist/react.d.ts",
      "import": "./dist/react.js",
      "require": "./dist/react.cjs"
    }
  },
  "files": ["dist", "README.md"],
  "peerDependencies": {
    "react": ">=18 <20"
  },
  "peerDependenciesMeta": {
    "react": { "optional": true }
  },
  "devDependencies": {
    "typescript": "^5.6.0",
    "tsup": "^8.3.0",
    "vitest": "^2.1.0",
    "happy-dom": "^15.0.0",
    "@types/react": "^18.3.0",
    "react": "^18.3.0"
  },
  "scripts": {
    "build": "tsup",
    "test": "vitest run",
    "typecheck": "tsc --noEmit"
  }
}
```

`sideEffects` includes the custom-element registration entry so bundlers
(webpack, Vite, Rollup) do not tree-shake the `customElements.define()` calls.
React is an optional peer — consumers using only the Web Component pay no
React cost.

### `tsup.config.ts` — two entry points

```ts
import { defineConfig } from "tsup";

export default defineConfig({
  entry: {
    index: "src/index.ts",
    react: "src/react.ts",
  },
  format: ["esm", "cjs"],
  dts: true,
  clean: true,
  sourcemap: true,
  target: "es2022",
});
```

## Core layer details

The Core layer is a direct port from the Swift `Core/` folder. Every Swift
file maps to one TypeScript file with the same responsibility:

| Swift | TypeScript | Notes |
|---|---|---|
| `Money.swift` | `core/types.ts` (`Money` type) | Uses `bigint` for integer minor units; precision preserved |
| `AmountConfig.swift` | `core/types.ts` (`AmountConfig`) | Discriminated union for `mode: "text" \| "field"` |
| `FieldRange.swift` | `core/types.ts` (`Field`, `FieldRange`) | Matches Swift enum |
| `CurrencyInfo.swift` | `core/currencyInfo.ts` | Decimal places + symbol lookup (ISO 4217 subset shared with Swift) |
| `SanitizeInput.swift` | `core/sanitizeInput.ts` | Keeps digits + one decimal separator, clamps to currency precision |
| `AmountFormatter.swift` | `core/formatter.ts` | Produces the displayed cell sequence from an `AmountConfig` |

Core has zero runtime dependencies. All functions are pure and synchronous.

## Rendering layer details

The Rendering layer owns a single `HTMLElement` (the host) and fills it with
glyph-stack columns. Each column is a `<span class="camount-cell">` containing
11 `<span class="camount-glyph">` children (0, 1, …, 9, blank). Active glyph
is selected by translating the column on `--y`:

```css
.camount-cell {
  display: inline-block;
  overflow: hidden;
  height: 1em;
  line-height: 1em;
}
.camount-stack {
  display: block;
  transform: translateY(var(--y, 0));
  transition: transform 300ms cubic-bezier(0.4, 0.0, 0.2, 1); /* FastOutSlowIn */
}
.camount-glyph {
  display: block;
  height: 1em;
}
```

`AmountPainter` (port of `AmountPainter.swift`) consumes two formatter outputs
(previous and current) plus a `DiffCalculator` result (`added`, `removed`,
`moved` indices) and mutates the DOM: creates/removes columns and updates each
column's `--y` to animate to the new digit. `DiffCalculator` mirrors the
Swift implementation exactly (LCS-based).

`AmountHostView` is the top-level `HTMLElement` manager — takes an
`AmountConfig`, owns the painter, exposes `update(config)`. `AmountField`
(custom element) layers input handling on top: a hidden `<input>` captures
keystrokes, sanitization runs through `core/sanitizeInput`, and the painter
re-renders with cursor position.

Rendering depends only on Core. Styles are delivered via the DOM: the custom
elements attach a shadow root and adopt a `CSSStyleSheet` (via
`adoptedStyleSheets`) containing the rules below. This keeps styles scoped
per-element, avoids a separate `styles.css` shipping path, and means
consumers don't have to import any CSS file manually. The stylesheet text
lives in `rendering/styles.ts` as a template literal.

## Framework layer details

### Web Components (`src/elements/`)

`amountText.ts`:

```ts
export class CamountTextElement extends HTMLElement {
  static observedAttributes = ["amount", "currency", "style-hint"];
  private host: AmountHostView | null = null;

  connectedCallback() {
    this.host = new AmountHostView(this, this.readConfig());
  }
  attributeChangedCallback() {
    this.host?.update(this.readConfig());
  }
  disconnectedCallback() {
    this.host?.dispose();
  }
  private readConfig(): AmountConfig { /* ... */ }
}

if (typeof customElements !== "undefined" &&
    !customElements.get("camount-text")) {
  customElements.define("camount-text", CamountTextElement);
}
```

`amountField.ts` follows the same pattern but wraps the input pipeline.

### React (`src/react/`)

`AmountText.tsx`:

```tsx
import "../elements/amountText"; // side-effect import registers the element
import { forwardRef } from "react";

interface AmountTextProps {
  amount: number | string;
  currency: string;
  styleHint?: string;
}

export const AmountText = forwardRef<HTMLElement, AmountTextProps>(
  ({ amount, currency, styleHint }, ref) => (
    <camount-text
      ref={ref}
      amount={String(amount)}
      currency={currency}
      style-hint={styleHint}
    />
  ),
);
```

Plus a TypeScript declaration merge so JSX accepts `<camount-text>`.

## Testing strategy

- **Core** — pure-function tests in `happy-dom` (really just node). Mirrors
  the cases in `AmountConfigTests.swift`, `AmountFormatterTests.swift`,
  `CurrencyInfoTests.swift`, `SanitizeInputTests.swift`, `DiffCalculatorTests.swift`,
  `MoneyTests.swift` — same inputs, same expected outputs, ensuring cross-port
  parity.
- **Rendering** — uses `happy-dom` to mount a detached `HTMLElement`, invoke
  `AmountHostView.update()`, and assert on the resulting DOM structure and
  CSS custom properties. Animations are verified by state after
  `transitionend` (dispatched synchronously in tests).
- **Elements** — mount `<camount-text>` via `document.createElement`, set
  attributes, assert rendered output.
- **React** — `vitest` + React Testing Library (light usage) to verify the
  wrapper forwards props to the underlying element.

## CI — `.github/workflows/js.yml`

```yaml
name: JS CI

on:
  push:
    branches: [main]
    paths:
      - 'camount-js/**'
      - '.github/workflows/js.yml'
  pull_request:
    paths:
      - 'camount-js/**'
      - '.github/workflows/js.yml'
  workflow_dispatch: {}

jobs:
  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: camount-js
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: camount-js/package-lock.json
      - run: npm ci
      - run: npm run typecheck
      - run: npm test
      - run: npm run build
```

## Release — `.github/workflows/publish-js.yml`

Triggered on the same `v*` tag used by Maven Central and Swift. Publishes to
npm when a matching `camount-js/package.json` version is present.

```yaml
name: Publish NPM

on:
  push:
    tags: ['v*']
  workflow_dispatch: {}

jobs:
  publish:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: camount-js
    permissions:
      contents: read
      id-token: write  # for npm provenance
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          registry-url: 'https://registry.npmjs.org'
      - run: npm ci
      - run: npm run build
      - run: npm publish --access public --provenance
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

Required repository secret: `NPM_TOKEN` (an npm automation token with
publish rights to `@yuridenison/camount`).

The `v0.9.1` tag therefore triggers three publications in parallel:

1. Maven Central (via `publish.yml`)
2. SwiftPM (implicit — resolved from the tag)
3. npm (via `publish-js.yml`)

## Verification

Implementation is complete when all of the following hold:

1. `npm run typecheck` passes with `strict: true`.
2. `npm test` passes — all ported tests from Swift's `CamountTests/` have JS
   equivalents with identical expected outputs.
3. `npm run build` produces a `dist/` containing `index.{js,cjs,d.ts}` and
   `react.{js,cjs,d.ts}`.
4. A manual smoke HTML page (not shipped) renders `<camount-text>` and
   animates between amounts.
5. A manual smoke React app renders `<AmountText>` and animates between amounts.
6. The JS CI workflow is present and passes on PR.
7. The JS publish workflow is present with a `v*` trigger and
   `NODE_AUTH_TOKEN` wiring.
8. `package.json` version is `0.9.1`, matching Kotlin and Swift.

## Risks and Open Questions

- **npm scope ownership.** `@yuridenison` must exist on npm and the automation
  token must have publish rights. If the scope is not yet claimed, the
  post-implementation manual step is "create the scope and issue a token".
- **React peer dep range.** React 19 changed JSX type handling for custom
  elements. The declaration merge in `src/react/index.ts` must cover both 18
  and 19. If it turns out React 19 requires a dedicated path, we add
  `"./react19"` export in a follow-up — not v1.
- **Bundle size.** Target: under 10 KB min+gzip for the core + element
  entry. `tsup`'s default minifier (esbuild) should hit this given the
  algorithmic simplicity. If it doesn't, we revisit tree-shaking of the
  currency table.
- **Custom-element names.** `<camount-text>` and `<camount-field>` — global
  names. Unlikely to collide, but consumers embedding in a page that also
  registers these names will double-define. Our `customElements.define` call
  guards with `!customElements.get("camount-text")` to avoid throwing.
- **Animation timing in tests.** `happy-dom` does not actually run CSS
  transitions. Tests that assert animated state must dispatch synthetic
  `transitionend` events rather than relying on time. This is called out in
  the test helpers.
- **Shared tag semantics.** If Kotlin or Swift ever needs a point release
  without JS (or vice versa), the escape hatch is a platform-prefixed tag
  (e.g. `js-0.9.2`). The `publish-js.yml` workflow can be extended to match
  `js-*` later. Out of scope for v1.

## Post-implementation manual steps

Out of scope for the automated work; listed for tracking:

1. Create the `@yuridenison` scope on npm (if not already claimed).
2. Generate an npm automation token and add it as the `NPM_TOKEN` repository
   secret.
3. Cut and push tag `v0.9.1`. This triggers Maven Central, SwiftPM (implicit),
   and npm publish workflows.
4. Verify the package on npmjs.com and install it in a scratch project to
   confirm the exports resolve.
