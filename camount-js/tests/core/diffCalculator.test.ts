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
