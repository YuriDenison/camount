import { BoundsTween, Tween } from "./tween";
import type { AnimationScope } from "./tween";

const BLINK_INTERVAL_MS = 530;
const APPEAR_DURATION_MS = 500;

export class CursorCell {
  readonly element: HTMLElement;

  private readonly scope: AnimationScope;
  private readonly bounds: BoundsTween;
  private readonly alpha: Tween;
  private durationMs = 120;
  private visible = false;
  private blinkTimer: ReturnType<typeof setInterval> | null = null;
  private blinkOn = true;

  constructor(doc: Document, scope: AnimationScope) {
    this.scope = scope;
    this.element = doc.createElement("span");
    this.element.className = "camount-cursor";
    this.bounds = new BoundsTween(scope);
    this.bounds.onChange(() => this.applyBounds());
    this.alpha = new Tween(scope, 0);
    this.alpha.onChange((v) => {
      this.element.style.opacity = `${v}`;
    });
  }

  setDuration(ms: number): void {
    this.durationMs = ms;
  }

  setColor(color: string): void {
    this.element.style.backgroundColor = color;
  }

  setWidthPx(px: number): void {
    this.element.style.borderRadius = `${Math.max(0, px / 2)}px`;
  }

  setTargetBounds(left: number, top: number, width: number, height: number): void {
    this.bounds.setTarget(left, top, width, height, this.durationMs);
  }

  setVisible(visible: boolean): void {
    if (this.visible === visible) return;
    this.visible = visible;
    this.clearBlink();
    if (visible) {
      this.blinkOn = true;
      this.alpha.animateTo(1, { durationMs: APPEAR_DURATION_MS });
      this.blinkTimer = setInterval(() => {
        this.blinkOn = !this.blinkOn;
        this.alpha.animateTo(this.blinkOn ? 1 : 0, { durationMs: APPEAR_DURATION_MS });
      }, BLINK_INTERVAL_MS);
    } else {
      this.alpha.animateTo(0, { durationMs: APPEAR_DURATION_MS });
    }
  }

  dispose(): void {
    this.clearBlink();
    this.alpha.snapTo(0);
    this.element.remove();
  }

  private clearBlink(): void {
    if (this.blinkTimer !== null) {
      clearInterval(this.blinkTimer);
      this.blinkTimer = null;
    }
  }

  private applyBounds(): void {
    const left = this.bounds.left.value;
    const top = this.bounds.top.value;
    const w = this.bounds.width.value;
    const h = this.bounds.height.value;
    this.element.style.transform = `translate(${left}px, ${top}px)`;
    this.element.style.width = `${Math.max(0, w)}px`;
    this.element.style.height = `${Math.max(0, h)}px`;
  }
}
