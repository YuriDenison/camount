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
