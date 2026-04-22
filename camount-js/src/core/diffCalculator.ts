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
