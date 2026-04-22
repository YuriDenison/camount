import { AmountHost, AmountHostOptions } from "../rendering/host";
import type { Money } from "../core/types";

export class CamountTextElement extends HTMLElement {
  static readonly tagName = "camount-text";
  static get observedAttributes(): string[] {
    return ["amount", "currency", "maximum-notation-digits", "show-sign", "fraction-policy", "alignment"];
  }

  private host: AmountHost | null = null;
  private _amount: bigint = 0n;
  private _nanos = 0;

  connectedCallback(): void {
    if (this.host !== null) return;
    const shadow = this.attachShadow({ mode: "open" });
    this.host = new AmountHost(shadow, this.readOptions());
    this.parseAmountAttribute();
    this.applyAmount();
  }

  attributeChangedCallback(name: string, _old: string | null, _next: string | null): void {
    if (!this.host) return;
    if (name === "amount") {
      this.parseAmountAttribute();
      this.applyAmount();
      return;
    }
    this.host.configure(this.readOptions());
    this.applyAmount();
  }

  disconnectedCallback(): void {
    this.host?.dispose();
    this.host = null;
  }

  set amount(value: number | string | bigint) {
    if (typeof value === "bigint") {
      this._amount = value;
      this._nanos = 0;
    } else {
      const { units, nanos } = parseNumeric(value);
      this._amount = units;
      this._nanos = nanos;
    }
    this.applyAmount();
  }

  get amount(): number {
    const nanoFraction = this._nanos / 1_000_000_000;
    return Number(this._amount) + (this._amount < 0n ? -nanoFraction : nanoFraction);
  }

  private parseAmountAttribute(): void {
    const raw = this.getAttribute("amount") ?? "0";
    const { units, nanos } = parseNumeric(raw);
    this._amount = units;
    this._nanos = nanos;
  }

  private applyAmount(): void {
    if (!this.host) return;
    const currency = this.getAttribute("currency") ?? "USD";
    const money: Money = { units: this._amount, nanos: this._nanos, currencyCode: currency };
    this.host.setAmount(money);
  }

  private readOptions(): AmountHostOptions {
    return {
      currencyCode: this.getAttribute("currency") ?? "USD",
      maximumNotationDigits: this.readIntAttr("maximum-notation-digits", 9),
      editable: false,
      showSign: this.getAttribute("show-sign") === "always" ? "always" : "auto",
      fractionPolicy: this.getAttribute("fraction-policy") === "compact" ? "compact" : "fixed",
      alignment: this.readAlignmentAttr(),
    };
  }

  private readIntAttr(name: string, fallback: number): number {
    const v = this.getAttribute(name);
    if (v === null) return fallback;
    const n = Number.parseInt(v, 10);
    return Number.isFinite(n) ? n : fallback;
  }

  private readAlignmentAttr(): "start" | "center" | "end" {
    const v = this.getAttribute("alignment");
    if (v === "start" || v === "end") return v;
    return "center";
  }
}

function parseNumeric(value: string | number): { units: bigint; nanos: number } {
  if (typeof value === "number") {
    if (!Number.isFinite(value)) return { units: 0n, nanos: 0 };
    const negative = value < 0;
    const abs = Math.abs(value);
    const units = BigInt(Math.trunc(abs));
    const frac = abs - Math.trunc(abs);
    const nanos = Math.round(frac * 1_000_000_000);
    return negative
      ? { units: units === 0n ? 0n : -units, nanos: nanos === 0 ? 0 : -nanos }
      : { units, nanos };
  }
  const raw = String(value).trim();
  if (raw.length === 0) return { units: 0n, nanos: 0 };
  let negative = false;
  let idx = 0;
  if (raw[0] === "-") { negative = true; idx += 1; }
  else if (raw[0] === "+") { idx += 1; }
  const rest = raw.substring(idx);
  const dotIdx = rest.indexOf(".");
  let intPart: string;
  let fracPart: string;
  if (dotIdx === -1) {
    intPart = rest;
    fracPart = "";
  } else {
    intPart = rest.substring(0, dotIdx);
    fracPart = rest.substring(dotIdx + 1);
  }
  const cleanInt = intPart.replace(/[^0-9]/g, "");
  const cleanFrac = fracPart.replace(/[^0-9]/g, "");
  const units = cleanInt.length === 0 ? 0n : BigInt(cleanInt);
  const fracPadded = (cleanFrac + "000000000").substring(0, 9);
  const nanos = Number.parseInt(fracPadded, 10) || 0;
  return negative
    ? { units: units === 0n ? 0n : -units, nanos: nanos === 0 ? 0 : -nanos }
    : { units, nanos };
}

export function registerAmountText(): void {
  if (typeof customElements === "undefined") return;
  if (!customElements.get(CamountTextElement.tagName)) {
    customElements.define(CamountTextElement.tagName, CamountTextElement);
  }
}
