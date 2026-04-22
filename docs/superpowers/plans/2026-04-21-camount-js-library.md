# Camount-JS Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `@yuridenison/camount` on npm — a TypeScript port of the Camount widget with DOM+CSS rendering, framework-agnostic Web Components, and a React wrapper — sharing the `v0.9.1` release tag with the Kotlin and Swift ports.

**Architecture:** Three layers matching the Swift port 1:1 — **Core** (pure TS: types, formatter, sanitizer, diff), **Rendering** (framework-agnostic DOM+CSS: host, painter, symbol cell, cursor, diff), **Framework** (`<camount-text>`/`<camount-field>` Web Components + a React wrapper). Per-character stack animations use `translateY` + CSS transitions with Material FastOutSlowIn easing.

**Tech Stack:** TypeScript 5.6 (strict), `tsup` for dual ESM+CJS build, `vitest` + `happy-dom` for tests, Custom Elements v1, optional React 18/19 peer. Target ES2022, Node 20 for tooling/CI.

---

## File Structure

```
camount-js/
  .gitignore
  package.json
  tsconfig.json
  tsup.config.ts
  vitest.config.ts
  README.md
  src/
    index.ts                 # barrel: core + elements (side-effect registers elements)
    react.ts                 # barrel: React components
    core/
      types.ts               # Money, AmountConfig, Field, FieldRange, AmountFieldPositions, SanitizedInput
      currencyInfo.ts        # Intl.NumberFormat-based currency metadata
      sanitizeInput.ts       # input pipeline sanitizer
      formatter.ts           # AmountFormatter — stateful, single-threaded
      diffCalculator.ts      # EditDiffCalculator + LevenshteinDiffCalculator
      index.ts               # core barrel
    rendering/
      styles.ts              # CSS template literal + CSSStyleSheet factory
      symbolCell.ts          # per-character stack DOM + transitions
      cursorCell.ts          # cursor element
      painter.ts             # orchestrates cells + cursor + layout
      host.ts                # AmountHost: public class driving painter + input pipeline
      index.ts               # rendering barrel
    elements/
      amountText.ts          # <camount-text>
      amountField.ts         # <camount-field>
      index.ts               # elements barrel (side-effect registers)
    react/
      AmountText.tsx
      AmountField.tsx
      jsx.d.ts               # JSX IntrinsicElements declaration merge
      index.ts               # react barrel
  tests/
    core/
      money.test.ts
      amountConfig.test.ts
      currencyInfo.test.ts
      sanitizeInput.test.ts
      formatter.test.ts
      diffCalculator.test.ts
    rendering/
      symbolCell.test.ts
      host.test.ts
    elements/
      amountText.test.ts
      amountField.test.ts
    react/
      AmountText.test.tsx
```

---

## Task 1: Scaffold camount-js package

**Files:**
- Create: `camount-js/.gitignore`
- Create: `camount-js/package.json`
- Create: `camount-js/tsconfig.json`
- Create: `camount-js/tsup.config.ts`
- Create: `camount-js/vitest.config.ts`

- [ ] **Step 1: Create camount-js/.gitignore**

```
node_modules/
dist/
coverage/
*.log
.DS_Store
```

- [ ] **Step 2: Create camount-js/package.json**

```json
{
  "name": "@yuridenison/camount",
  "version": "0.9.1",
  "description": "Animated currency/amount formatter widget — per-character stack animations, field-aware styling, Web Component + React.",
  "type": "module",
  "sideEffects": [
    "./dist/index.js",
    "./dist/index.cjs",
    "./dist/react.js",
    "./dist/react.cjs",
    "./src/index.ts",
    "./src/react.ts",
    "./src/elements/**"
  ],
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
  "main": "./dist/index.cjs",
  "module": "./dist/index.js",
  "types": "./dist/index.d.ts",
  "files": ["dist", "README.md"],
  "repository": {
    "type": "git",
    "url": "git+https://github.com/yuridenison/camount.git",
    "directory": "camount-js"
  },
  "homepage": "https://github.com/yuridenison/camount",
  "bugs": "https://github.com/yuridenison/camount/issues",
  "author": "Yuri Denison <yuri.denison@gmail.com>",
  "license": "Apache-2.0",
  "keywords": ["amount", "currency", "formatter", "animation", "web-component", "react"],
  "peerDependencies": {
    "react": ">=18 <20"
  },
  "peerDependenciesMeta": {
    "react": { "optional": true }
  },
  "devDependencies": {
    "@types/react": "^18.3.0",
    "happy-dom": "^15.0.0",
    "react": "^18.3.0",
    "tsup": "^8.3.0",
    "typescript": "^5.6.0",
    "vitest": "^2.1.0"
  },
  "scripts": {
    "build": "tsup",
    "test": "vitest run",
    "test:watch": "vitest",
    "typecheck": "tsc --noEmit"
  },
  "engines": {
    "node": ">=20"
  }
}
```

- [ ] **Step 3: Create camount-js/tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "jsx": "react-jsx",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitOverride": true,
    "noFallthroughCasesInSwitch": true,
    "exactOptionalPropertyTypes": false,
    "forceConsistentCasingInFileNames": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "esModuleInterop": true,
    "isolatedModules": true,
    "skipLibCheck": true,
    "resolveJsonModule": true,
    "outDir": "dist",
    "rootDir": "."
  },
  "include": ["src", "tests"]
}
```

- [ ] **Step 4: Create camount-js/tsup.config.ts**

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
  treeshake: true,
});
```

- [ ] **Step 5: Create camount-js/vitest.config.ts**

```ts
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "happy-dom",
    include: ["tests/**/*.test.{ts,tsx}"],
    globals: false,
  },
});
```

- [ ] **Step 6: Install dependencies**

Run: `cd camount-js && npm install`
Expected: `package-lock.json` created, `node_modules/` populated, no install errors.

- [ ] **Step 7: Verify tooling**

Run: `cd camount-js && npx tsc --noEmit --listFiles | head -5`
Expected: tsc exits 0 (no files to compile yet but config is valid).

---

## Task 2: Core types

**Files:**
- Create: `camount-js/src/core/types.ts`
- Create: `camount-js/tests/core/money.test.ts`
- Create: `camount-js/tests/core/amountConfig.test.ts`

- [ ] **Step 1: Write the failing Money tests**

Create `camount-js/tests/core/money.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { Money, moneyEquals, moneyIsZero, moneyIsPositive, moneyCompare, moneyAbsoluteUnits, moneyAbsoluteNanos } from "../../src/core/types";

describe("Money", () => {
  it("isZero", () => {
    expect(moneyIsZero({ units: 0n, nanos: 0, currencyCode: "USD" })).toBe(true);
    expect(moneyIsZero({ units: 1n, nanos: 0, currencyCode: "USD" })).toBe(false);
    expect(moneyIsZero({ units: 0n, nanos: 1, currencyCode: "USD" })).toBe(false);
  });

  it("isPositive", () => {
    expect(moneyIsPositive({ units: 1n, nanos: 0, currencyCode: "USD" })).toBe(true);
    expect(moneyIsPositive({ units: 0n, nanos: 1, currencyCode: "USD" })).toBe(true);
    expect(moneyIsPositive({ units: 0n, nanos: 0, currencyCode: "USD" })).toBe(false);
    expect(moneyIsPositive({ units: -1n, nanos: 0, currencyCode: "USD" })).toBe(false);
    expect(moneyIsPositive({ units: 0n, nanos: -1, currencyCode: "USD" })).toBe(false);
  });

  it("compare", () => {
    const a: Money = { units: 1n, nanos: 0, currencyCode: "USD" };
    const b: Money = { units: 1n, nanos: 500_000_000, currencyCode: "USD" };
    const c: Money = { units: 2n, nanos: 0, currencyCode: "USD" };
    expect(moneyCompare(a, b)).toBeLessThan(0);
    expect(moneyCompare(b, c)).toBeLessThan(0);
    expect(moneyEquals(a, { units: 1n, nanos: 0, currencyCode: "USD" })).toBe(true);
  });

  it("absolute helpers", () => {
    expect(moneyAbsoluteUnits({ units: -5n, nanos: 0, currencyCode: "USD" })).toBe(5n);
    expect(moneyAbsoluteNanos({ units: 0n, nanos: -7, currencyCode: "USD" })).toBe(7);
  });
});
```

- [ ] **Step 2: Write the failing AmountConfig tests**

Create `camount-js/tests/core/amountConfig.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import {
  AmountConfig,
  configIsDigit,
  configIsZero,
  configIsInputSeparator,
  configIsDecimalSeparator,
  configIsGroupingSeparator,
  configMaximumFormattedSymbols,
  configDigitAt,
  configZero,
} from "../../src/core/types";

const euro = (maxNotation = 5): AmountConfig => ({
  maximumNotationDigits: maxNotation,
  decimalSeparator: ",",
  groupingSeparator: " ",
  prefix: "",
  suffix: " €",
  groupingSize: 3,
  maximumFractionDigits: 2,
});

describe("AmountConfig", () => {
  it("digit predicate", () => {
    const c = euro();
    for (const ch of "0123456789") expect(configIsDigit(c, ch)).toBe(true);
    for (const ch of "abc,. €") expect(configIsDigit(c, ch)).toBe(false);
  });

  it("zero is first digit", () => {
    const c = euro();
    expect(configZero(c)).toBe("0");
    expect(configIsZero(c, "0")).toBe(true);
    expect(configIsZero(c, "1")).toBe(false);
  });

  it("input separator", () => {
    const c = euro();
    expect(configIsInputSeparator(c, ".")).toBe(true);
    expect(configIsInputSeparator(c, ",")).toBe(true);
    expect(configIsInputSeparator(c, " ")).toBe(false);
  });

  it("decimal separator", () => {
    const c = euro();
    expect(configIsDecimalSeparator(c, ",")).toBe(true);
    expect(configIsDecimalSeparator(c, ".")).toBe(false);
  });

  it("grouping separator", () => {
    const c = euro();
    expect(configIsGroupingSeparator(c, " ")).toBe(true);
    expect(configIsGroupingSeparator(c, ",")).toBe(false);
  });

  it("grouping separator false when groupingSize zero", () => {
    const c: AmountConfig = { ...euro(), groupingSize: 0, groupingSeparator: "," };
    expect(configIsGroupingSeparator(c, ",")).toBe(false);
  });

  it("maximum formatted symbols", () => {
    expect(configMaximumFormattedSymbols(euro())).toBe(11);
  });

  it("digit at index", () => {
    expect(configDigitAt(euro(), 0)).toBe("0");
    expect(configDigitAt(euro(), 9)).toBe("9");
  });
});
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `cd camount-js && npm test`
Expected: FAIL — module `../../src/core/types` not found.

- [ ] **Step 4: Create src/core/types.ts**

```ts
// Money — integer units + 9-digit nano fraction, matching Swift and Compose ports.
export interface Money {
  readonly units: bigint;
  readonly nanos: number;
  readonly currencyCode: string;
}

export function moneyZero(currencyCode: string): Money {
  return { units: 0n, nanos: 0, currencyCode };
}

export function moneyEquals(a: Money, b: Money): boolean {
  return a.units === b.units && a.nanos === b.nanos && a.currencyCode === b.currencyCode;
}

export function moneyIsZero(m: Money): boolean {
  return m.units === 0n && m.nanos === 0;
}

export function moneyIsPositive(m: Money): boolean {
  return m.units > 0n || (m.units === 0n && m.nanos > 0);
}

export function moneyCompare(a: Money, b: Money): number {
  if (a.units !== b.units) return a.units < b.units ? -1 : 1;
  return a.nanos - b.nanos;
}

export function moneyAbsoluteUnits(m: Money): bigint {
  return m.units < 0n ? -m.units : m.units;
}

export function moneyAbsoluteNanos(m: Money): number {
  return m.nanos < 0 ? -m.nanos : m.nanos;
}

// AmountConfig — immutable plain object; helpers are free functions.
export interface AmountConfig {
  readonly maximumNotationDigits: number;
  readonly decimalSeparator: string; // single character
  readonly groupingSeparator: string;
  readonly prefix: string;
  readonly suffix: string;
  readonly groupingSize: number;
  readonly maximumFractionDigits: number;
}

const DIGITS: ReadonlyArray<string> = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"];

export function configZero(_config: AmountConfig): string {
  return DIGITS[0]!;
}

export function configDigitAt(_config: AmountConfig, index: number): string {
  const d = DIGITS[index];
  if (d === undefined) throw new RangeError(`digit index ${index} out of range`);
  return d;
}

export function configIsDigit(_config: AmountConfig, c: string): boolean {
  return c.length === 1 && c >= "0" && c <= "9";
}

export function configIsZero(_config: AmountConfig, c: string): boolean {
  return c === "0";
}

export function configIsInputSeparator(_config: AmountConfig, c: string): boolean {
  return c === "." || c === ",";
}

export function configIsDecimalSeparator(config: AmountConfig, c: string): boolean {
  return c === config.decimalSeparator;
}

export function configIsGroupingSeparator(config: AmountConfig, c: string): boolean {
  return config.groupingSize > 0 && c === config.groupingSeparator;
}

export function configMaximumFormattedSymbols(config: AmountConfig): number {
  const groupingSeparators =
    config.groupingSize === 0
      ? 0
      : Math.floor((config.maximumNotationDigits - 1) / config.groupingSize);
  return (
    config.prefix.length +
    config.maximumNotationDigits +
    groupingSeparators +
    1 +
    config.maximumFractionDigits +
    config.suffix.length
  );
}

// Field classification used by the renderer to apply field-specific colors.
export type Field = "fixedFraction" | "zeroNotation" | "currencySuffix";

export interface FieldRange {
  beginIndex: number;
  endIndex: number;
}

export function fieldRangeEmpty(): FieldRange {
  return { beginIndex: 0, endIndex: 0 };
}

export function fieldRangeIsValid(r: FieldRange): boolean {
  return r.beginIndex < r.endIndex && r.beginIndex >= 0;
}

export function fieldRangeLength(r: FieldRange): number {
  return fieldRangeIsValid(r) ? r.endIndex - r.beginIndex : 0;
}

export function fieldRangeContains(r: FieldRange, index: number): boolean {
  return index >= r.beginIndex && index < r.endIndex;
}

export function fieldRangeOffset(r: FieldRange, value: number): FieldRange {
  if (!fieldRangeIsValid(r)) return r;
  return { beginIndex: r.beginIndex + value, endIndex: r.endIndex + value };
}

export interface AmountFieldPositions {
  readonly cursorPosition: number;
  readonly fixedFraction: FieldRange;
  readonly zeroNotation: FieldRange;
}

export const EMPTY_POSITIONS: AmountFieldPositions = {
  cursorPosition: -1,
  fixedFraction: fieldRangeEmpty(),
  zeroNotation: fieldRangeEmpty(),
};

export interface SanitizedInput {
  readonly text: string;
  readonly cursor: number;
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd camount-js && npm test`
Expected: PASS for both `money.test.ts` and `amountConfig.test.ts`.

- [ ] **Step 6: Commit skipped (session rule: do not commit)**

---

## Task 3: Core — CurrencyInfo

**Files:**
- Create: `camount-js/src/core/currencyInfo.ts`
- Create: `camount-js/tests/core/currencyInfo.test.ts`

- [ ] **Step 1: Write the failing tests**

Create `camount-js/tests/core/currencyInfo.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { currencyInfoFor, amountConfigForCurrency, sanitizeBidi } from "../../src/core/currencyInfo";

describe("CurrencyInfo", () => {
  it("USD has non-empty prefix and 2 fraction digits", () => {
    const info = currencyInfoFor("USD");
    expect(info.maximumFractionDigits).toBe(2);
    expect(info.prefix.length > 0 || info.suffix.length > 0).toBe(true);
  });

  it("JPY has 0 fraction digits", () => {
    const info = currencyInfoFor("JPY");
    expect(info.maximumFractionDigits).toBe(0);
  });

  it("unknown currency still returns values", () => {
    const info = currencyInfoFor("ZZZ");
    expect(info.decimalSeparator.length).toBe(1);
    expect(info.groupingSeparator.length).toBe(1);
  });

  it("sanitizeBidi strips control chars", () => {
    expect(sanitizeBidi("‎$‪")).toBe("$");
  });

  it("amountConfigForCurrency wires into AmountConfig", () => {
    const config = amountConfigForCurrency("USD", 6);
    expect(config.maximumNotationDigits).toBe(6);
    expect(config.maximumFractionDigits).toBe(2);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd camount-js && npm test -- currencyInfo`
Expected: FAIL — module not found.

- [ ] **Step 3: Create src/core/currencyInfo.ts**

```ts
import type { AmountConfig } from "./types";

export interface CurrencyInfo {
  readonly decimalSeparator: string;
  readonly groupingSeparator: string;
  readonly prefix: string;
  readonly suffix: string;
  readonly groupingSize: number;
  readonly maximumFractionDigits: number;
}

const BIDI_CONTROL = new Set<number>([
  0x200e, 0x200f,
  0x202a, 0x202b, 0x202c, 0x202d, 0x202e,
  0x2066, 0x2067, 0x2068, 0x2069,
]);

export function sanitizeBidi(s: string): string {
  let out = "";
  for (const ch of s) {
    const cp = ch.codePointAt(0);
    if (cp !== undefined && BIDI_CONTROL.has(cp)) continue;
    out += ch;
  }
  return out;
}

// Probe Intl.NumberFormat to derive currency metadata. We use an extreme
// sample (1,234,567.89) to discover the decimal + grouping separators, prefix,
// suffix, and grouping size in one shot.
export function currencyInfoFor(currencyCode: string): CurrencyInfo {
  const maximumFractionDigits = fractionDigitsFor(currencyCode);
  let fmt: Intl.NumberFormat;
  try {
    fmt = new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: currencyCode,
      currencyDisplay: "symbol",
      minimumFractionDigits: maximumFractionDigits,
      maximumFractionDigits,
      useGrouping: true,
    });
  } catch {
    return {
      decimalSeparator: ".",
      groupingSeparator: ",",
      prefix: "",
      suffix: "",
      groupingSize: 3,
      maximumFractionDigits: 2,
    };
  }

  const parts = fmt.formatToParts(1234567.89);
  let decimal = ".";
  let grouping = ",";
  let prefix = "";
  let suffix = "";
  let seenInteger = false;
  let firstGroupLen = 0;
  let secondGroupLen = 0;
  let groupIdx = 0;
  for (const p of parts) {
    switch (p.type) {
      case "currency":
      case "literal":
      case "plusSign":
      case "minusSign": {
        const sanitized = sanitizeBidi(p.value);
        if (!seenInteger) prefix += sanitized;
        else suffix += sanitized;
        break;
      }
      case "integer": {
        seenInteger = true;
        if (groupIdx === 0) firstGroupLen = p.value.length;
        else if (groupIdx === 1) secondGroupLen = p.value.length;
        groupIdx += 1;
        break;
      }
      case "group":
        grouping = p.value.length === 1 ? p.value : grouping;
        break;
      case "decimal":
        decimal = p.value.length === 1 ? p.value : decimal;
        break;
      case "fraction":
      default:
        break;
    }
  }

  const groupingSize = secondGroupLen > 0 ? secondGroupLen : firstGroupLen > 0 ? firstGroupLen : 3;

  return {
    decimalSeparator: decimal,
    groupingSeparator: grouping,
    prefix,
    suffix,
    groupingSize,
    maximumFractionDigits,
  };
}

function fractionDigitsFor(currencyCode: string): number {
  try {
    const opts = new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: currencyCode,
    }).resolvedOptions();
    if (typeof opts.maximumFractionDigits === "number") return opts.maximumFractionDigits;
    return 2;
  } catch {
    return 2;
  }
}

export function amountConfigForCurrency(
  currencyCode: string,
  maximumNotationDigits: number,
): AmountConfig {
  const info = currencyInfoFor(currencyCode);
  return {
    maximumNotationDigits,
    decimalSeparator: info.decimalSeparator,
    groupingSeparator: info.groupingSeparator,
    prefix: info.prefix,
    suffix: info.suffix,
    groupingSize: info.groupingSize,
    maximumFractionDigits: info.maximumFractionDigits,
  };
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd camount-js && npm test -- currencyInfo`
Expected: PASS. Note: `happy-dom` provides `Intl` natively via Node; no polyfill needed.

---

## Task 4: Core — sanitizeInput

**Files:**
- Create: `camount-js/src/core/sanitizeInput.ts`
- Create: `camount-js/tests/core/sanitizeInput.test.ts`

- [ ] **Step 1: Write the failing tests**

Create `camount-js/tests/core/sanitizeInput.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import type { AmountConfig } from "../../src/core/types";
import { sanitizeInput } from "../../src/core/sanitizeInput";

const usd: AmountConfig = {
  maximumNotationDigits: 5,
  decimalSeparator: ".",
  groupingSeparator: ",",
  prefix: "",
  suffix: "",
  groupingSize: 3,
  maximumFractionDigits: 2,
};

describe("sanitizeInput", () => {
  it("strips letters", () => {
    const result = sanitizeInput("1a2b3", 5, usd);
    expect(result.text).toBe("123");
    expect(result.cursor).toBe(3);
  });

  it("enforces max notation", () => {
    const result = sanitizeInput("123456", 6, usd);
    expect(result.text).toBe("12345");
    expect(result.cursor).toBe(5);
  });

  it("enforces max fraction", () => {
    const result = sanitizeInput("1.234", 5, usd);
    expect(result.text).toBe("1.23");
    expect(result.cursor).toBe(4);
  });

  it("only one separator", () => {
    const result = sanitizeInput("1.2.3", 5, usd);
    expect(result.text).toBe("1.23");
    expect(result.cursor).toBe(4);
  });

  it("comma mapped to decimal separator", () => {
    const result = sanitizeInput("1,2", 3, usd);
    expect(result.text).toBe("1.2");
  });
});
```

- [ ] **Step 2: Run to verify failure**

Run: `cd camount-js && npm test -- sanitizeInput`
Expected: FAIL — module not found.

- [ ] **Step 3: Create src/core/sanitizeInput.ts**

```ts
import type { AmountConfig, SanitizedInput } from "./types";
import { configIsDigit, configIsInputSeparator } from "./types";

export function sanitizeInput(
  text: string,
  cursor: number,
  config: AmountConfig,
): SanitizedInput {
  const src = [...text];
  let builder = "";
  let separatorSeen = false;
  let integerDigits = 0;
  let fractionDigits = 0;
  const originalCursor = Math.max(0, Math.min(cursor, src.length));
  let mappedCursor = 0;

  for (let i = 0; i < src.length; i++) {
    const c = src[i]!;
    let kept = false;
    if (configIsDigit(config, c)) {
      const underLimit = separatorSeen
        ? fractionDigits < config.maximumFractionDigits
        : integerDigits < config.maximumNotationDigits;
      if (underLimit) {
        builder += c;
        if (separatorSeen) fractionDigits += 1;
        else integerDigits += 1;
        kept = true;
      }
    } else if (
      configIsInputSeparator(config, c) &&
      !separatorSeen &&
      config.maximumFractionDigits > 0
    ) {
      separatorSeen = true;
      builder += config.decimalSeparator;
      kept = true;
    }
    if (kept && i < originalCursor) mappedCursor += 1;
  }

  return { text: builder, cursor: Math.min(mappedCursor, builder.length) };
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd camount-js && npm test -- sanitizeInput`
Expected: PASS.

---

## Task 5: Core — AmountFormatter

**Files:**
- Create: `camount-js/src/core/formatter.ts`
- Create: `camount-js/tests/core/formatter.test.ts`

- [ ] **Step 1: Write the failing tests**

Create `camount-js/tests/core/formatter.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import type { AmountConfig, Money } from "../../src/core/types";
import { AmountFormatter } from "../../src/core/formatter";

const euro: AmountConfig = {
  maximumNotationDigits: 5,
  decimalSeparator: ",",
  groupingSeparator: " ",
  prefix: "",
  suffix: " €",
  groupingSize: 3,
  maximumFractionDigits: 2,
};
const usd: AmountConfig = {
  maximumNotationDigits: 5,
  decimalSeparator: ".",
  groupingSeparator: ",",
  prefix: "$",
  suffix: "",
  groupingSize: 3,
  maximumFractionDigits: 2,
};
const yen: AmountConfig = {
  maximumNotationDigits: 5,
  decimalSeparator: ".",
  groupingSeparator: ",",
  prefix: "¥",
  suffix: "",
  groupingSize: 3,
  maximumFractionDigits: 0,
};

const money = (units: bigint, nanos: number, currencyCode: string): Money =>
  ({ units, nanos, currencyCode });

describe("AmountFormatter — format(Money)", () => {
  it("small integer", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.format(money(42n, 0, "USD"))).toBe("$42");
  });

  it("with fraction", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.format(money(1n, 500_000_000, "USD"))).toBe("$1.50");
  });

  it("suffix currency", () => {
    const f = new AmountFormatter({ config: euro });
    expect(f.format(money(1234n, 560_000_000, "EUR"))).toBe("1234,56 €");
  });

  it("zero-fraction currency", () => {
    const f = new AmountFormatter({ config: yen });
    expect(f.format(money(12345n, 0, "JPY"))).toBe("¥12345");
  });

  it("negative unsigned here", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.format(money(-42n, 0, "USD"))).toBe("$42");
  });

  it("nanos padded to nine", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.format(money(1n, 50_000_000, "USD"))).toBe("$1.05");
  });

  it("overflow notation truncated", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.format(money(1_234_567n, 0, "USD"))).toBe("$12345");
  });

  it("zero nanos + fixed fraction true keeps integer-only", () => {
    const f = new AmountFormatter({ config: usd, withFixedFractionLength: true });
    expect(f.format(money(10n, 0, "USD"))).toBe("$10");
  });
});

describe("AmountFormatter — parse", () => {
  it("units only", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.parse("42", "USD")).toEqual(money(42n, 0, "USD"));
  });

  it("with fraction", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.parse("1.5", "USD")).toEqual(money(1n, 500_000_000, "USD"));
  });

  it("negative", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.parse("-2", "USD")).toEqual(money(-2n, 0, "USD"));
  });

  it("ignores grouping and currency", () => {
    const f = new AmountFormatter({ config: euro });
    expect(f.parse("1 234,56 €", "EUR")).toEqual(money(1234n, 560_000_000, "EUR"));
  });

  it("empty", () => {
    const f = new AmountFormatter({ config: usd });
    expect(f.parse("", "USD")).toEqual(money(0n, 0, "USD"));
  });
});

describe("AmountFormatter — input flow", () => {
  it("simple digit", () => {
    const f = new AmountFormatter({
      config: usd,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true,
    });
    expect(f.formatInput({ source: "", start: 0, end: 0, text: "5", textStart: 0, textEnd: 1 })).toBe("5");
  });

  it("decimal separator", () => {
    const f = new AmountFormatter({
      config: usd,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true,
    });
    const result = f.formatInput({ source: "5", start: 1, end: 1, text: ".", textStart: 0, textEnd: 1 });
    expect(result.startsWith("5.") || result.startsWith("5,")).toBe(true);
  });
});
```

- [ ] **Step 2: Run to verify failure**

Run: `cd camount-js && npm test -- formatter`
Expected: FAIL — module not found.

- [ ] **Step 3: Create src/core/formatter.ts**

```ts
import {
  AmountConfig,
  AmountFieldPositions,
  FieldRange,
  Money,
  configIsDecimalSeparator,
  configIsDigit,
  configIsInputSeparator,
  configIsZero,
  configZero,
  fieldRangeEmpty,
  fieldRangeIsValid,
  fieldRangeOffset,
} from "./types";

export interface AmountFormatterOptions {
  readonly config: AmountConfig;
  readonly withCurrency?: boolean;
  readonly withGroupingSeparators?: boolean;
  readonly withFixedFractionLength?: boolean;
  readonly withFixedZeroNotation?: boolean;
}

export interface FormatInputArgs {
  readonly source: string;
  readonly start: number;
  readonly end: number;
  readonly text: string;
  readonly textStart: number;
  readonly textEnd: number;
}

export class AmountFormatter {
  private readonly config: AmountConfig;
  private readonly withCurrency: boolean;
  private readonly withGroupingSeparators: boolean;
  private readonly withFixedFractionLength: boolean;
  private readonly withFixedZeroNotation: boolean;

  private fixedFractionPosition: FieldRange = fieldRangeEmpty();
  private zeroNotationPosition: FieldRange = fieldRangeEmpty();
  private currencySuffixPosition: FieldRange = fieldRangeEmpty();
  private cursorPos = 0;

  private notation = "";
  private separatorFound = false;
  private duplicateSeparator = false;
  private fraction = "";
  private resultBuffer = "";

  constructor(opts: AmountFormatterOptions) {
    this.config = opts.config;
    this.withCurrency = opts.withCurrency ?? true;
    this.withGroupingSeparators = opts.withGroupingSeparators ?? true;
    this.withFixedFractionLength = opts.withFixedFractionLength ?? true;
    this.withFixedZeroNotation = opts.withFixedZeroNotation ?? true;
  }

  get lastCursorPosition(): number {
    return this.cursorPos;
  }

  format(money: Money): string {
    this.reset();

    const absUnits = money.units < 0n ? -money.units : money.units;
    const absNanos = money.nanos < 0 ? -money.nanos : money.nanos;

    const unitsStr = absUnits.toString();
    const units =
      unitsStr.length <= this.config.maximumNotationDigits
        ? unitsStr
        : unitsStr.substring(0, this.config.maximumNotationDigits);
    this.resultBuffer += units;

    if (this.config.maximumFractionDigits > 0 && money.nanos !== 0) {
      const nanosStr = absNanos.toString();
      const nanosPadded = "0".repeat(Math.max(0, 9 - nanosStr.length)) + nanosStr;
      const nanos =
        nanosPadded.length <= this.config.maximumFractionDigits
          ? nanosPadded
          : nanosPadded.substring(0, this.config.maximumFractionDigits);
      const hasNonZero = [...nanos].some((ch) => !configIsZero(this.config, ch));
      if (this.withFixedFractionLength || hasNonZero) {
        this.resultBuffer += this.config.decimalSeparator;
        this.resultBuffer += nanos;
      }
    }

    this.appendCurrency();
    return this.resultBuffer;
  }

  formatInput(args: FormatInputArgs): string {
    this.reset();

    this.appendRange(args.source, 0, args.start, false);
    const afterChangeCount = this.appendRange(args.text, args.textStart, args.textEnd, true);
    this.appendRange(args.source, args.end, args.source.length, false);

    if (this.duplicateSeparator) {
      this.cursorPos = args.end;
      return args.source;
    }
    const out = this.buildResult();
    this.cursorPos = this.findSelection(out, afterChangeCount);
    return out;
  }

  fieldPositions(): AmountFieldPositions {
    return {
      cursorPosition: this.cursorPos,
      fixedFraction: { ...this.fixedFractionPosition },
      zeroNotation: { ...this.zeroNotationPosition },
    };
  }

  parse(raw: string, currencyCode: string): Money {
    let negative = false;
    let separator = false;
    let hasDigits = false;
    let integer = "";
    let fractionDigits = "";

    for (const c of raw) {
      if (c === "-" && !hasDigits) {
        negative = true;
      } else if (configIsInputSeparator(this.config, c)) {
        if (!separator) separator = true;
      } else if (configIsDigit(this.config, c)) {
        hasDigits = true;
        if (separator) fractionDigits += c;
        else integer += c;
      }
    }

    if (!hasDigits) return { units: 0n, nanos: 0, currencyCode };

    const units = BigInt(integer.length === 0 ? "0" : integer);
    const nanoDigitsPadded = (fractionDigits + "0".repeat(Math.max(0, 9 - fractionDigits.length))).substring(0, 9);
    const nanos = Number.parseInt(nanoDigitsPadded, 10) || 0;

    const signedUnits = negative ? -units : units;
    const signedNanos = negative ? -nanos : nanos;
    return { units: signedUnits, nanos: signedNanos, currencyCode };
  }

  private reset(): void {
    this.notation = "";
    this.separatorFound = false;
    this.duplicateSeparator = false;
    this.fraction = "";
    this.resultBuffer = "";
    this.fixedFractionPosition = fieldRangeEmpty();
    this.zeroNotationPosition = fieldRangeEmpty();
    this.currencySuffixPosition = fieldRangeEmpty();
    this.cursorPos = 0;
  }

  private appendRange(
    source: string,
    start: number,
    end: number,
    withInputSeparator: boolean,
  ): number {
    let count = 0;
    const chars = [...source];
    const clampedStart = Math.max(0, Math.min(start, chars.length));
    const clampedEnd = Math.max(clampedStart, Math.min(end, chars.length));
    for (let i = clampedStart; i < clampedEnd; i++) {
      if (this.duplicateSeparator) break;
      const c = chars[i]!;
      if (withInputSeparator && configIsInputSeparator(this.config, c)) {
        count += this.ensureSeparator();
      } else if (!withInputSeparator && configIsDecimalSeparator(this.config, c)) {
        count += this.ensureSeparator();
      } else if (configIsDigit(this.config, c)) {
        count += this.appendDigit(c);
      }
    }
    return count;
  }

  private ensureSeparator(): number {
    if (this.separatorFound) {
      this.duplicateSeparator = true;
      return 0;
    }
    this.separatorFound = true;
    return 1;
  }

  private appendDigit(c: string): number {
    if (this.separatorFound) {
      if (this.fraction.length < this.config.maximumFractionDigits) {
        this.fraction += c;
        return 1;
      }
    } else {
      if (this.notation.length < this.config.maximumNotationDigits) {
        if (this.notation.length === 1 && configIsZero(this.config, this.notation[0]!)) {
          if (!configIsZero(this.config, c)) {
            this.notation = c;
          }
        } else {
          this.notation += c;
          return 1;
        }
      }
    }
    return 0;
  }

  private buildResult(): string {
    this.appendNotation();
    this.appendFraction();
    this.appendCurrency();
    return this.resultBuffer;
  }

  private appendNotation(): void {
    if (this.notation.length > 0) {
      this.resultBuffer += this.notation;
      if (this.withGroupingSeparators) {
        const groupLength = this.config.groupingSize;
        if (groupLength > 0) {
          const notationLength = this.notation.length;
          if (notationLength > groupLength) {
            let offset = notationLength - groupLength;
            while (offset >= 1) {
              this.resultBuffer =
                this.resultBuffer.substring(0, offset) +
                this.config.groupingSeparator +
                this.resultBuffer.substring(offset);
              offset -= groupLength;
            }
          }
        }
      }
    } else if (this.withFixedZeroNotation) {
      this.savePosition("zeroNotation", () => {
        this.resultBuffer += configZero(this.config);
      });
    }
  }

  private appendFraction(): void {
    if (!this.separatorFound) return;
    if (this.config.maximumFractionDigits === 0) return;

    this.zeroNotationPosition = fieldRangeEmpty();

    if (this.resultBuffer.length === 0) this.resultBuffer += configZero(this.config);

    this.resultBuffer += this.config.decimalSeparator;
    this.resultBuffer += this.fraction;

    if (this.withFixedFractionLength) {
      this.savePosition("fixedFraction", () => {
        const pad = this.config.maximumFractionDigits - this.fraction.length;
        if (pad > 0) this.resultBuffer += configZero(this.config).repeat(pad);
      });
    }
  }

  private appendCurrency(): void {
    if (!this.withCurrency) return;

    if (this.config.prefix.length > 0) {
      this.resultBuffer = this.config.prefix + this.resultBuffer;
      this.fixedFractionPosition = fieldRangeOffset(this.fixedFractionPosition, this.config.prefix.length);
      this.zeroNotationPosition = fieldRangeOffset(this.zeroNotationPosition, this.config.prefix.length);
    }

    if (this.config.suffix.length > 0) {
      this.savePosition("currencySuffix", () => {
        this.resultBuffer += this.config.suffix;
      });
    }
  }

  private findSelection(text: string, selection: number): number {
    let count = selection;
    const trailing: FieldRange[] = [this.currencySuffixPosition, this.fixedFractionPosition, this.zeroNotationPosition].filter(fieldRangeIsValid);
    let index = trailing.length === 0 ? text.length : trailing.reduce((m, r) => Math.min(m, r.beginIndex), text.length);
    while (index > 0 && count > 0) {
      const c = text[index - 1]!;
      if (configIsDecimalSeparator(this.config, c) || configIsDigit(this.config, c)) {
        count -= 1;
      }
      index -= 1;
    }
    return index;
  }

  private savePosition(which: "fixedFraction" | "zeroNotation" | "currencySuffix", block: () => void): void {
    const begin = this.resultBuffer.length;
    block();
    const end = this.resultBuffer.length;
    const range = { beginIndex: begin, endIndex: end };
    if (which === "fixedFraction") this.fixedFractionPosition = range;
    else if (which === "zeroNotation") this.zeroNotationPosition = range;
    else this.currencySuffixPosition = range;
  }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd camount-js && npm test -- formatter`
Expected: all formatter tests PASS.

---

## Task 6: Core — DiffCalculator

**Files:**
- Create: `camount-js/src/core/diffCalculator.ts`
- Create: `camount-js/tests/core/diffCalculator.test.ts`

**Note on layering:** The Swift port puts `DiffCalculator` under `Rendering/` because it holds `SymbolCell` refs. In JS we keep a minimal `SymbolCellLike` interface so Core can own the diff algorithm without DOM — tests then use an in-memory stub. This matches the Swift "no UIKit" path (`else` branch of `#if canImport(UIKit)` in `SymbolCell.swift`).

- [ ] **Step 1: Write the failing tests**

Create `camount-js/tests/core/diffCalculator.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import type { AmountConfig, Field } from "../../src/core/types";
import { EMPTY_POSITIONS } from "../../src/core/types";
import {
  EditDiffCalculator,
  LevenshteinDiffCalculator,
  SymbolCellLike,
} from "../../src/core/diffCalculator";

const usd: AmountConfig = {
  maximumNotationDigits: 5,
  decimalSeparator: ".",
  groupingSeparator: ",",
  prefix: "$",
  suffix: "",
  groupingSize: 3,
  maximumFractionDigits: 2,
};

class StubCell implements SymbolCellLike {
  currentChar = "\0";
  isVisible = false;
  isRunning = false;
  field: Field | undefined;
  replace(char: string, field: Field | undefined): void {
    this.currentChar = char;
    this.field = field;
    this.isVisible = true;
  }
  delete(): void {
    this.isVisible = false;
  }
}

const newCell = () => new StubCell();

const visible = (cells: SymbolCellLike[]): string =>
  cells.filter((c) => c.isVisible).map((c) => c.currentChar).join("");

describe("EditDiffCalculator", () => {
  it("inserts into empty", () => {
    let cells: SymbolCellLike[] = [];
    const diff = new EditDiffCalculator(usd, newCell);
    cells = diff.diff(cells, "$42", EMPTY_POSITIONS);
    expect(visible(cells)).toBe("$42");
  });

  it("replaces digit", () => {
    let cells: SymbolCellLike[] = [];
    const diff = new EditDiffCalculator(usd, newCell);
    cells = diff.diff(cells, "$4", EMPTY_POSITIONS);
    cells = diff.diff(cells, "$5", EMPTY_POSITIONS);
    expect(visible(cells)).toBe("$5");
  });

  it("deletes trailing", () => {
    let cells: SymbolCellLike[] = [];
    const diff = new EditDiffCalculator(usd, newCell);
    cells = diff.diff(cells, "$42", EMPTY_POSITIONS);
    cells = diff.diff(cells, "$4", EMPTY_POSITIONS);
    expect(visible(cells)).toBe("$4");
  });
});

describe("LevenshteinDiffCalculator", () => {
  it("inserts from empty", () => {
    let cells: SymbolCellLike[] = [];
    const diff = new LevenshteinDiffCalculator(usd, newCell);
    cells = diff.diff(cells, "$1.50", EMPTY_POSITIONS);
    expect(visible(cells)).toBe("$1.50");
  });

  it("replaces interior", () => {
    let cells: SymbolCellLike[] = [];
    const diff = new LevenshteinDiffCalculator(usd, newCell);
    cells = diff.diff(cells, "$1.50", EMPTY_POSITIONS);
    cells = diff.diff(cells, "$2.50", EMPTY_POSITIONS);
    expect(visible(cells)).toBe("$2.50");
  });

  it("shrinks", () => {
    let cells: SymbolCellLike[] = [];
    const diff = new LevenshteinDiffCalculator(usd, newCell);
    cells = diff.diff(cells, "$12.34", EMPTY_POSITIONS);
    cells = diff.diff(cells, "$1.23", EMPTY_POSITIONS);
    expect(visible(cells)).toBe("$1.23");
  });
});
```

- [ ] **Step 2: Run to verify failure**

Run: `cd camount-js && npm test -- diffCalculator`
Expected: FAIL — module not found.

- [ ] **Step 3: Create src/core/diffCalculator.ts**

```ts
import {
  AmountConfig,
  AmountFieldPositions,
  Field,
  configIsDecimalSeparator,
  configIsDigit,
  configIsGroupingSeparator,
  configMaximumFormattedSymbols,
  fieldRangeContains,
} from "./types";

export interface SymbolCellLike {
  readonly currentChar: string;
  readonly isVisible: boolean;
  readonly isRunning: boolean;
  field: Field | undefined;
  replace(char: string, field: Field | undefined): void;
  delete(): void;
}

export type DiffMode = "edit" | "levenshtein";

export interface DiffCalculator {
  diff<T extends SymbolCellLike>(
    cells: ReadonlyArray<T>,
    text: string,
    positions: AmountFieldPositions,
  ): T[];
}

export function makeDiffCalculator<T extends SymbolCellLike>(
  mode: DiffMode,
  config: AmountConfig,
  newCell: () => T,
): DiffCalculator {
  return mode === "edit"
    ? new EditDiffCalculator(config, newCell)
    : new LevenshteinDiffCalculator(config, newCell);
}

function fieldAt(positions: AmountFieldPositions, index: number): Field | undefined {
  if (fieldRangeContains(positions.zeroNotation, index)) return "zeroNotation";
  if (fieldRangeContains(positions.fixedFraction, index)) return "fixedFraction";
  return undefined;
}

export class EditDiffCalculator implements DiffCalculator {
  constructor(private readonly config: AmountConfig, private readonly newCell: () => SymbolCellLike) {}

  diff<T extends SymbolCellLike>(
    cells: ReadonlyArray<T>,
    text: string,
    positions: AmountFieldPositions,
  ): T[] {
    const result: T[] = [];
    const chars = [...text];
    let textIndex = 0;

    for (let cellIndex = 0; cellIndex < cells.length; cellIndex++) {
      const curCell = cells[cellIndex]!;
      if (!(curCell.isVisible || curCell.isRunning)) continue;

      let retry = true;
      while (retry) {
        if (textIndex >= chars.length) {
          curCell.delete();
          result.push(curCell);
          retry = false;
          break;
        }
        const s1 = curCell.currentChar;
        const s2 = chars[textIndex]!;
        const field = fieldAt(positions, textIndex);
        if (s1 === s2) {
          curCell.replace(s2, field);
          textIndex += 1;
          result.push(curCell);
          retry = false;
        } else if (configIsGroupingSeparator(this.config, s1) || configIsDecimalSeparator(this.config, s1)) {
          curCell.delete();
          result.push(curCell);
          retry = false;
        } else if (configIsGroupingSeparator(this.config, s2) || configIsDecimalSeparator(this.config, s2)) {
          const fresh = this.newCell() as T;
          fresh.replace(s2, field);
          textIndex += 1;
          result.push(fresh);
          continue;
        } else {
          const s1Digit = configIsDigit(this.config, s1);
          const s2Digit = configIsDigit(this.config, s2);
          if (s1Digit) {
            if (s2Digit) {
              curCell.replace(s2, field);
              textIndex += 1;
              result.push(curCell);
              retry = false;
            } else {
              curCell.delete();
              result.push(curCell);
              retry = false;
            }
          } else {
            if (s2Digit) {
              const fresh = this.newCell() as T;
              fresh.replace(s2, field);
              textIndex += 1;
              result.push(fresh);
              continue;
            } else {
              curCell.replace(s2, field);
              textIndex += 1;
              result.push(curCell);
              retry = false;
            }
          }
        }
      }
    }

    const restCount = Math.min(chars.length, configMaximumFormattedSymbols(this.config));
    while (textIndex < restCount) {
      const s = chars[textIndex]!;
      const field = fieldAt(positions, textIndex);
      textIndex += 1;
      const cell = this.newCell() as T;
      cell.replace(s, field);
      result.push(cell);
    }

    return result;
  }
}

export class LevenshteinDiffCalculator implements DiffCalculator {
  constructor(private readonly config: AmountConfig, private readonly newCell: () => SymbolCellLike) {}

  diff<T extends SymbolCellLike>(
    cells: ReadonlyArray<T>,
    text: string,
    positions: AmountFieldPositions,
  ): T[] {
    const working: T[] = [...cells];
    const chars = [...text];
    const matrix = this.matrix(working, chars);
    this.apply(working, chars, matrix, positions);
    return working;
  }

  private matrix(cells: SymbolCellLike[], text: string[]): number[][] {
    const xLen = cells.length;
    const yLen = text.length;
    const dp: number[][] = Array.from({ length: xLen + 1 }, () => new Array<number>(yLen + 1).fill(0));
    for (let i = 0; i <= xLen; i++) {
      for (let j = 0; j <= yLen; j++) {
        if (i === 0) dp[i]![j] = j;
        else if (j === 0) dp[i]![j] = i;
        else {
          const c1 = cells[i - 1]!.currentChar;
          const c2 = text[j - 1]!;
          const replaceCost = dp[i - 1]![j - 1]! + (c1 === c2 ? 0 : 1);
          const insertCost = dp[i]![j - 1]! + 1;
          const deleteCost = dp[i - 1]![j]! + 1;
          dp[i]![j] = Math.min(replaceCost, insertCost, deleteCost);
        }
      }
    }
    return dp;
  }

  private apply<T extends SymbolCellLike>(
    cells: T[],
    text: string[],
    matrix: number[][],
    positions: AmountFieldPositions,
  ): void {
    let i = cells.length;
    let j = text.length;
    while (i >= 0 && j >= 0) {
      if (i === 0 && j === 0) break;
      if (i === 0 && j > 0) {
        j -= 1;
        this.insertAt(cells, i, text[j]!, fieldAt(positions, j));
      } else if (i > 0 && j === 0) {
        i -= 1;
        cells[i]!.delete();
      } else {
        const replaceCost = matrix[i - 1]![j - 1]!;
        const deleteCost = matrix[i - 1]![j]!;
        const insertCost = matrix[i]![j - 1]!;
        const from = cells[i - 1]!.currentChar;
        const to = text[j - 1]!;
        const minCost = Math.min(replaceCost, insertCost, deleteCost);
        if (minCost === replaceCost) {
          if (this.isSeparator(from) && !this.isSeparator(to)) {
            i -= 1;
            cells[i]!.delete();
          } else if (this.isSeparator(to) && !this.isSeparator(from)) {
            j -= 1;
            this.insertAt(cells, i, to, fieldAt(positions, j));
          } else {
            i -= 1;
            j -= 1;
            cells[i]!.replace(to, fieldAt(positions, j));
          }
        } else if (minCost === insertCost) {
          j -= 1;
          this.insertAt(cells, i, to, fieldAt(positions, j));
        } else {
          i -= 1;
          cells[i]!.delete();
        }
      }
    }
  }

  private insertAt<T extends SymbolCellLike>(cells: T[], index: number, char: string, field: Field | undefined): void {
    const cell = this.newCell() as T;
    cell.replace(char, field);
    cells.splice(index, 0, cell);
  }

  private isSeparator(c: string): boolean {
    return configIsDecimalSeparator(this.config, c) || configIsGroupingSeparator(this.config, c);
  }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd camount-js && npm test -- diffCalculator`
Expected: PASS.

---

## Task 7: Core barrel

**Files:**
- Create: `camount-js/src/core/index.ts`

- [ ] **Step 1: Create the barrel**

```ts
export * from "./types";
export * from "./currencyInfo";
export * from "./sanitizeInput";
export * from "./formatter";
export * from "./diffCalculator";
```

- [ ] **Step 2: Typecheck**

Run: `cd camount-js && npm run typecheck`
Expected: PASS — no diagnostics.

---

## Task 8: Rendering — styles

**Files:**
- Create: `camount-js/src/rendering/styles.ts`

- [ ] **Step 1: Create styles module**

```ts
// Shared CSS for <camount-text> and <camount-field> hosts. Attached to each
// element's shadow root via adoptedStyleSheets (cheap — one sheet shared).
const CSS = `
:host {
  display: inline-block;
  position: relative;
  line-height: 1;
  font: inherit;
  color: inherit;
  white-space: nowrap;
  --camount-duration: 120ms;
  --camount-easing: cubic-bezier(0.4, 0, 0.2, 1);
  --camount-cursor-color: currentColor;
  --camount-cursor-width: 2px;
  --camount-cursor-height: 1em;
}
.camount-row {
  display: inline-flex;
  align-items: stretch;
  position: relative;
}
.camount-cell {
  display: inline-block;
  overflow: hidden;
  height: 1em;
  line-height: 1em;
  position: relative;
  transition: width var(--camount-duration) var(--camount-easing);
}
.camount-cell[data-visible="false"] {
  width: 0 !important;
  transition: width var(--camount-duration) var(--camount-easing);
}
.camount-stack {
  display: block;
  transform: translateY(var(--camount-y, 0));
  transition:
    transform var(--camount-duration) var(--camount-easing),
    opacity var(--camount-duration) var(--camount-easing);
}
.camount-glyph {
  display: block;
  height: 1em;
  line-height: 1em;
  opacity: var(--camount-opacity, 1);
  transition: opacity var(--camount-duration) var(--camount-easing);
}
.camount-glyph[data-field="zeroNotation"] {
  color: var(--camount-zero-notation-color, inherit);
}
.camount-glyph[data-field="fixedFraction"] {
  color: var(--camount-fixed-fraction-color, inherit);
}
.camount-cursor {
  position: absolute;
  width: var(--camount-cursor-width);
  height: var(--camount-cursor-height);
  background-color: var(--camount-cursor-color);
  top: 0;
  transition: left var(--camount-duration) var(--camount-easing), opacity 120ms linear;
  pointer-events: none;
  opacity: 0;
}
.camount-cursor[data-visible="true"] {
  animation: camount-cursor-blink 1s step-end infinite;
  opacity: 1;
}
@keyframes camount-cursor-blink {
  0%, 50% { opacity: 1; }
  50.01%, 100% { opacity: 0; }
}
.camount-hidden-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: text;
  background: transparent;
  border: 0;
  padding: 0;
  margin: 0;
  color: transparent;
  caret-color: transparent;
  font: inherit;
}
`;

let sharedSheet: CSSStyleSheet | null = null;

export function camountStyleSheet(): CSSStyleSheet {
  if (sharedSheet) return sharedSheet;
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(CSS);
  sharedSheet = sheet;
  return sheet;
}

export const CAMOUNT_CSS = CSS;
```

- [ ] **Step 2: Typecheck**

Run: `cd camount-js && npm run typecheck`
Expected: PASS.

---

## Task 9: Rendering — SymbolCell

**Files:**
- Create: `camount-js/src/rendering/symbolCell.ts`
- Create: `camount-js/tests/rendering/symbolCell.test.ts`

- [ ] **Step 1: Write the failing tests**

Create `camount-js/tests/rendering/symbolCell.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { SymbolCell } from "../../src/rendering/symbolCell";

describe("SymbolCell", () => {
  it("starts hidden", () => {
    const cell = new SymbolCell();
    expect(cell.isVisible).toBe(false);
    expect(cell.currentChar).toBe("\0");
    expect(cell.element).toBeInstanceOf(HTMLElement);
  });

  it("replace sets char, visibility, and DOM text", () => {
    const cell = new SymbolCell();
    cell.replace("5", undefined);
    expect(cell.isVisible).toBe(true);
    expect(cell.currentChar).toBe("5");
    expect(cell.element.dataset.visible).toBe("true");
    expect(cell.element.textContent).toContain("5");
  });

  it("delete hides but keeps DOM", () => {
    const cell = new SymbolCell();
    cell.replace("5", undefined);
    cell.delete();
    expect(cell.isVisible).toBe(false);
    expect(cell.element.dataset.visible).toBe("false");
  });

  it("field updates data attribute on active glyph", () => {
    const cell = new SymbolCell();
    cell.replace("0", "zeroNotation");
    const active = cell.element.querySelector<HTMLElement>(".camount-glyph[data-active='true']");
    expect(active?.dataset.field).toBe("zeroNotation");
  });
});
```

- [ ] **Step 2: Run to verify failure**

Run: `cd camount-js && npm test -- symbolCell`
Expected: FAIL — module not found.

- [ ] **Step 3: Create src/rendering/symbolCell.ts**

```ts
import type { Field } from "../core/types";
import type { SymbolCellLike } from "../core/diffCalculator";

const MAX_STACK = 3;

interface StackEntry {
  readonly char: string;
  readonly field: Field | undefined;
  readonly element: HTMLElement;
}

export class SymbolCell implements SymbolCellLike {
  readonly element: HTMLElement;
  private readonly stackElement: HTMLElement;
  private stack: StackEntry[] = [];
  currentChar = "\0";
  isVisible = false;
  field: Field | undefined;

  constructor(doc: Document = document) {
    this.element = doc.createElement("span");
    this.element.className = "camount-cell";
    this.element.dataset.visible = "false";
    this.stackElement = doc.createElement("span");
    this.stackElement.className = "camount-stack";
    this.element.appendChild(this.stackElement);
  }

  get isRunning(): boolean {
    return false;
  }

  replace(char: string, field: Field | undefined): void {
    this.field = field;
    if (this.stack.length > 0) {
      const last = this.stack[this.stack.length - 1]!;
      if (last.char === char && last.field === field) {
        this.setActive(this.stack.length - 1);
        this.currentChar = char;
        this.isVisible = true;
        this.element.dataset.visible = "true";
        return;
      }
    }
    const doc = this.element.ownerDocument!;
    const glyph = doc.createElement("span");
    glyph.className = "camount-glyph";
    glyph.textContent = char;
    if (field) glyph.dataset.field = field;
    this.stackElement.appendChild(glyph);
    this.stack.push({ char, field, element: glyph });
    while (this.stack.length > MAX_STACK) {
      const evicted = this.stack.shift()!;
      evicted.element.remove();
    }
    this.setActive(this.stack.length - 1);
    this.currentChar = char;
    this.isVisible = true;
    this.element.dataset.visible = "true";
  }

  delete(): void {
    this.isVisible = false;
    this.element.dataset.visible = "false";
    for (const entry of this.stack) entry.element.dataset.active = "false";
  }

  setStyleHints(hints: { zeroNotationColor?: string; fixedFractionColor?: string }): void {
    if (hints.zeroNotationColor !== undefined) {
      this.element.style.setProperty("--camount-zero-notation-color", hints.zeroNotationColor);
    }
    if (hints.fixedFractionColor !== undefined) {
      this.element.style.setProperty("--camount-fixed-fraction-color", hints.fixedFractionColor);
    }
  }

  private setActive(index: number): void {
    for (let i = 0; i < this.stack.length; i++) {
      const entry = this.stack[i]!;
      entry.element.dataset.active = i === index ? "true" : "false";
    }
    const activeOffset = -(this.stack.length - 1 - index);
    this.stackElement.style.setProperty("--camount-y", `${activeOffset}em`);
  }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd camount-js && npm test -- symbolCell`
Expected: PASS.

---

## Task 10: Rendering — CursorCell

**Files:**
- Create: `camount-js/src/rendering/cursorCell.ts`

- [ ] **Step 1: Create CursorCell**

```ts
export class CursorCell {
  readonly element: HTMLElement;

  constructor(doc: Document = document) {
    this.element = doc.createElement("span");
    this.element.className = "camount-cursor";
    this.element.dataset.visible = "false";
  }

  setVisible(visible: boolean): void {
    this.element.dataset.visible = visible ? "true" : "false";
  }

  setPosition(leftPx: number): void {
    this.element.style.left = `${leftPx}px`;
  }
}
```

- [ ] **Step 2: Typecheck**

Run: `cd camount-js && npm run typecheck`
Expected: PASS.

---

## Task 11: Rendering — painter + host

**Files:**
- Create: `camount-js/src/rendering/painter.ts`
- Create: `camount-js/src/rendering/host.ts`
- Create: `camount-js/src/rendering/index.ts`
- Create: `camount-js/tests/rendering/host.test.ts`

- [ ] **Step 1: Create painter.ts**

```ts
import type { AmountConfig, AmountFieldPositions } from "../core/types";
import { type DiffMode, makeDiffCalculator } from "../core/diffCalculator";
import { SymbolCell } from "./symbolCell";
import { CursorCell } from "./cursorCell";

export interface AmountStyle {
  zeroNotationColor?: string;
  fixedFractionColor?: string;
  cursorColor?: string;
  cursorWidthPx?: number;
}

export class AmountPainter {
  private readonly row: HTMLElement;
  private readonly cursor: CursorCell;
  private cells: SymbolCell[] = [];
  private config: AmountConfig;
  private mode: DiffMode;
  private diff = this.rebuildDiffCalculator();
  private style: AmountStyle;
  private lastText: string | null = null;
  private lastPositions: AmountFieldPositions | null = null;

  constructor(
    private readonly root: HTMLElement,
    opts: { config: AmountConfig; mode: DiffMode; style?: AmountStyle },
  ) {
    this.config = opts.config;
    this.mode = opts.mode;
    this.style = opts.style ?? {};
    this.row = root.ownerDocument!.createElement("span");
    this.row.className = "camount-row";
    this.cursor = new CursorCell(root.ownerDocument!);
    this.row.appendChild(this.cursor.element);
    root.appendChild(this.row);
    this.applyStyle();
    this.diff = this.rebuildDiffCalculator();
  }

  updateConfig(config: AmountConfig, mode: DiffMode): void {
    const configChanged = !this.configsEqual(config, this.config);
    const modeChanged = mode !== this.mode;
    this.config = config;
    this.mode = mode;
    if (configChanged || modeChanged) {
      this.diff = this.rebuildDiffCalculator();
      this.lastText = null;
    }
  }

  updateStyle(style: AmountStyle): void {
    this.style = style;
    this.applyStyle();
    for (const cell of this.cells) {
      cell.setStyleHints({
        zeroNotationColor: style.zeroNotationColor,
        fixedFractionColor: style.fixedFractionColor,
      });
    }
  }

  setText(text: string, positions: AmountFieldPositions): void {
    if (this.lastText === text && this.positionsEqual(this.lastPositions, positions)) return;
    this.lastText = text;
    this.lastPositions = positions;

    this.cells = this.diff.diff(this.cells, text, positions);
    this.syncDom();
    this.layoutCursor(positions);
  }

  setCursorVisible(visible: boolean): void {
    this.cursor.setVisible(visible);
  }

  dispose(): void {
    this.row.remove();
    this.cells = [];
  }

  private applyStyle(): void {
    const s = this.style;
    if (s.cursorColor !== undefined) this.root.style.setProperty("--camount-cursor-color", s.cursorColor);
    if (s.cursorWidthPx !== undefined) this.root.style.setProperty("--camount-cursor-width", `${s.cursorWidthPx}px`);
    if (s.zeroNotationColor !== undefined) this.root.style.setProperty("--camount-zero-notation-color", s.zeroNotationColor);
    if (s.fixedFractionColor !== undefined) this.root.style.setProperty("--camount-fixed-fraction-color", s.fixedFractionColor);
  }

  private syncDom(): void {
    const existingChildren = Array.from(this.row.children).filter(
      (c) => c !== this.cursor.element,
    ) as HTMLElement[];
    const kept = new Set<HTMLElement>(this.cells.map((c) => c.element));

    for (const child of existingChildren) {
      if (!kept.has(child)) child.remove();
    }
    for (let i = 0; i < this.cells.length; i++) {
      const cell = this.cells[i]!;
      const desired = cell.element;
      if (this.row.children[i] !== desired) {
        this.row.insertBefore(desired, this.row.children[i] ?? this.cursor.element);
      }
    }
    if (this.cursor.element.parentNode !== this.row) this.row.appendChild(this.cursor.element);
  }

  private layoutCursor(positions: AmountFieldPositions): void {
    if (positions.cursorPosition < 0) {
      this.cursor.setVisible(false);
      return;
    }
    let leftPx = 0;
    let seenVisible = 0;
    for (const cell of this.cells) {
      if (!cell.isVisible) continue;
      if (seenVisible === positions.cursorPosition) break;
      leftPx += cell.element.getBoundingClientRect().width;
      seenVisible += 1;
    }
    this.cursor.setPosition(leftPx);
  }

  private rebuildDiffCalculator() {
    return makeDiffCalculator(this.mode, this.config, () => new SymbolCell(this.root.ownerDocument!));
  }

  private configsEqual(a: AmountConfig, b: AmountConfig): boolean {
    return (
      a.maximumNotationDigits === b.maximumNotationDigits &&
      a.decimalSeparator === b.decimalSeparator &&
      a.groupingSeparator === b.groupingSeparator &&
      a.prefix === b.prefix &&
      a.suffix === b.suffix &&
      a.groupingSize === b.groupingSize &&
      a.maximumFractionDigits === b.maximumFractionDigits
    );
  }

  private positionsEqual(a: AmountFieldPositions | null, b: AmountFieldPositions): boolean {
    if (a === null) return false;
    return (
      a.cursorPosition === b.cursorPosition &&
      a.fixedFraction.beginIndex === b.fixedFraction.beginIndex &&
      a.fixedFraction.endIndex === b.fixedFraction.endIndex &&
      a.zeroNotation.beginIndex === b.zeroNotation.beginIndex &&
      a.zeroNotation.endIndex === b.zeroNotation.endIndex
    );
  }
}
```

- [ ] **Step 2: Create host.ts**

```ts
import {
  AmountConfig,
  EMPTY_POSITIONS,
  Money,
  moneyEquals,
  moneyIsPositive,
  moneyIsZero,
} from "../core/types";
import { amountConfigForCurrency } from "../core/currencyInfo";
import { AmountFormatter } from "../core/formatter";
import { sanitizeInput } from "../core/sanitizeInput";
import { AmountPainter, AmountStyle } from "./painter";
import { camountStyleSheet } from "./styles";
import type { DiffMode } from "../core/diffCalculator";

export type SignMode = "auto" | "always";
export type FractionPolicy = "fixed" | "compact";
export type Alignment = "start" | "center" | "end";

export interface AmountHostOptions {
  currencyCode: string;
  maximumNotationDigits?: number;
  editable?: boolean;
  mode?: DiffMode;
  style?: AmountStyle;
  showSign?: SignMode;
  fractionPolicy?: FractionPolicy;
  alignment?: Alignment;
  onMoneyChange?: (money: Money) => void;
}

export class AmountHost {
  private readonly root: HTMLElement; // shadow root host container
  private readonly painter: AmountPainter;
  private readonly hiddenInput: HTMLInputElement | null;

  private currencyCode: string;
  private config: AmountConfig;
  private maxNotation: number;
  private editable: boolean;
  private mode: DiffMode;
  private style: AmountStyle;
  private showSign: SignMode;
  private fractionPolicy: FractionPolicy;
  private alignment: Alignment;
  private onMoneyChange: ((m: Money) => void) | undefined;

  private displayFormatter: AmountFormatter;
  private inputFormatter: AmountFormatter;
  private lastParsed: Money | null = null;

  constructor(shadow: ShadowRoot, opts: AmountHostOptions) {
    shadow.adoptedStyleSheets = [...shadow.adoptedStyleSheets, camountStyleSheet()];
    this.root = shadow.ownerDocument!.createElement("span");
    this.root.className = "camount-host";
    shadow.appendChild(this.root);

    this.currencyCode = opts.currencyCode;
    this.maxNotation = opts.maximumNotationDigits ?? 9;
    this.editable = opts.editable ?? false;
    this.mode = opts.mode ?? (this.editable ? "edit" : "levenshtein");
    this.style = opts.style ?? {};
    this.showSign = opts.showSign ?? "auto";
    this.fractionPolicy = opts.fractionPolicy ?? "fixed";
    this.alignment = opts.alignment ?? "center";
    this.onMoneyChange = opts.onMoneyChange;

    this.config = amountConfigForCurrency(this.currencyCode, this.maxNotation);
    this.displayFormatter = this.makeDisplayFormatter();
    this.inputFormatter = this.makeInputFormatter();
    this.painter = new AmountPainter(this.root, { config: this.config, mode: this.mode, style: this.style });
    this.applyAlignment();

    if (this.editable) {
      const input = shadow.ownerDocument!.createElement("input");
      input.type = "text";
      input.inputMode = "decimal";
      input.autocomplete = "off";
      input.className = "camount-hidden-input";
      shadow.appendChild(input);
      this.hiddenInput = input;
      input.addEventListener("input", () => this.onInputEvent());
      input.addEventListener("focus", () => this.painter.setCursorVisible(true));
      input.addEventListener("blur", () => this.painter.setCursorVisible(false));
      shadow.host.addEventListener("click", () => input.focus());
    } else {
      this.hiddenInput = null;
    }
  }

  configure(changes: Partial<AmountHostOptions>): void {
    let recomputeConfig = false;
    if (changes.currencyCode !== undefined && changes.currencyCode !== this.currencyCode) {
      this.currencyCode = changes.currencyCode;
      recomputeConfig = true;
    }
    if (changes.maximumNotationDigits !== undefined && changes.maximumNotationDigits !== this.maxNotation) {
      this.maxNotation = changes.maximumNotationDigits;
      recomputeConfig = true;
    }
    if (changes.mode !== undefined) this.mode = changes.mode;
    if (changes.style !== undefined) {
      this.style = changes.style;
      this.painter.updateStyle(this.style);
    }
    if (changes.showSign !== undefined) this.showSign = changes.showSign;
    if (changes.fractionPolicy !== undefined) this.fractionPolicy = changes.fractionPolicy;
    if (changes.alignment !== undefined) {
      this.alignment = changes.alignment;
      this.applyAlignment();
    }
    if (changes.onMoneyChange !== undefined) this.onMoneyChange = changes.onMoneyChange;
    if (recomputeConfig) {
      this.config = amountConfigForCurrency(this.currencyCode, this.maxNotation);
      this.displayFormatter = this.makeDisplayFormatter();
      this.inputFormatter = this.makeInputFormatter();
    }
    this.painter.updateConfig(this.config, this.mode);
  }

  setAmount(money: Money): void {
    if (this.lastParsed && moneyEquals(this.lastParsed, money)) return;
    this.lastParsed = money;
    if (this.editable && this.hiddenInput) {
      const inputText = this.inputFormatter.format(money);
      this.hiddenInput.value = inputText;
      this.inputFormatter.formatInput({
        source: inputText,
        start: inputText.length,
        end: inputText.length,
        text: inputText,
        textStart: inputText.length,
        textEnd: inputText.length,
      });
      const positions = this.inputFormatter.fieldPositions();
      const displayText = this.renderDisplayFromInput(inputText);
      this.painter.setText(displayText, positions);
    } else {
      const base = this.displayFormatter.format(money);
      const rendered = moneyIsZero(money)
        ? base
        : !moneyIsPositive(money)
          ? "-" + base
          : this.showSign === "always"
            ? "+" + base
            : base;
      this.painter.setText(rendered, EMPTY_POSITIONS);
    }
  }

  dispose(): void {
    this.painter.dispose();
  }

  private onInputEvent(): void {
    if (!this.hiddenInput) return;
    const raw = this.hiddenInput.value;
    const cursor = this.hiddenInput.selectionStart ?? raw.length;
    const sanitized = sanitizeInput(raw, cursor, this.config);
    if (sanitized.text !== raw) {
      this.hiddenInput.value = sanitized.text;
      this.hiddenInput.setSelectionRange(sanitized.cursor, sanitized.cursor);
    }
    this.inputFormatter.formatInput({
      source: sanitized.text,
      start: sanitized.text.length,
      end: sanitized.text.length,
      text: sanitized.text,
      textStart: sanitized.text.length,
      textEnd: sanitized.text.length,
    });
    const positions = this.inputFormatter.fieldPositions();
    const displayText = this.renderDisplayFromInput(sanitized.text);
    this.painter.setText(displayText, positions);
    const parsed = this.displayFormatter.parse(sanitized.text, this.currencyCode);
    if (!this.lastParsed || !moneyEquals(this.lastParsed, parsed)) {
      this.lastParsed = parsed;
      this.onMoneyChange?.(parsed);
    }
  }

  private renderDisplayFromInput(input: string): string {
    return this.displayFormatter.formatInput({
      source: input,
      start: input.length,
      end: input.length,
      text: input,
      textStart: input.length,
      textEnd: input.length,
    });
  }

  private makeDisplayFormatter(): AmountFormatter {
    return new AmountFormatter({
      config: this.config,
      withCurrency: true,
      withGroupingSeparators: true,
      withFixedFractionLength: this.fractionPolicy === "fixed",
      withFixedZeroNotation: true,
    });
  }

  private makeInputFormatter(): AmountFormatter {
    return new AmountFormatter({
      config: this.config,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true,
    });
  }

  private applyAlignment(): void {
    this.root.style.textAlign = this.alignment === "center" ? "center" : this.alignment;
    this.root.style.justifyContent =
      this.alignment === "center" ? "center" : this.alignment === "end" ? "flex-end" : "flex-start";
  }
}
```

- [ ] **Step 3: Create rendering/index.ts**

```ts
export { AmountHost } from "./host";
export type { AmountHostOptions, SignMode, FractionPolicy, Alignment } from "./host";
export { AmountPainter } from "./painter";
export type { AmountStyle } from "./painter";
export { SymbolCell } from "./symbolCell";
export { CursorCell } from "./cursorCell";
export { camountStyleSheet, CAMOUNT_CSS } from "./styles";
```

- [ ] **Step 4: Write the failing host test**

Create `camount-js/tests/rendering/host.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { AmountHost } from "../../src/rendering/host";

describe("AmountHost (text mode)", () => {
  it("renders a money value into the shadow DOM", () => {
    const hostEl = document.createElement("div");
    const shadow = hostEl.attachShadow({ mode: "open" });
    document.body.appendChild(hostEl);
    const host = new AmountHost(shadow, { currencyCode: "USD" });
    host.setAmount({ units: 42n, nanos: 0, currencyCode: "USD" });
    const cells = shadow.querySelectorAll(".camount-cell[data-visible='true']");
    expect(cells.length).toBeGreaterThan(0);
    const text = Array.from(shadow.querySelectorAll(".camount-glyph[data-active='true']"))
      .map((e) => e.textContent)
      .join("");
    expect(text).toContain("42");
    host.dispose();
    hostEl.remove();
  });

  it("updates on second setAmount", () => {
    const hostEl = document.createElement("div");
    const shadow = hostEl.attachShadow({ mode: "open" });
    document.body.appendChild(hostEl);
    const host = new AmountHost(shadow, { currencyCode: "USD" });
    host.setAmount({ units: 1n, nanos: 500_000_000, currencyCode: "USD" });
    host.setAmount({ units: 2n, nanos: 500_000_000, currencyCode: "USD" });
    const text = Array.from(shadow.querySelectorAll(".camount-glyph[data-active='true']"))
      .map((e) => e.textContent)
      .join("");
    expect(text).toContain("2");
    expect(text).toContain("50");
    host.dispose();
    hostEl.remove();
  });
});
```

- [ ] **Step 5: Run to verify pass**

Run: `cd camount-js && npm test -- rendering/host`
Expected: PASS.

---

## Task 12: Elements — `<camount-text>` and `<camount-field>`

**Files:**
- Create: `camount-js/src/elements/amountText.ts`
- Create: `camount-js/src/elements/amountField.ts`
- Create: `camount-js/src/elements/index.ts`
- Create: `camount-js/tests/elements/amountText.test.ts`
- Create: `camount-js/tests/elements/amountField.test.ts`

- [ ] **Step 1: Create amountText.ts**

```ts
import { AmountHost, AmountHostOptions } from "../rendering/host";
import type { Money } from "../core/types";

export class CamountTextElement extends HTMLElement {
  static readonly tagName = "camount-text";
  static get observedAttributes(): string[] {
    return ["amount", "currency", "maximum-notation-digits", "show-sign", "fraction-policy", "alignment"];
  }

  private host: AmountHost | null = null;
  private _amount: bigint = 0n;
  private _nanos = 0;

  connectedCallback(): void {
    if (this.host !== null) return;
    const shadow = this.attachShadow({ mode: "open" });
    this.host = new AmountHost(shadow, this.readOptions());
    this.applyAmount();
  }

  attributeChangedCallback(name: string, _old: string | null, _next: string | null): void {
    if (!this.host) return;
    if (name === "amount") {
      this.parseAmountAttribute();
      this.applyAmount();
      return;
    }
    this.host.configure(this.readOptions());
    this.applyAmount();
  }

  disconnectedCallback(): void {
    this.host?.dispose();
    this.host = null;
  }

  set amount(value: number | string | bigint) {
    if (typeof value === "bigint") {
      this._amount = value;
      this._nanos = 0;
    } else {
      const { units, nanos } = parseNumeric(value);
      this._amount = units;
      this._nanos = nanos;
    }
    this.applyAmount();
  }

  get amount(): number {
    const nanoFraction = this._nanos / 1_000_000_000;
    return Number(this._amount) + (this._amount < 0n ? -nanoFraction : nanoFraction);
  }

  private parseAmountAttribute(): void {
    const raw = this.getAttribute("amount") ?? "0";
    const { units, nanos } = parseNumeric(raw);
    this._amount = units;
    this._nanos = nanos;
  }

  private applyAmount(): void {
    if (!this.host) return;
    const currency = this.getAttribute("currency") ?? "USD";
    const money: Money = { units: this._amount, nanos: this._nanos, currencyCode: currency };
    this.host.setAmount(money);
  }

  private readOptions(): AmountHostOptions {
    return {
      currencyCode: this.getAttribute("currency") ?? "USD",
      maximumNotationDigits: this.readIntAttr("maximum-notation-digits", 9),
      editable: false,
      showSign: this.getAttribute("show-sign") === "always" ? "always" : "auto",
      fractionPolicy: this.getAttribute("fraction-policy") === "compact" ? "compact" : "fixed",
      alignment: this.readAlignmentAttr(),
    };
  }

  private readIntAttr(name: string, fallback: number): number {
    const v = this.getAttribute(name);
    if (v === null) return fallback;
    const n = Number.parseInt(v, 10);
    return Number.isFinite(n) ? n : fallback;
  }

  private readAlignmentAttr(): "start" | "center" | "end" {
    const v = this.getAttribute("alignment");
    if (v === "start" || v === "end") return v;
    return "center";
  }
}

function parseNumeric(value: string | number): { units: bigint; nanos: number } {
  if (typeof value === "number") {
    if (!Number.isFinite(value)) return { units: 0n, nanos: 0 };
    const negative = value < 0;
    const abs = Math.abs(value);
    const units = BigInt(Math.trunc(abs));
    const frac = abs - Math.trunc(abs);
    const nanos = Math.round(frac * 1_000_000_000);
    return negative
      ? { units: -units, nanos: -nanos }
      : { units, nanos };
  }
  const raw = String(value).trim();
  if (raw.length === 0) return { units: 0n, nanos: 0 };
  let negative = false;
  let idx = 0;
  if (raw[0] === "-") { negative = true; idx += 1; }
  else if (raw[0] === "+") { idx += 1; }
  const rest = raw.substring(idx);
  const dotIdx = rest.indexOf(".");
  let intPart: string;
  let fracPart: string;
  if (dotIdx === -1) {
    intPart = rest;
    fracPart = "";
  } else {
    intPart = rest.substring(0, dotIdx);
    fracPart = rest.substring(dotIdx + 1);
  }
  const cleanInt = intPart.replace(/[^0-9]/g, "");
  const cleanFrac = fracPart.replace(/[^0-9]/g, "");
  const units = cleanInt.length === 0 ? 0n : BigInt(cleanInt);
  const fracPadded = (cleanFrac + "000000000").substring(0, 9);
  const nanos = Number.parseInt(fracPadded, 10) || 0;
  return negative ? { units: -units, nanos: -nanos } : { units, nanos };
}

export function registerAmountText(): void {
  if (typeof customElements === "undefined") return;
  if (!customElements.get(CamountTextElement.tagName)) {
    customElements.define(CamountTextElement.tagName, CamountTextElement);
  }
}
```

- [ ] **Step 2: Create amountField.ts**

```ts
import { AmountHost, AmountHostOptions } from "../rendering/host";
import type { Money } from "../core/types";

export interface CamountFieldChangeEventDetail {
  amount: number;
  units: bigint;
  nanos: number;
  currencyCode: string;
}

export class CamountFieldElement extends HTMLElement {
  static readonly tagName = "camount-field";
  static get observedAttributes(): string[] {
    return ["value", "currency", "maximum-notation-digits", "alignment"];
  }

  private host: AmountHost | null = null;
  private _value: bigint = 0n;
  private _nanos = 0;

  connectedCallback(): void {
    if (this.host !== null) return;
    const shadow = this.attachShadow({ mode: "open" });
    this.host = new AmountHost(shadow, {
      ...this.readOptions(),
      onMoneyChange: (money) => this.onMoneyChanged(money),
    });
    this.applyValue();
  }

  attributeChangedCallback(name: string, _old: string | null, _next: string | null): void {
    if (!this.host) return;
    if (name === "value") {
      this.parseValueAttribute();
      this.applyValue();
      return;
    }
    this.host.configure(this.readOptions());
    this.applyValue();
  }

  disconnectedCallback(): void {
    this.host?.dispose();
    this.host = null;
  }

  get value(): number {
    const frac = this._nanos / 1_000_000_000;
    return Number(this._value) + (this._value < 0n ? -frac : frac);
  }

  set value(v: number | string | bigint) {
    const parsed = typeof v === "bigint" ? { units: v, nanos: 0 } : parseNumeric(v);
    this._value = parsed.units;
    this._nanos = parsed.nanos;
    this.applyValue();
  }

  private parseValueAttribute(): void {
    const raw = this.getAttribute("value") ?? "0";
    const parsed = parseNumeric(raw);
    this._value = parsed.units;
    this._nanos = parsed.nanos;
  }

  private applyValue(): void {
    if (!this.host) return;
    const currency = this.getAttribute("currency") ?? "USD";
    const money: Money = { units: this._value, nanos: this._nanos, currencyCode: currency };
    this.host.setAmount(money);
  }

  private onMoneyChanged(money: Money): void {
    if (money.units === this._value && money.nanos === this._nanos) return;
    this._value = money.units;
    this._nanos = money.nanos;
    const detail: CamountFieldChangeEventDetail = {
      amount: this.value,
      units: money.units,
      nanos: money.nanos,
      currencyCode: money.currencyCode,
    };
    this.dispatchEvent(new CustomEvent("camount-change", { detail, bubbles: true, composed: true }));
  }

  private readOptions(): AmountHostOptions {
    const alignment = this.getAttribute("alignment");
    return {
      currencyCode: this.getAttribute("currency") ?? "USD",
      maximumNotationDigits: readIntAttr(this, "maximum-notation-digits", 9),
      editable: true,
      alignment: alignment === "start" || alignment === "end" ? alignment : "center",
    };
  }
}

function readIntAttr(el: HTMLElement, name: string, fallback: number): number {
  const v = el.getAttribute(name);
  if (v === null) return fallback;
  const n = Number.parseInt(v, 10);
  return Number.isFinite(n) ? n : fallback;
}

function parseNumeric(value: string | number): { units: bigint; nanos: number } {
  if (typeof value === "number") {
    if (!Number.isFinite(value)) return { units: 0n, nanos: 0 };
    const negative = value < 0;
    const abs = Math.abs(value);
    const units = BigInt(Math.trunc(abs));
    const frac = abs - Math.trunc(abs);
    const nanos = Math.round(frac * 1_000_000_000);
    return negative ? { units: -units, nanos: -nanos } : { units, nanos };
  }
  const raw = String(value).trim();
  if (raw.length === 0) return { units: 0n, nanos: 0 };
  let negative = false;
  let idx = 0;
  if (raw[0] === "-") { negative = true; idx += 1; }
  else if (raw[0] === "+") { idx += 1; }
  const rest = raw.substring(idx);
  const dotIdx = rest.indexOf(".");
  const intPart = dotIdx === -1 ? rest : rest.substring(0, dotIdx);
  const fracPart = dotIdx === -1 ? "" : rest.substring(dotIdx + 1);
  const cleanInt = intPart.replace(/[^0-9]/g, "");
  const cleanFrac = fracPart.replace(/[^0-9]/g, "");
  const units = cleanInt.length === 0 ? 0n : BigInt(cleanInt);
  const fracPadded = (cleanFrac + "000000000").substring(0, 9);
  const nanos = Number.parseInt(fracPadded, 10) || 0;
  return negative ? { units: -units, nanos: -nanos } : { units, nanos };
}

export function registerAmountField(): void {
  if (typeof customElements === "undefined") return;
  if (!customElements.get(CamountFieldElement.tagName)) {
    customElements.define(CamountFieldElement.tagName, CamountFieldElement);
  }
}
```

- [ ] **Step 3: Create elements/index.ts (registers on import)**

```ts
import { registerAmountText, CamountTextElement } from "./amountText";
import { registerAmountField, CamountFieldElement } from "./amountField";

registerAmountText();
registerAmountField();

export { CamountTextElement, CamountFieldElement, registerAmountText, registerAmountField };
```

- [ ] **Step 4: Write tests**

Create `camount-js/tests/elements/amountText.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import "../../src/elements";

describe("<camount-text>", () => {
  it("registers the element", () => {
    expect(customElements.get("camount-text")).toBeDefined();
  });

  it("renders a currency amount", () => {
    const el = document.createElement("camount-text");
    el.setAttribute("amount", "42.50");
    el.setAttribute("currency", "USD");
    document.body.appendChild(el);
    const shadow = el.shadowRoot!;
    const text = Array.from(shadow.querySelectorAll(".camount-glyph[data-active='true']"))
      .map((e) => e.textContent)
      .join("");
    expect(text).toContain("42");
    expect(text).toContain("50");
    el.remove();
  });

  it("updates when amount attribute changes", () => {
    const el = document.createElement("camount-text");
    el.setAttribute("amount", "1");
    el.setAttribute("currency", "USD");
    document.body.appendChild(el);
    el.setAttribute("amount", "2");
    const shadow = el.shadowRoot!;
    const text = Array.from(shadow.querySelectorAll(".camount-glyph[data-active='true']"))
      .map((e) => e.textContent)
      .join("");
    expect(text).toContain("2");
    el.remove();
  });
});
```

Create `camount-js/tests/elements/amountField.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import "../../src/elements";
import type { CamountFieldElement, CamountFieldChangeEventDetail } from "../../src/elements/amountField";

describe("<camount-field>", () => {
  it("registers the element", () => {
    expect(customElements.get("camount-field")).toBeDefined();
  });

  it("fires camount-change on input", () => {
    const el = document.createElement("camount-field") as CamountFieldElement;
    el.setAttribute("currency", "USD");
    document.body.appendChild(el);
    const shadow = el.shadowRoot!;
    const hidden = shadow.querySelector<HTMLInputElement>("input.camount-hidden-input")!;
    let last: CamountFieldChangeEventDetail | null = null;
    el.addEventListener("camount-change", (e) => {
      last = (e as CustomEvent<CamountFieldChangeEventDetail>).detail;
    });
    hidden.value = "5";
    hidden.dispatchEvent(new Event("input"));
    expect(last).not.toBeNull();
    expect(last!.units).toBe(5n);
    expect(last!.currencyCode).toBe("USD");
    el.remove();
  });
});
```

- [ ] **Step 5: Run to verify pass**

Run: `cd camount-js && npm test -- elements`
Expected: PASS for both files.

---

## Task 13: React wrapper

**Files:**
- Create: `camount-js/src/react/jsx.d.ts`
- Create: `camount-js/src/react/AmountText.tsx`
- Create: `camount-js/src/react/AmountField.tsx`
- Create: `camount-js/src/react/index.ts`
- Create: `camount-js/tests/react/AmountText.test.tsx`

- [ ] **Step 1: Create jsx.d.ts**

```ts
import type React from "react";

declare global {
  namespace JSX {
    interface IntrinsicElements {
      "camount-text": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement> & {
          amount?: string;
          currency?: string;
          "maximum-notation-digits"?: string;
          "show-sign"?: "auto" | "always";
          "fraction-policy"?: "fixed" | "compact";
          alignment?: "start" | "center" | "end";
        },
        HTMLElement
      >;
      "camount-field": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement> & {
          value?: string;
          currency?: string;
          "maximum-notation-digits"?: string;
          alignment?: "start" | "center" | "end";
        },
        HTMLElement
      >;
    }
  }
}

export {};
```

- [ ] **Step 2: Create AmountText.tsx**

```tsx
import * as React from "react";
import "../elements";
import "./jsx";

export interface AmountTextProps {
  amount: number | string | bigint;
  currency: string;
  maximumNotationDigits?: number;
  showSign?: "auto" | "always";
  fractionPolicy?: "fixed" | "compact";
  alignment?: "start" | "center" | "end";
  className?: string;
  style?: React.CSSProperties;
}

export const AmountText = React.forwardRef<HTMLElement, AmountTextProps>(
  (props, ref) => {
    const {
      amount,
      currency,
      maximumNotationDigits,
      showSign,
      fractionPolicy,
      alignment,
      className,
      style,
    } = props;
    return (
      <camount-text
        ref={ref}
        amount={String(amount)}
        currency={currency}
        {...(maximumNotationDigits !== undefined
          ? { "maximum-notation-digits": String(maximumNotationDigits) }
          : null)}
        {...(showSign !== undefined ? { "show-sign": showSign } : null)}
        {...(fractionPolicy !== undefined ? { "fraction-policy": fractionPolicy } : null)}
        {...(alignment !== undefined ? { alignment } : null)}
        className={className}
        style={style}
      />
    );
  },
);
AmountText.displayName = "AmountText";
```

- [ ] **Step 3: Create AmountField.tsx**

```tsx
import * as React from "react";
import "../elements";
import "./jsx";
import type { CamountFieldChangeEventDetail, CamountFieldElement } from "../elements/amountField";

export interface AmountFieldProps {
  value: number | string | bigint;
  currency: string;
  onChange?: (detail: CamountFieldChangeEventDetail) => void;
  maximumNotationDigits?: number;
  alignment?: "start" | "center" | "end";
  className?: string;
  style?: React.CSSProperties;
}

export const AmountField = React.forwardRef<HTMLElement, AmountFieldProps>(
  (props, ref) => {
    const {
      value,
      currency,
      onChange,
      maximumNotationDigits,
      alignment,
      className,
      style,
    } = props;
    const innerRef = React.useRef<HTMLElement | null>(null);

    React.useEffect(() => {
      const el = innerRef.current as CamountFieldElement | null;
      if (!el || !onChange) return;
      const handler = (e: Event) =>
        onChange((e as CustomEvent<CamountFieldChangeEventDetail>).detail);
      el.addEventListener("camount-change", handler);
      return () => {
        el.removeEventListener("camount-change", handler);
      };
    }, [onChange]);

    const setRefs = React.useCallback(
      (el: HTMLElement | null) => {
        innerRef.current = el;
        if (typeof ref === "function") ref(el);
        else if (ref) (ref as React.MutableRefObject<HTMLElement | null>).current = el;
      },
      [ref],
    );

    return (
      <camount-field
        ref={setRefs}
        value={String(value)}
        currency={currency}
        {...(maximumNotationDigits !== undefined
          ? { "maximum-notation-digits": String(maximumNotationDigits) }
          : null)}
        {...(alignment !== undefined ? { alignment } : null)}
        className={className}
        style={style}
      />
    );
  },
);
AmountField.displayName = "AmountField";
```

- [ ] **Step 4: Create react/index.ts**

```ts
export { AmountText } from "./AmountText";
export type { AmountTextProps } from "./AmountText";
export { AmountField } from "./AmountField";
export type { AmountFieldProps } from "./AmountField";
export type { CamountFieldChangeEventDetail } from "../elements/amountField";
```

- [ ] **Step 5: Write the failing React test**

Create `camount-js/tests/react/AmountText.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import * as React from "react";
import { createRoot } from "react-dom/client";
import { act } from "react";
import { AmountText } from "../../src/react";

describe("<AmountText /> (React)", () => {
  it("renders the custom element and mirrors the amount attribute", async () => {
    const container = document.createElement("div");
    document.body.appendChild(container);
    const root = createRoot(container);
    await act(async () => {
      root.render(<AmountText amount="42.50" currency="USD" />);
    });
    const ct = container.querySelector("camount-text");
    expect(ct).not.toBeNull();
    expect(ct!.getAttribute("amount")).toBe("42.50");
    expect(ct!.getAttribute("currency")).toBe("USD");
    await act(async () => {
      root.unmount();
    });
    container.remove();
  });
});
```

- [ ] **Step 6: Install react-dom for tests**

Run: `cd camount-js && npm install --save-dev react-dom @types/react-dom`
Expected: `react-dom` added to devDependencies.

- [ ] **Step 7: Run to verify pass**

Run: `cd camount-js && npm test -- react`
Expected: PASS.

---

## Task 14: Top-level barrels

**Files:**
- Create: `camount-js/src/index.ts`
- Create: `camount-js/src/react.ts`

- [ ] **Step 1: Create src/index.ts**

```ts
// Public barrel — imports elements for their side effect (custom element registration)
// then re-exports Core + Rendering + Element types/classes.
import "./elements";

export * from "./core";
export * from "./rendering";
export { CamountTextElement, CamountFieldElement, registerAmountText, registerAmountField } from "./elements";
export type { CamountFieldChangeEventDetail } from "./elements/amountField";
```

- [ ] **Step 2: Create src/react.ts**

```ts
export * from "./react/index";
```

- [ ] **Step 3: Typecheck**

Run: `cd camount-js && npm run typecheck`
Expected: PASS.

- [ ] **Step 4: Build**

Run: `cd camount-js && npm run build`
Expected: `dist/` contains `index.{js,cjs,d.ts}` and `react.{js,cjs,d.ts}` (plus sourcemaps).

- [ ] **Step 5: Full test run**

Run: `cd camount-js && npm test`
Expected: all tests PASS.

---

## Task 15: CI — `.github/workflows/js.yml`

**Files:**
- Create: `.github/workflows/js.yml`

- [ ] **Step 1: Create the workflow**

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

- [ ] **Step 2: Validate YAML sanity**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/js.yml'))"`
Expected: no output, no error.

---

## Task 16: Publish workflow — `.github/workflows/publish-js.yml`

**Files:**
- Create: `.github/workflows/publish-js.yml`

- [ ] **Step 1: Create the workflow**

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
      id-token: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          registry-url: 'https://registry.npmjs.org'
          cache: 'npm'
          cache-dependency-path: camount-js/package-lock.json
      - run: npm ci
      - run: npm run typecheck
      - run: npm test
      - run: npm run build
      - run: npm publish --access public --provenance
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

- [ ] **Step 2: Validate YAML**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/publish-js.yml'))"`
Expected: no output, no error.

---

## Task 17: camount-js/README.md

**Files:**
- Create: `camount-js/README.md`

- [ ] **Step 1: Write README**

```markdown
# @yuridenison/camount

TypeScript port of [Camount](../README.md) — animated currency / amount widget
with per-character stack animations, field-aware styling, and a real input
pipeline. Ships as a framework-agnostic Web Component with an optional React
wrapper.

## Install

```bash
npm install @yuridenison/camount
```

React is an optional peer dependency — only install it if you use the React
wrapper:

```bash
npm install react
```

## Usage — Web Component

```html
<script type="module">
  import "@yuridenison/camount";
</script>

<camount-text amount="1234.56" currency="USD"></camount-text>
<camount-field value="0" currency="EUR"></camount-field>

<script>
  document.querySelector("camount-field").addEventListener("camount-change", (e) => {
    console.log(e.detail); // { amount, units, nanos, currencyCode }
  });
</script>
```

## Usage — React

```tsx
import { AmountText, AmountField } from "@yuridenison/camount/react";

export function Example() {
  const [value, setValue] = React.useState(0);
  return (
    <>
      <AmountText amount={1234.56} currency="USD" />
      <AmountField
        value={value}
        currency="USD"
        onChange={(detail) => setValue(detail.amount)}
      />
    </>
  );
}
```

## Attributes / Props

### `<camount-text>` / `<AmountText>`
- `amount` / `amount` — number or string, e.g. `"1234.56"`
- `currency` / `currency` — ISO 4217 code, e.g. `"USD"`
- `maximum-notation-digits` / `maximumNotationDigits` — default 9
- `show-sign` / `showSign` — `"auto"` (default) or `"always"`
- `fraction-policy` / `fractionPolicy` — `"fixed"` (default) or `"compact"`
- `alignment` / `alignment` — `"start"` / `"center"` (default) / `"end"`

### `<camount-field>` / `<AmountField>`
- `value` / `value` — current amount (number or string)
- `currency` / `currency` — ISO 4217 code
- `maximum-notation-digits` / `maximumNotationDigits`
- `alignment` / `alignment`
- `onChange` (React) or `"camount-change"` event (element):
  `detail` is `{ amount, units, nanos, currencyCode }`

## CSS Variables

Styles are shadow-DOM-scoped. Override these on the element:

```css
camount-text {
  --camount-duration: 200ms;
  --camount-easing: ease-out;
  --camount-zero-notation-color: #aaa;
  --camount-fixed-fraction-color: #aaa;
  --camount-cursor-color: dodgerblue;
  --camount-cursor-width: 2px;
}
```

## Development

```bash
cd camount-js
npm install
npm test
npm run build
```

## Related

- Kotlin / Compose Multiplatform: [`io.github.yuridenison:camount`](https://central.sonatype.com/artifact/io.github.yuridenison/camount)
- Swift Package Manager: `https://github.com/yuridenison/camount`

All three ports share the same version tag (`v0.9.1`).
```

---

## Task 18: Update root README.md

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add camount-js to the Repository Layout list**

Find the line under `## Repository Layout`:
```
- [`/samples/ios`](./samples/ios) — iOS sample app (shows both the Compose demo and the native SwiftUI demo side by side).
```

Insert before that line (so it sits after `camount-swift`):
```
- [`/camount-js`](./camount-js) — TypeScript / Web port (DOM+CSS renderer, `<camount-text>` / `<camount-field>` Web Components, React wrapper). Published to npm as `@yuridenison/camount`.
```

- [ ] **Step 2: Add a "Build and Run — Web library (camount-js)" section**

Insert after the existing `## Build and Run — Native iOS (camount-swift)` section and before the `## API parity` section:

```markdown
## Build and Run — Web library (camount-js)

`camount-js` is a standalone TypeScript package under [`/camount-js`](./camount-js). From that directory:

\`\`\`shell
cd camount-js
npm install
npm test
npm run build
\`\`\`

### npm

\`\`\`bash
npm install @yuridenison/camount
\`\`\`

Use as a Web Component:

\`\`\`html
<camount-text amount="1234.56" currency="USD"></camount-text>
\`\`\`

…or as a React component:

\`\`\`tsx
import { AmountText } from "@yuridenison/camount/react";
<AmountText amount={1234.56} currency="USD" />
\`\`\`

The npm package shares version `0.9.1` with the Kotlin and Swift releases — the same `v0.9.1` tag triggers Maven Central, SwiftPM, and npm.
```

(In the actual file, remove the backslashes that escape the triple backticks — they're present here only because this plan itself is fenced markdown.)

- [ ] **Step 3: Extend the Publishing section to mention npm**

Find the line in `## Publishing`:
```
CI publishes on tag push (`v*`) — see [`.github/workflows/publish.yml`](./.github/workflows/publish.yml).
```

Replace that single paragraph with:
```
CI publishes Kotlin artifacts on tag push (`v*`) — see [`.github/workflows/publish.yml`](./.github/workflows/publish.yml). Required repository secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` (Central Portal user token), `SIGNING_IN_MEMORY_KEY` (ASCII-armored GPG private key), `SIGNING_IN_MEMORY_KEY_PASSWORD`.

The same `v*` tag also triggers the npm publish — see [`.github/workflows/publish-js.yml`](./.github/workflows/publish-js.yml). Required secret: `NPM_TOKEN` (npm automation token with publish rights to `@yuridenison/camount`).
```

(Keep the "For a local publish" paragraph that follows the original line as-is.)

---

## Task 19: Final verification

- [ ] **Step 1: Full typecheck**

Run: `cd camount-js && npm run typecheck`
Expected: 0 errors.

- [ ] **Step 2: Full test run**

Run: `cd camount-js && npm test`
Expected: all tests PASS.

- [ ] **Step 3: Full build**

Run: `cd camount-js && npm run build`
Expected: `dist/` contains `index.{js,cjs,d.ts,js.map,cjs.map}` and `react.{js,cjs,d.ts,js.map,cjs.map}`.

- [ ] **Step 4: Dist inventory check**

Run: `ls camount-js/dist/`
Expected: the 10 files listed in Step 3.

- [ ] **Step 5: YAML sanity for both workflows**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/js.yml')); yaml.safe_load(open('.github/workflows/publish-js.yml'))"`
Expected: no output.

- [ ] **Step 6: Session rule — no commit**

Do NOT `git add` or `git commit` — the user's durable rule for this session is "do not commit any files." Summarize all changes at end of execution.

---

## Post-implementation manual steps (out of scope)

1. Create the `@yuridenison` scope on npm.
2. Generate an npm automation token; add it as the `NPM_TOKEN` repository secret.
3. Tag `v0.9.1` to trigger the three publications (Maven Central, SwiftPM, npm).
4. Smoke install: `npm install @yuridenison/camount` in a scratch project; verify the exports resolve.
5. Submit the package to [Swift Package Index](https://swiftpackageindex.com) and ensure the npm listing on npmjs.com renders correctly.
