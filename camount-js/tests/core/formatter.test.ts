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
