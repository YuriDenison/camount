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
