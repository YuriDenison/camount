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
