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

export interface AmountConfig {
  readonly maximumNotationDigits: number;
  readonly decimalSeparator: string;
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
