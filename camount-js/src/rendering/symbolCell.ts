import type { Field } from "../core/types";
import type { SymbolCellLike } from "../core/diffCalculator";
import { BoundsTween, Tween } from "./tween";
import type { AnimationScope } from "./tween";
import type { GlyphMeasurement, GlyphMeasurer } from "./measure";

export const DIFF_ANIMATION_DURATION_MS = 120;
const MAX_STACK_SYMBOLS = 3;
const ANIMATION_SCALE = 0.6;

export interface SymbolStyleHints {
  zeroNotationColor?: string;
  fixedFractionColor?: string;
}

interface GlyphLayer {
  readonly char: string;
  readonly field: Field | undefined;
  readonly element: HTMLElement;
  readonly measurement: GlyphMeasurement;
  readonly appearance: Tween;
  color: string;
}

export class SymbolCell implements SymbolCellLike {
  readonly element: HTMLElement;
  currentChar = "\0";
  isVisible = false;
  field: Field | undefined;

  private readonly scope: AnimationScope;
  private readonly measurer: GlyphMeasurer;
  private readonly bounds: BoundsTween;
  private readonly stack: GlyphLayer[] = [];
  private durationMs: number = DIFF_ANIMATION_DURATION_MS;
  private defaultColor = "inherit";
  private hints: SymbolStyleHints = {};
  private useGradient = false;

  constructor(doc: Document, scope: AnimationScope, measurer: GlyphMeasurer) {
    this.scope = scope;
    this.measurer = measurer;
    this.element = doc.createElement("span");
    this.element.className = "camount-cell";
    this.bounds = new BoundsTween(scope);
    this.bounds.onChange(() => this.applyBounds());
    this.applyBounds();
  }

  get isRunning(): boolean {
    if (this.bounds.isRunning) return true;
    for (const l of this.stack) if (l.appearance.isRunning) return true;
    return false;
  }

  get intrinsicWidth(): number {
    return this.stack.length === 0 ? 0 : this.stack[this.stack.length - 1]!.measurement.width;
  }

  get intrinsicHeight(): number {
    return this.stack.length === 0 ? 0 : this.stack[this.stack.length - 1]!.measurement.height;
  }

  setDuration(ms: number): void {
    this.durationMs = ms;
  }

  setStyleHints(hints: SymbolStyleHints): void {
    this.hints = { ...hints };
    this.rerenderColors();
  }

  setDefaultColor(color: string): void {
    this.defaultColor = color;
    this.rerenderColors();
  }

  setUseGradient(use: boolean): void {
    this.useGradient = use;
    this.rerenderColors();
  }

  setTargetBounds(left: number, top: number, width: number, height: number): void {
    this.bounds.setTarget(left, top, width, height, this.durationMs);
  }

  replace(char: string, field: Field | undefined): void {
    const last = this.stack[this.stack.length - 1];
    if (last !== undefined && last.char === char && last.field === field) {
      this.animateAppearance(last, 1);
      this.currentChar = char;
      this.isVisible = true;
      return;
    }
    this.field = field;
    const layer = this.createLayer(char, field);
    this.stack.push(layer);
    while (this.stack.length > MAX_STACK_SYMBOLS) {
      const evicted = this.stack.shift()!;
      evicted.element.remove();
    }
    for (let i = 0; i < this.stack.length - 1; i += 1) this.animateAppearance(this.stack[i]!, 0);
    this.animateAppearance(layer, 1);
    this.currentChar = char;
    this.isVisible = true;
  }

  delete(): void {
    const last = this.stack[this.stack.length - 1];
    if (last !== undefined) this.animateAppearance(last, 0);
    this.isVisible = false;
  }

  dispose(): void {
    for (const layer of this.stack) {
      layer.appearance.snapTo(0);
      layer.element.remove();
    }
    this.stack.length = 0;
    this.element.remove();
  }

  private createLayer(char: string, field: Field | undefined): GlyphLayer {
    const doc = this.element.ownerDocument!;
    const el = doc.createElement("span");
    el.className = "camount-glyph";
    el.textContent = char;
    const measurement = this.measurer.measure(char);
    const layer: GlyphLayer = {
      char,
      field,
      element: el,
      measurement,
      appearance: new Tween(this.scope, 0),
      color: "inherit",
    };
    layer.appearance.onChange((v) => this.renderLayer(layer, v));
    this.element.appendChild(el);
    this.applyLayerColor(layer);
    this.renderLayer(layer, 0);
    return layer;
  }

  private animateAppearance(layer: GlyphLayer, target: number): void {
    if (this.durationMs <= 0 || layer.appearance.value === target) {
      layer.appearance.snapTo(target);
      return;
    }
    layer.appearance.animateTo(target, { durationMs: this.durationMs });
  }

  private renderLayer(layer: GlyphLayer, v: number): void {
    if (v <= 0) {
      layer.element.style.opacity = "0";
      layer.element.style.transform = "scale(0)";
      return;
    }
    const selfScale = ANIMATION_SCALE + (1 - ANIMATION_SCALE) * v;
    layer.element.style.opacity = `${v}`;
    layer.element.style.transform = `scale(${selfScale})`;
  }

  private applyBounds(): void {
    const w = this.bounds.width.value;
    const h = this.bounds.height.value;
    const left = this.bounds.left.value;
    const top = this.bounds.top.value;
    if (w <= 0 || h <= 0) {
      this.element.style.transform = `translate(${left}px, ${top}px) scale(0, 0)`;
      return;
    }
    const last = this.stack[this.stack.length - 1];
    const iw = last?.measurement.width ?? 0;
    const ih = last?.measurement.height ?? 0;
    const sx = iw <= 0 ? 1 : w / iw;
    const sy = ih <= 0 ? 1 : h / ih;
    this.element.style.transform = `translate(${left}px, ${top}px) scale(${sx}, ${sy})`;
    this.element.style.width = `${iw}px`;
    this.element.style.height = `${ih}px`;
  }

  private rerenderColors(): void {
    for (const layer of this.stack) this.applyLayerColor(layer);
  }

  private applyLayerColor(layer: GlyphLayer): void {
    const color = this.colorFor(layer.field);
    layer.color = color;
    if (this.useGradient && layer.field === undefined && !isWhitespace(layer.char)) {
      layer.element.dataset.gradient = "true";
      layer.element.style.color = "";
    } else {
      delete layer.element.dataset.gradient;
      layer.element.style.color = color;
    }
  }

  private colorFor(field: Field | undefined): string {
    if (field === "zeroNotation" && this.hints.zeroNotationColor !== undefined) return this.hints.zeroNotationColor;
    if (field === "fixedFraction" && this.hints.fixedFractionColor !== undefined) return this.hints.fixedFractionColor;
    return this.defaultColor;
  }
}

function isWhitespace(c: string): boolean {
  return c === " " || c === " " || c === "\t" || c === "\n";
}
