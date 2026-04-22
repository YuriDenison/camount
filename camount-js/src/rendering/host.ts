import {
  AmountConfig,
  EMPTY_POSITIONS,
  Money,
  moneyEquals,
  moneyIsPositive,
  moneyIsZero,
} from "../core/types";
import { amountConfigForCurrency } from "../core/currencyInfo";
import { AmountFormatter } from "../core/formatter";
import { sanitizeInput } from "../core/sanitizeInput";
import { AmountPainter, AmountStyle, AmountAlignment } from "./painter";
import { camountStyleSheet } from "./styles";
import type { DiffMode } from "../core/diffCalculator";

export type SignMode = "auto" | "always";
export type FractionPolicy = "fixed" | "compact";
export type Alignment = AmountAlignment;

export interface AmountHostOptions {
  currencyCode: string;
  maximumNotationDigits?: number;
  editable?: boolean;
  mode?: DiffMode;
  style?: AmountStyle;
  showSign?: SignMode;
  fractionPolicy?: FractionPolicy;
  alignment?: Alignment;
  onMoneyChange?: (money: Money) => void;
}

export class AmountHost {
  private readonly shadow: ShadowRoot;
  private readonly root: HTMLElement;
  private readonly painter: AmountPainter;
  private readonly hiddenInput: HTMLInputElement | null;
  private readonly resizeObserver: ResizeObserver | null;

  private currencyCode: string;
  private config: AmountConfig;
  private maxNotation: number;
  private editable: boolean;
  private mode: DiffMode;
  private style: AmountStyle;
  private showSign: SignMode;
  private fractionPolicy: FractionPolicy;
  private alignment: Alignment;
  private onMoneyChange: ((m: Money) => void) | undefined;

  private displayFormatter: AmountFormatter;
  private inputFormatter: AmountFormatter;
  private lastParsed: Money | null = null;

  constructor(shadow: ShadowRoot, opts: AmountHostOptions) {
    this.shadow = shadow;
    shadow.adoptedStyleSheets = [...shadow.adoptedStyleSheets, camountStyleSheet()];
    const doc = shadow.ownerDocument!;
    this.root = doc.createElement("span");
    this.root.className = "camount-host";
    this.root.style.display = "block";
    this.root.style.position = "relative";
    shadow.appendChild(this.root);

    this.currencyCode = opts.currencyCode;
    this.maxNotation = opts.maximumNotationDigits ?? 9;
    this.editable = opts.editable ?? false;
    this.mode = opts.mode ?? (this.editable ? "edit" : "levenshtein");
    this.style = opts.style ?? {};
    this.showSign = opts.showSign ?? "auto";
    this.fractionPolicy = opts.fractionPolicy ?? "fixed";
    this.alignment = opts.alignment ?? "center";
    this.onMoneyChange = opts.onMoneyChange;

    this.config = amountConfigForCurrency(this.currencyCode, this.maxNotation);
    this.displayFormatter = this.makeDisplayFormatter();
    this.inputFormatter = this.makeInputFormatter();
    this.painter = new AmountPainter(this.root, {
      config: this.config,
      mode: this.mode,
      style: this.mergeStyleFromCSS(this.style),
      alignment: this.alignment,
    });

    if (this.editable) {
      const input = doc.createElement("input");
      input.type = "text";
      input.inputMode = "decimal";
      input.autocomplete = "off";
      input.className = "camount-hidden-input";
      shadow.appendChild(input);
      this.hiddenInput = input;
      input.addEventListener("input", () => this.onInputEvent());
      input.addEventListener("focus", () => this.painter.setCursorVisible(true));
      input.addEventListener("blur", () => this.painter.setCursorVisible(false));
      shadow.host.addEventListener("click", () => input.focus());
    } else {
      this.hiddenInput = null;
    }

    const win = doc.defaultView ?? window;
    const ResizeObs = (win as unknown as { ResizeObserver?: typeof ResizeObserver }).ResizeObserver;
    if (ResizeObs !== undefined) {
      this.resizeObserver = new ResizeObs((entries) => {
        for (const entry of entries) {
          const rect = entry.contentRect;
          this.painter.setBounds(rect.width, rect.height);
        }
      });
      this.resizeObserver.observe(shadow.host);
    } else {
      this.resizeObserver = null;
    }
    const hostRect = (shadow.host as HTMLElement).getBoundingClientRect();
    if (hostRect.width > 0 && hostRect.height > 0) {
      this.painter.setBounds(hostRect.width, hostRect.height);
    }
  }

  configure(changes: Partial<AmountHostOptions>): void {
    let recomputeConfig = false;
    if (changes.currencyCode !== undefined && changes.currencyCode !== this.currencyCode) {
      this.currencyCode = changes.currencyCode;
      recomputeConfig = true;
    }
    if (changes.maximumNotationDigits !== undefined && changes.maximumNotationDigits !== this.maxNotation) {
      this.maxNotation = changes.maximumNotationDigits;
      recomputeConfig = true;
    }
    if (changes.mode !== undefined) this.mode = changes.mode;
    if (changes.style !== undefined) {
      this.style = changes.style;
    }
    this.painter.updateStyle(this.mergeStyleFromCSS(this.style));
    if (changes.showSign !== undefined) this.showSign = changes.showSign;
    if (changes.fractionPolicy !== undefined) {
      this.fractionPolicy = changes.fractionPolicy;
      this.displayFormatter = this.makeDisplayFormatter();
    }
    if (changes.alignment !== undefined) {
      this.alignment = changes.alignment;
      this.painter.updateAlignment(this.alignment);
    }
    if (changes.onMoneyChange !== undefined) this.onMoneyChange = changes.onMoneyChange;
    if (recomputeConfig) {
      this.config = amountConfigForCurrency(this.currencyCode, this.maxNotation);
      this.displayFormatter = this.makeDisplayFormatter();
      this.inputFormatter = this.makeInputFormatter();
    }
    this.painter.updateConfig(this.config, this.mode);
  }

  setAmount(money: Money): void {
    if (this.lastParsed && moneyEquals(this.lastParsed, money)) return;
    this.lastParsed = money;
    if (this.editable && this.hiddenInput) {
      const inputText = this.inputFormatter.format(money);
      this.hiddenInput.value = inputText;
      const displayText = this.displayFormatter.formatInput({
        source: inputText,
        start: inputText.length,
        end: inputText.length,
        text: inputText,
        textStart: inputText.length,
        textEnd: inputText.length,
      });
      this.painter.setText(displayText, this.displayFormatter.fieldPositions());
    } else {
      const base = this.displayFormatter.format(money);
      const rendered = moneyIsZero(money)
        ? base
        : !moneyIsPositive(money)
          ? "-" + base
          : this.showSign === "always"
            ? "+" + base
            : base;
      this.painter.setText(rendered, EMPTY_POSITIONS);
    }
  }

  dispose(): void {
    this.resizeObserver?.disconnect();
    this.painter.dispose();
  }

  refreshStyle(): void {
    this.painter.updateStyle(this.mergeStyleFromCSS(this.style));
  }

  private mergeStyleFromCSS(base: AmountStyle): AmountStyle {
    const hostEl = this.shadow.host as HTMLElement;
    const win = this.shadow.ownerDocument?.defaultView ?? window;
    const computed = win.getComputedStyle(hostEl);
    const read = (name: string): string | undefined => {
      const v = computed.getPropertyValue(name).trim();
      return v.length === 0 ? undefined : v;
    };
    const color = base.color ?? read("--camount-color") ?? computed.color;
    const zero = base.zeroNotationColor ?? read("--camount-zero-notation-color");
    const fraction = base.fixedFractionColor ?? read("--camount-fixed-fraction-color");
    const gradient = base.gradient ?? read("--camount-gradient");
    const cursorColor = base.cursor?.color ?? read("--camount-cursor-color");
    const cursorWidthRaw = read("--camount-cursor-width");
    const cursorWidthPx = base.cursor?.widthPx ?? (cursorWidthRaw !== undefined ? parsePx(cursorWidthRaw) : undefined);
    const cursor = base.cursor ?? (cursorColor !== undefined ? { color: cursorColor, widthPx: cursorWidthPx } : undefined);
    return {
      ...base,
      color,
      zeroNotationColor: zero,
      fixedFractionColor: fraction,
      gradient,
      cursor,
    };
  }

  private onInputEvent(): void {
    if (!this.hiddenInput) return;
    const raw = this.hiddenInput.value;
    const cursor = this.hiddenInput.selectionStart ?? raw.length;
    const sanitized = sanitizeInput(raw, cursor, this.config);
    if (sanitized.text !== raw) {
      this.hiddenInput.value = sanitized.text;
      this.hiddenInput.setSelectionRange(sanitized.cursor, sanitized.cursor);
    }
    const displayText = this.displayFormatter.formatInput({
      source: sanitized.text,
      start: sanitized.text.length,
      end: sanitized.text.length,
      text: sanitized.text,
      textStart: sanitized.text.length,
      textEnd: sanitized.text.length,
    });
    const positions = this.displayFormatter.fieldPositions();
    this.painter.setText(displayText, positions);
    const parsed = this.displayFormatter.parse(sanitized.text, this.currencyCode);
    if (!this.lastParsed || !moneyEquals(this.lastParsed, parsed)) {
      this.lastParsed = parsed;
      this.onMoneyChange?.(parsed);
    }
  }

  private makeDisplayFormatter(): AmountFormatter {
    return new AmountFormatter({
      config: this.config,
      withCurrency: true,
      withGroupingSeparators: true,
      withFixedFractionLength: this.fractionPolicy === "fixed",
      withFixedZeroNotation: true,
    });
  }

  private makeInputFormatter(): AmountFormatter {
    return new AmountFormatter({
      config: this.config,
      withCurrency: false,
      withGroupingSeparators: false,
      withFixedFractionLength: false,
      withFixedZeroNotation: true,
    });
  }
}

function parsePx(v: string): number | undefined {
  const n = Number.parseFloat(v);
  return Number.isFinite(n) ? n : undefined;
}
