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
