import type { AmountConfig, AmountFieldPositions } from "../core/types";
import { type DiffMode, makeDiffCalculator } from "../core/diffCalculator";
import { SymbolCell, DIFF_ANIMATION_DURATION_MS } from "./symbolCell";
import { CursorCell } from "./cursorCell";
import { AnimationScope } from "./tween";
import { GlyphMeasurer } from "./measure";

export type AmountAlignment = "start" | "center" | "end";

export interface CursorStyleOptions {
  color?: string;
  widthPx?: number;
  heightFraction?: number;
}

export interface AmountStyle {
  color?: string;
  zeroNotationColor?: string;
  fixedFractionColor?: string;
  gradient?: string;
  cursor?: CursorStyleOptions;
}

export interface AmountPainterOptions {
  config: AmountConfig;
  mode: DiffMode;
  style?: AmountStyle;
  alignment?: AmountAlignment;
}

const DEFAULT_CURSOR_WIDTH_PX = 2;
const DEFAULT_CURSOR_HEIGHT_FRACTION = 1;

export class AmountPainter {
  private readonly root: HTMLElement;
  private readonly rowEl: HTMLElement;
  private readonly sizerEl: HTMLElement;
  private readonly scope: AnimationScope;
  private readonly measurer: GlyphMeasurer;
  private cells: SymbolCell[] = [];
  private cursor: CursorCell | null = null;

  private config: AmountConfig;
  private mode: DiffMode;
  private style: AmountStyle;
  private alignment: AmountAlignment;

  private diff = this.rebuildDiff();
  private containerWidth = 0;
  private containerHeight = 0;
  private cursorPositionIndex = -1;

  private lastText: string | null = null;
  private lastPositions: AmountFieldPositions | null = null;

  intrinsicWidth = 0;
  intrinsicHeight = 0;

  constructor(root: HTMLElement, opts: AmountPainterOptions) {
    this.root = root;
    this.config = opts.config;
    this.mode = opts.mode;
    this.style = opts.style ?? {};
    this.alignment = opts.alignment ?? "center";
    const doc = root.ownerDocument!;
    const win = doc.defaultView ?? window;
    this.scope = new AnimationScope(win);
    this.measurer = new GlyphMeasurer(root);
    this.rowEl = doc.createElement("span");
    this.rowEl.className = "camount-root";
    this.sizerEl = doc.createElement("span");
    this.sizerEl.className = "camount-sizer";
    this.sizerEl.setAttribute("aria-hidden", "true");
    root.appendChild(this.sizerEl);
    root.appendChild(this.rowEl);
    this.ensureCursor();
    this.diff = this.rebuildDiff();
  }

  updateConfig(config: AmountConfig, mode: DiffMode): void {
    const configChanged = !this.configsEqual(config, this.config);
    const modeChanged = mode !== this.mode;
    this.config = config;
    this.mode = mode;
    if (configChanged || modeChanged) {
      this.diff = this.rebuildDiff();
      this.lastText = null;
    }
  }

  updateStyle(style: AmountStyle): void {
    this.style = style;
    const gradient = style.gradient;
    if (gradient !== undefined) {
      this.root.style.setProperty("--camount-gradient", gradient);
    } else {
      this.root.style.removeProperty("--camount-gradient");
    }
    this.ensureCursor();
    for (const cell of this.cells) {
      cell.setDefaultColor(style.color ?? "inherit");
      cell.setStyleHints({
        zeroNotationColor: style.zeroNotationColor,
        fixedFractionColor: style.fixedFractionColor,
      });
      cell.setUseGradient(gradient !== undefined);
    }
    if (this.cursor !== null && style.cursor !== undefined) {
      if (style.cursor.color !== undefined) this.cursor.setColor(style.cursor.color);
      if (style.cursor.widthPx !== undefined) this.cursor.setWidthPx(style.cursor.widthPx);
    }
    this.layout();
  }

  updateAlignment(alignment: AmountAlignment): void {
    if (this.alignment === alignment) return;
    this.alignment = alignment;
    this.layout();
  }

  setBounds(width: number, height: number): void {
    if (this.containerWidth === width && this.containerHeight === height) return;
    this.containerWidth = width;
    this.containerHeight = height;
    this.layout();
  }

  setText(text: string, positions: AmountFieldPositions): void {
    if (this.lastText === text && this.positionsEqual(this.lastPositions, positions)) return;
    this.lastText = text;
    this.lastPositions = positions;
    this.cells = this.diff.diff(this.cells, text, positions);
    this.syncDom();
    this.sizerEl.textContent = text;
    this.cursorPositionIndex = positions.cursorPosition;
    this.calculateIntrinsic();
    this.layout();
  }

  setCursorVisible(visible: boolean): void {
    this.cursor?.setVisible(visible);
  }

  dispose(): void {
    for (const cell of this.cells) cell.dispose();
    this.cells = [];
    this.cursor?.dispose();
    this.cursor = null;
    this.measurer.dispose();
    this.scope.dispose();
    this.rowEl.remove();
  }

  remeasure(): void {
    this.measurer.clearCache();
  }

  private createCell(): SymbolCell {
    const doc = this.rowEl.ownerDocument!;
    const cell = new SymbolCell(doc, this.scope, this.measurer);
    cell.setDuration(DIFF_ANIMATION_DURATION_MS);
    cell.setDefaultColor(this.style.color ?? "inherit");
    cell.setStyleHints({
      zeroNotationColor: this.style.zeroNotationColor,
      fixedFractionColor: this.style.fixedFractionColor,
    });
    cell.setUseGradient(this.style.gradient !== undefined);
    return cell;
  }

  private ensureCursor(): void {
    const cursorStyle = this.style.cursor;
    if (cursorStyle === undefined) {
      this.cursor?.dispose();
      this.cursor = null;
      return;
    }
    if (this.cursor === null) {
      const doc = this.rowEl.ownerDocument!;
      this.cursor = new CursorCell(doc, this.scope);
      this.cursor.setDuration(DIFF_ANIMATION_DURATION_MS);
      this.rowEl.appendChild(this.cursor.element);
    }
    if (cursorStyle.color !== undefined) this.cursor.setColor(cursorStyle.color);
    this.cursor.setWidthPx(cursorStyle.widthPx ?? DEFAULT_CURSOR_WIDTH_PX);
  }

  private rebuildDiff() {
    return makeDiffCalculator(this.mode, this.config, () => this.createCell());
  }

  private syncDom(): void {
    const kept = new Set<HTMLElement>();
    for (const cell of this.cells) kept.add(cell.element);
    const existing = Array.from(this.rowEl.children) as HTMLElement[];
    for (const child of existing) {
      if (child === this.cursor?.element) continue;
      if (!kept.has(child)) {
        if (child.classList.contains("camount-cell")) {
          child.remove();
        }
      }
    }
    for (const cell of this.cells) {
      if (cell.element.parentNode !== this.rowEl) this.rowEl.appendChild(cell.element);
    }
    if (this.cursor !== null && this.cursor.element.parentNode !== this.rowEl) {
      this.rowEl.appendChild(this.cursor.element);
    }
  }

  private calculateIntrinsic(): void {
    let w = 0;
    let h = 0;
    for (const cell of this.cells) {
      if (!cell.isVisible) continue;
      w += cell.intrinsicWidth;
      if (cell.intrinsicHeight > h) h = cell.intrinsicHeight;
    }
    this.intrinsicWidth = w;
    this.intrinsicHeight = h;
  }

  private layout(): void {
    if (this.containerWidth <= 0 || this.containerHeight <= 0) return;

    let visibleWidth = 0;
    let visibleHeight = 0;
    for (const cell of this.cells) {
      if (!cell.isVisible) continue;
      visibleWidth += cell.intrinsicWidth;
      if (cell.intrinsicHeight > visibleHeight) visibleHeight = cell.intrinsicHeight;
    }
    const cursorStyle = this.style.cursor;
    if (this.cursor !== null && cursorStyle !== undefined) {
      const cursorW = cursorStyle.widthPx ?? DEFAULT_CURSOR_WIDTH_PX;
      const cursorH = visibleHeight * (cursorStyle.heightFraction ?? DEFAULT_CURSOR_HEIGHT_FRACTION);
      visibleWidth += cursorW;
      if (cursorH > visibleHeight) visibleHeight = cursorH;
    }

    const scale = this.containerWidth < visibleWidth && visibleWidth > 0
      ? this.containerWidth / visibleWidth
      : 1;
    const scaledWidth = visibleWidth * scale;
    const scaledHeight = visibleHeight * scale;

    const top = (this.containerHeight - scaledHeight) / 2;
    let left = 0;
    if (this.alignment === "center") left = (this.containerWidth - scaledWidth) / 2;
    else if (this.alignment === "end") left = this.containerWidth - scaledWidth;

    const gradient = this.style.gradient !== undefined;
    const rowStart = left;
    let cursorLeft = left;
    let visibleIndex = 0;
    for (const cell of this.cells) {
      if (!cell.isVisible) continue;
      const w = cell.intrinsicWidth * scale;
      const h = cell.intrinsicHeight * scale;
      cell.setTargetBounds(left, top, w, h);
      if (gradient) {
        const offsetFromRow = left - rowStart;
        cell.element.style.setProperty("--camount-gradient-size", `${scaledWidth}px ${scaledHeight}px`);
        cell.element.style.setProperty("--camount-gradient-pos", `-${offsetFromRow}px 0`);
      } else {
        cell.element.style.removeProperty("--camount-gradient-size");
        cell.element.style.removeProperty("--camount-gradient-pos");
      }
      left += w;
      visibleIndex += 1;
      if (visibleIndex === this.cursorPositionIndex) cursorLeft = left;
    }

    if (this.cursor !== null && cursorStyle !== undefined) {
      const cursorW = (cursorStyle.widthPx ?? DEFAULT_CURSOR_WIDTH_PX) * scale;
      const cursorH = visibleHeight * (cursorStyle.heightFraction ?? DEFAULT_CURSOR_HEIGHT_FRACTION) * scale;
      const cursorTop = top + (scaledHeight - cursorH) / 2;
      this.cursor.setTargetBounds(cursorLeft, cursorTop, cursorW, cursorH);
    }
  }

  private configsEqual(a: AmountConfig, b: AmountConfig): boolean {
    return (
      a.maximumNotationDigits === b.maximumNotationDigits &&
      a.decimalSeparator === b.decimalSeparator &&
      a.groupingSeparator === b.groupingSeparator &&
      a.prefix === b.prefix &&
      a.suffix === b.suffix &&
      a.groupingSize === b.groupingSize &&
      a.maximumFractionDigits === b.maximumFractionDigits
    );
  }

  private positionsEqual(a: AmountFieldPositions | null, b: AmountFieldPositions): boolean {
    if (a === null) return false;
    return (
      a.cursorPosition === b.cursorPosition &&
      a.fixedFraction.beginIndex === b.fixedFraction.beginIndex &&
      a.fixedFraction.endIndex === b.fixedFraction.endIndex &&
      a.zeroNotation.beginIndex === b.zeroNotation.beginIndex &&
      a.zeroNotation.endIndex === b.zeroNotation.endIndex
    );
  }
}
