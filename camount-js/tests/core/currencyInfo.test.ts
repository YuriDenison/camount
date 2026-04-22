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
