import { AmountHost, AmountHostOptions } from "../rendering/host";
import type { Money } from "../core/types";

export interface CamountFieldChangeEventDetail {
  amount: number;
  units: bigint;
  nanos: number;
  currencyCode: string;
}

export class CamountFieldElement extends HTMLElement {
  static readonly tagName = "camount-field";
  static get observedAttributes(): string[] {
    return ["value", "currency", "maximum-notation-digits", "alignment"];
  }

  private host: AmountHost | null = null;
  private _value: bigint = 0n;
  private _nanos = 0;

  connectedCallback(): void {
    if (this.host !== null) return;
    const shadow = this.attachShadow({ mode: "open" });
    this.host = new AmountHost(shadow, {
      ...this.readOptions(),
      onMoneyChange: (money) => this.onMoneyChanged(money),
    });
    this.parseValueAttribute();
    this.applyValue();
  }

  attributeChangedCallback(name: string, _old: string | null, _next: string | null): void {
    if (!this.host) return;
    if (name === "value") {
      this.parseValueAttribute();
      this.applyValue();
      return;
    }
    this.host.configure(this.readOptions());
    this.applyValue();
  }

  disconnectedCallback(): void {
    this.host?.dispose();
    this.host = null;
  }

  get value(): number {
    const frac = this._nanos / 1_000_000_000;
    return Number(this._value) + (this._value < 0n ? -frac : frac);
  }

  set value(v: number | string | bigint) {
    const parsed = typeof v === "bigint" ? { units: v, nanos: 0 } : parseNumeric(v);
    this._value = parsed.units;
    this._nanos = parsed.nanos;
    this.applyValue();
  }

  private parseValueAttribute(): void {
    const raw = this.getAttribute("value") ?? "0";
    const parsed = parseNumeric(raw);
    this._value = parsed.units;
    this._nanos = parsed.nanos;
  }

  private applyValue(): void {
    if (!this.host) return;
    const currency = this.getAttribute("currency") ?? "USD";
    const money: Money = { units: this._value, nanos: this._nanos, currencyCode: currency };
    this.host.setAmount(money);
  }

  private onMoneyChanged(money: Money): void {
    if (money.units === this._value && money.nanos === this._nanos) return;
    this._value = money.units;
    this._nanos = money.nanos;
    const detail: CamountFieldChangeEventDetail = {
      amount: this.value,
      units: money.units,
      nanos: money.nanos,
      currencyCode: money.currencyCode,
    };
    this.dispatchEvent(new CustomEvent("camount-change", { detail, bubbles: true, composed: true }));
  }

  private readOptions(): AmountHostOptions {
    const alignment = this.getAttribute("alignment");
    return {
      currencyCode: this.getAttribute("currency") ?? "USD",
      maximumNotationDigits: readIntAttr(this, "maximum-notation-digits", 9),
      editable: true,
      alignment: alignment === "start" || alignment === "end" ? alignment : "center",
    };
  }
}

function readIntAttr(el: HTMLElement, name: string, fallback: number): number {
  const v = el.getAttribute(name);
  if (v === null) return fallback;
  const n = Number.parseInt(v, 10);
  return Number.isFinite(n) ? n : fallback;
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
  const intPart = dotIdx === -1 ? rest : rest.substring(0, dotIdx);
  const fracPart = dotIdx === -1 ? "" : rest.substring(dotIdx + 1);
  const cleanInt = intPart.replace(/[^0-9]/g, "");
  const cleanFrac = fracPart.replace(/[^0-9]/g, "");
  const units = cleanInt.length === 0 ? 0n : BigInt(cleanInt);
  const fracPadded = (cleanFrac + "000000000").substring(0, 9);
  const nanos = Number.parseInt(fracPadded, 10) || 0;
  return negative
    ? { units: units === 0n ? 0n : -units, nanos: nanos === 0 ? 0 : -nanos }
    : { units, nanos };
}

export function registerAmountField(): void {
  if (typeof customElements === "undefined") return;
  if (!customElements.get(CamountFieldElement.tagName)) {
    customElements.define(CamountFieldElement.tagName, CamountFieldElement);
  }
}
