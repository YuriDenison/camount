export const FAST_OUT_SLOW_IN = [0.4, 0, 0.2, 1] as const;

const DEFAULT_EPSILON = 1e-5;

export function cubicBezier(t: number, p1x: number, p1y: number, p2x: number, p2y: number): number {
  if (t <= 0) return 0;
  if (t >= 1) return 1;
  let u = t;
  for (let i = 0; i < 8; i += 1) {
    const bx = bezier(u, p1x, p2x) - t;
    const dbx = bezierDerivative(u, p1x, p2x);
    if (Math.abs(dbx) < 1e-6) break;
    const delta = bx / dbx;
    u -= delta;
    if (Math.abs(delta) < DEFAULT_EPSILON) break;
  }
  if (u < 0) u = 0;
  else if (u > 1) u = 1;
  return bezier(u, p1y, p2y);
}

function bezier(t: number, p1: number, p2: number): number {
  const u = 1 - t;
  return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t;
}

function bezierDerivative(t: number, p1: number, p2: number): number {
  const u = 1 - t;
  return 3 * u * u * p1 + 6 * u * t * (p2 - p1) + 3 * t * t * (1 - p2);
}

export interface TweenOptions {
  readonly durationMs: number;
  readonly curve?: readonly [number, number, number, number];
}

export class AnimationScope {
  private readonly tweens = new Set<Tween>();
  private rafId: number | null = null;
  private readonly win: Window;

  constructor(win: Window = window) {
    this.win = win;
  }

  start(tween: Tween): void {
    this.tweens.add(tween);
    this.ensureFrame();
  }

  stop(tween: Tween): void {
    this.tweens.delete(tween);
  }

  dispose(): void {
    this.tweens.clear();
    if (this.rafId !== null) {
      this.win.cancelAnimationFrame(this.rafId);
      this.rafId = null;
    }
  }

  private ensureFrame(): void {
    if (this.rafId !== null) return;
    const tick = (now: number) => {
      this.rafId = null;
      const snapshot = Array.from(this.tweens);
      for (const tween of snapshot) {
        if (tween.tick(now)) this.tweens.delete(tween);
      }
      if (this.tweens.size > 0) {
        this.rafId = this.win.requestAnimationFrame(tick);
      }
    };
    this.rafId = this.win.requestAnimationFrame(tick);
  }
}

export class Tween {
  private scope: AnimationScope;
  private from = 0;
  private target = 0;
  private current = 0;
  private startMs = 0;
  private durationMs = 0;
  private curve: readonly [number, number, number, number] = FAST_OUT_SLOW_IN;
  private running = false;
  private listener: ((v: number) => void) | null = null;

  constructor(scope: AnimationScope, initialValue = 0) {
    this.scope = scope;
    this.current = initialValue;
    this.target = initialValue;
  }

  get value(): number {
    return this.current;
  }

  get isRunning(): boolean {
    return this.running;
  }

  onChange(listener: (v: number) => void): void {
    this.listener = listener;
  }

  snapTo(value: number): void {
    this.running = false;
    this.scope.stop(this);
    this.current = value;
    this.target = value;
    this.listener?.(value);
  }

  animateTo(target: number, options: TweenOptions): void {
    if (this.current === target) {
      this.target = target;
      this.running = false;
      this.scope.stop(this);
      return;
    }
    this.from = this.current;
    this.target = target;
    this.startMs = performance.now();
    this.durationMs = Math.max(1, options.durationMs);
    this.curve = options.curve ?? FAST_OUT_SLOW_IN;
    this.running = true;
    this.scope.start(this);
  }

  tick(now: number): boolean {
    if (!this.running) return true;
    const raw = (now - this.startMs) / this.durationMs;
    if (raw >= 1) {
      this.current = this.target;
      this.running = false;
      this.listener?.(this.current);
      return true;
    }
    const t = raw < 0 ? 0 : raw;
    const eased = cubicBezier(t, this.curve[0], this.curve[1], this.curve[2], this.curve[3]);
    this.current = this.from + (this.target - this.from) * eased;
    this.listener?.(this.current);
    return false;
  }
}

export class BoundsTween {
  readonly left: Tween;
  readonly top: Tween;
  readonly width: Tween;
  readonly height: Tween;

  constructor(scope: AnimationScope) {
    this.left = new Tween(scope);
    this.top = new Tween(scope);
    this.width = new Tween(scope);
    this.height = new Tween(scope);
  }

  get isRunning(): boolean {
    return this.left.isRunning || this.top.isRunning || this.width.isRunning || this.height.isRunning;
  }

  setTarget(left: number, top: number, width: number, height: number, durationMs: number): void {
    if (this.width.value === 0 && this.height.value === 0) {
      this.left.snapTo(left);
      this.top.snapTo(top);
      this.width.snapTo(width);
      this.height.snapTo(height);
      return;
    }
    const opts: TweenOptions = { durationMs };
    this.left.animateTo(left, opts);
    this.top.animateTo(top, opts);
    this.width.animateTo(width, opts);
    this.height.animateTo(height, opts);
  }

  onChange(listener: () => void): void {
    this.left.onChange(listener);
    this.top.onChange(listener);
    this.width.onChange(listener);
    this.height.onChange(listener);
  }
}
