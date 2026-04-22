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

    const signedUnits = negative && units !== 0n ? -units : units;
    const signedNanos = negative && nanos !== 0 ? -nanos : nanos;
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
