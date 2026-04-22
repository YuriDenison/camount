export interface GlyphMeasurement {
  readonly width: number;
  readonly height: number;
  readonly baseline: number;
}

export class GlyphMeasurer {
  private readonly probe: HTMLSpanElement;
  private readonly cache = new Map<string, GlyphMeasurement>();

  constructor(host: HTMLElement) {
    const doc = host.ownerDocument!;
    const probe = doc.createElement("span");
    probe.setAttribute("aria-hidden", "true");
    probe.style.position = "absolute";
    probe.style.visibility = "hidden";
    probe.style.top = "0";
    probe.style.left = "0";
    probe.style.pointerEvents = "none";
    probe.style.whiteSpace = "pre";
    probe.style.font = "inherit";
    probe.style.letterSpacing = "inherit";
    probe.style.lineHeight = "1";
    host.appendChild(probe);
    this.probe = probe;
  }

  clearCache(): void {
    this.cache.clear();
  }

  measure(char: string): GlyphMeasurement {
    const cached = this.cache.get(char);
    if (cached !== undefined) return cached;
    this.probe.textContent = char;
    const rect = this.probe.getBoundingClientRect();
    const height = rect.height;
    const measurement: GlyphMeasurement = {
      width: rect.width,
      height,
      baseline: height,
    };
    this.cache.set(char, measurement);
    return measurement;
  }

  lineHeight(): number {
    this.probe.textContent = "0";
    return this.probe.getBoundingClientRect().height;
  }

  dispose(): void {
    this.probe.remove();
  }
}
